package br.com.bolsoclaro;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

/** Mantém perfis locais independentes sem mover os dados da instalação legada. */
public final class ProfileStore {
    private static final String DEFAULT_ID = "default";
    private final Path root;
    private final Path indexFile;
    private final List<Profile> profiles = new ArrayList<>();
    private String activeId;

    public ProfileStore(Path root) throws IOException {
        this.root = root;
        this.indexFile = root.resolve("profiles.properties");
        load();
    }

    public List<Profile> profiles() { return List.copyOf(profiles); }

    public Profile activeProfile() {
        return profiles.stream().filter(profile -> profile.id().equals(activeId)).findFirst()
                .orElseGet(() -> profiles.stream().filter(profile -> profile.id().equals(DEFAULT_ID)).findFirst().orElseThrow());
    }

    public Path directory(Profile profile) {
        if (DEFAULT_ID.equals(profile.id())) return root;
        if (!isUuid(profile.id())) throw new IllegalArgumentException("Identificador de perfil inválido.");
        return root.resolve("profiles").resolve(profile.id());
    }

    public Profile create(String requestedName) throws IOException {
        String name = requestedName == null ? "" : requestedName.strip();
        if (name.isEmpty()) throw new IllegalArgumentException("Informe um nome para o perfil.");
        if (name.length() > 40) throw new IllegalArgumentException("Use no máximo 40 caracteres.");
        if (name.codePoints().anyMatch(Character::isISOControl)) throw new IllegalArgumentException("O nome não pode conter quebras de linha ou caracteres de controle.");
        if (profiles.stream().anyMatch(profile -> profile.name().equalsIgnoreCase(name))) throw new IllegalArgumentException("Já existe um perfil com esse nome.");
        Profile profile = new Profile(UUID.randomUUID().toString(), name);
        Files.createDirectories(directory(profile));
        profiles.add(profile);
        save();
        return profile;
    }

    public void setActive(String id) throws IOException {
        if (profiles.stream().noneMatch(profile -> profile.id().equals(id))) throw new IllegalArgumentException("Esse perfil não existe.");
        activeId = id;
        save();
    }

    /** Deletes an inactive, non-legacy profile and returns false only if leftover files could not be cleaned up. */
    public boolean delete(Profile requested) throws IOException {
        if (requested == null) throw new IllegalArgumentException("Selecione um perfil.");
        int index = -1;
        for (int i = 0; i < profiles.size(); i++) if (profiles.get(i).id().equals(requested.id())) { index = i; break; }
        if (index < 0) throw new IllegalArgumentException("Esse perfil não existe mais.");
        Profile profile = profiles.get(index);
        if (DEFAULT_ID.equals(profile.id())) throw new IllegalArgumentException("O perfil Pessoal contém os dados legados e não pode ser excluído.");
        if (profile.id().equals(activeId)) throw new IllegalArgumentException("Troque para outro perfil antes de excluir este perfil.");

        Path profilesDirectory = root.resolve("profiles").toAbsolutePath().normalize();
        Path dataDirectory = directory(profile).toAbsolutePath().normalize();
        if (Files.isSymbolicLink(profilesDirectory)) throw new IOException("A pasta de perfis foi redirecionada; a exclusão foi cancelada por segurança.");
        if (!dataDirectory.startsWith(profilesDirectory) || dataDirectory.equals(profilesDirectory))
            throw new IOException("O caminho dos dados do perfil não é seguro para exclusão.");
        Path quarantine = profilesDirectory.resolve(".deleting-" + profile.id() + "-" + UUID.randomUUID()).normalize();
        boolean moved = false;
        if (Files.exists(dataDirectory, LinkOption.NOFOLLOW_LINKS)) {
            Files.createDirectories(profilesDirectory);
            move(dataDirectory, quarantine);
            moved = true;
        }

        profiles.remove(index);
        try {
            save();
        } catch (IOException ex) {
            profiles.add(index, profile);
            if (moved) {
                try { move(quarantine, dataDirectory); }
                catch (IOException rollback) { ex.addSuppressed(rollback); }
            }
            throw ex;
        }
        if (!moved) return true;
        try {
            deleteTree(quarantine);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static void move(Path source, Path target) throws IOException {
        try { Files.move(source, target, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException ex) { Files.move(source, target); }
    }

    private static void deleteTree(Path directory) throws IOException {
        if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) return;
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }

    private void load() throws IOException {
        Files.createDirectories(root);
        boolean changed = false;
        if (Files.exists(indexFile)) {
            Properties properties = new Properties();
            try (InputStream in = Files.newInputStream(indexFile)) { properties.load(in); }
            int count;
            try { count = Integer.parseInt(properties.getProperty("profile.count", "0")); }
            catch (NumberFormatException ex) { throw new IOException("O catálogo de perfis está inválido.", ex); }
            if (count < 0 || count > 1000) throw new IOException("Número de perfis inválido.");
            for (int i = 0; i < count; i++) {
                String id = properties.getProperty("profile." + i + ".id", "");
                String name = properties.getProperty("profile." + i + ".name", "").strip();
                if (name.isEmpty() || !(DEFAULT_ID.equals(id) || isUuid(id))) continue;
                if (profiles.stream().noneMatch(existing -> existing.id().equals(id))) profiles.add(new Profile(id, name));
            }
            activeId = properties.getProperty("active.id", DEFAULT_ID);
        }
        if (profiles.stream().noneMatch(profile -> profile.id().equals(DEFAULT_ID))) {
            profiles.add(0, new Profile(DEFAULT_ID, "Pessoal"));
            changed = true;
        }
        if (profiles.stream().noneMatch(profile -> profile.id().equals(activeId))) {
            activeId = DEFAULT_ID;
            changed = true;
        }
        if (changed || !Files.exists(indexFile)) save();
    }

    private void save() throws IOException {
        Properties properties = new Properties();
        properties.setProperty("profile.count", Integer.toString(profiles.size()));
        properties.setProperty("active.id", activeId == null ? DEFAULT_ID : activeId);
        for (int i = 0; i < profiles.size(); i++) {
            Profile profile = profiles.get(i);
            properties.setProperty("profile." + i + ".id", profile.id());
            properties.setProperty("profile." + i + ".name", profile.name());
        }
        Path temporary = Files.createTempFile(root, "profiles-", ".tmp");
        try {
            try (OutputStream out = Files.newOutputStream(temporary)) { properties.store(out, "Perfis locais do OrcaBem"); }
            try { Files.move(temporary, indexFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temporary, indexFile, StandardCopyOption.REPLACE_EXISTING); }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static boolean isUuid(String value) {
        if (value == null) return false;
        try { UUID.fromString(value); return true; }
        catch (IllegalArgumentException ex) { return false; }
    }

    public record Profile(String id, String name) {
        @Override public String toString() { return name; }
    }
}

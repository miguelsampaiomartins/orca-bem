package br.com.bolsoclaro;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
        try (OutputStream out = Files.newOutputStream(indexFile)) { properties.store(out, "Perfis locais do OrcaBem"); }
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

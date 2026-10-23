package br.com.bolsoclaro;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/** Catálogo pessoal de categorias, persistido no perfil local do usuário. */
public final class CategoryStore {
    private static final Map<Transaction.Type, List<String>> DEFAULTS = Map.of(
            Transaction.Type.EXPENSE, List.of("Moradia", "Alimentação", "Restaurantes", "Transporte", "Saúde", "Lazer", "Educação", "Contas", "Compras", "Outros"),
            Transaction.Type.INCOME, List.of("Salário", "Freelance", "Investimentos", "Vendas", "Outros")
    );

    private final Path file;
    private final Map<Transaction.Type, List<String>> categories = new EnumMap<>(Transaction.Type.class);

    public CategoryStore(Path file) throws IOException {
        this.file = file;
        for (Transaction.Type type : Transaction.Type.values()) categories.put(type, new ArrayList<>(DEFAULTS.get(type)));
        load();
    }

    public List<String> categories(Transaction.Type type) { return List.copyOf(categories.get(type)); }

    public boolean contains(Transaction.Type type, String name) {
        return categories.get(type).stream().anyMatch(existing -> existing.equalsIgnoreCase(name == null ? "" : name.trim()));
    }

    public boolean isDefault(Transaction.Type type, String name) {
        return DEFAULTS.get(type).stream().anyMatch(value -> value.equalsIgnoreCase(name));
    }

    public void add(Transaction.Type type, String name) throws IOException {
        String clean = name == null ? "" : name.strip();
        if (clean.isEmpty()) throw new IllegalArgumentException("Informe um nome para a categoria.");
        if (clean.length() > 40) throw new IllegalArgumentException("Use no máximo 40 caracteres.");
        if (clean.codePoints().anyMatch(Character::isISOControl)) throw new IllegalArgumentException("A categoria não pode conter quebras de linha ou caracteres de controle.");
        if (contains(type, clean)) throw new IllegalArgumentException("Essa categoria já existe.");
        categories.get(type).add(clean);
        save();
    }

    public void remove(Transaction.Type type, String name) throws IOException {
        List<String> list = categories.get(type);
        if (list.size() <= 1) throw new IllegalArgumentException("Mantenha pelo menos uma categoria disponível.");
        int index = -1;
        for (int i = 0; i < list.size(); i++) if (list.get(i).equalsIgnoreCase(name)) { index = i; break; }
        if (index < 0) throw new IllegalArgumentException("A categoria já não está na lista.");
        list.remove(index);
        save();
    }

    private void load() throws IOException {
        if (!Files.exists(file)) return;
        Properties props = new Properties();
        try (Reader reader = Files.newBufferedReader(file, java.nio.charset.StandardCharsets.UTF_8)) { props.load(reader); }
        for (Transaction.Type type : Transaction.Type.values()) {
            String prefix = prefix(type);
            String countText = props.getProperty(prefix + ".count");
            if (countText == null) continue;
            int count;
            try { count = Integer.parseInt(countText); }
            catch (NumberFormatException ex) { throw new IOException("Arquivo de categorias inválido.", ex); }
            if (count < 1 || count > 1000) throw new IOException("Número de categorias inválido no arquivo local.");
            List<String> loaded = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String value = props.getProperty(prefix + "." + i, "").strip();
                if (!value.isEmpty() && loaded.stream().noneMatch(existing -> existing.equalsIgnoreCase(value))) loaded.add(value);
            }
            if (!loaded.isEmpty()) categories.put(type, loaded);
        }
    }

    private void save() throws IOException {
        Properties props = new Properties();
        for (Transaction.Type type : Transaction.Type.values()) {
            String prefix = prefix(type);
            List<String> values = categories.get(type);
            props.setProperty(prefix + ".count", Integer.toString(values.size()));
            for (int i = 0; i < values.size(); i++) props.setProperty(prefix + "." + i, values.get(i));
        }
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        try (Writer writer = Files.newBufferedWriter(file, java.nio.charset.StandardCharsets.UTF_8)) { props.store(writer, "Categorias do OrçaBem"); }
    }

    private static String prefix(Transaction.Type type) { return type == Transaction.Type.EXPENSE ? "expense" : "income"; }
}

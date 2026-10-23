package br.com.bolsoclaro;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public final class BudgetStore {
    private final Path file;
    private final Properties values = new Properties();

    public BudgetStore(Path file) throws IOException {
        this.file = file;
        if (Files.exists(file)) try (InputStream in = Files.newInputStream(file)) { values.load(in); }
    }

    public Map<String, BigDecimal> forMonth(YearMonth month) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        String prefix = month + ".";
        for (String key : values.stringPropertyNames()) {
            if (key.startsWith(prefix)) {
                String category = new String(Base64.getUrlDecoder().decode(key.substring(prefix.length())), java.nio.charset.StandardCharsets.UTF_8);
                try { result.put(category, new BigDecimal(values.getProperty(key))); } catch (NumberFormatException ignored) { }
            }
        }
        return result;
    }

    public void set(YearMonth month, String category, BigDecimal amount) throws IOException {
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(category.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String key = month + "." + encoded;
        if (amount == null || amount.signum() <= 0) values.remove(key);
        else values.setProperty(key, amount.toPlainString());
        save();
    }

    public void setAll(YearMonth month, Map<String, BigDecimal> budgets) throws IOException {
        String prefix = month + ".";
        for (String key : new java.util.ArrayList<>(values.stringPropertyNames())) {
            if (key.startsWith(prefix)) values.remove(key);
        }
        for (Map.Entry<String, BigDecimal> entry : budgets.entrySet()) {
            if (entry.getValue() != null && entry.getValue().signum() > 0) {
                String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(entry.getKey().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                values.setProperty(prefix + encoded, entry.getValue().toPlainString());
            }
        }
        save();
    }

    private void save() throws IOException {
        Files.createDirectories(file.getParent());
        try (OutputStream out = Files.newOutputStream(file)) { values.store(out, "Orçamentos do Bolso Claro"); }
    }
}

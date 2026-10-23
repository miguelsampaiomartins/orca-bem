package br.com.bolsoclaro;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.Properties;

/** Metas de economia por mês, guardadas localmente. */
public final class SavingsGoalStore {
    private final Path file;
    private final Properties values = new Properties();

    public SavingsGoalStore(Path file) throws IOException {
        this.file = file;
        if (Files.exists(file)) try (InputStream in = Files.newInputStream(file)) { values.load(in); }
    }

    public BigDecimal get(YearMonth month) {
        try { return new BigDecimal(values.getProperty(month.toString(), "0")); }
        catch (NumberFormatException ignored) { return BigDecimal.ZERO; }
    }

    public void set(YearMonth month, BigDecimal amount) throws IOException {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount;
        if (value.signum() < 0) throw new IllegalArgumentException("A meta deve ser positiva ou zero.");
        if (value.signum() == 0) values.remove(month.toString());
        else values.setProperty(month.toString(), value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        try (OutputStream out = Files.newOutputStream(file)) { values.store(out, "Metas de economia do Bolso Claro"); }
    }
}

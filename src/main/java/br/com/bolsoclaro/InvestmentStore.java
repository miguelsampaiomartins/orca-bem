package br.com.bolsoclaro;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/** Registra aportes e resgates; a origem determina se um aporte reduz as receitas disponíveis do mês. */
public final class InvestmentStore {
    public enum Type { APORTE, RESGATE }
    public enum Source { RECEITA, EXTERNO }

    public record Movement(String id, LocalDate date, Type type, BigDecimal amount, String note, Source source) {
        public Movement {
            if (id == null || date == null || type == null || amount == null) throw new IllegalArgumentException("Movimento incompleto.");
            amount = amount.setScale(2, RoundingMode.HALF_UP);
            if (amount.signum() <= 0) throw new IllegalArgumentException("O valor precisa ser positivo.");
            note = note == null ? "" : note.strip();
            source = source == null ? Source.EXTERNO : source;
        }
        public Movement(String id, LocalDate date, Type type, BigDecimal amount, String note) {
            this(id, date, type, amount, note, Source.EXTERNO);
        }
        public String label() { return type == Type.APORTE ? "Aporte" : "Resgate"; }
        public String sourceLabel() {
            if (type != Type.APORTE) return "—";
            return source == Source.RECEITA ? "Receita registrada" : "Externo";
        }
    }

    private final Path file;
    private final List<Movement> movements = new ArrayList<>();

    public InvestmentStore(Path file) throws IOException {
        this.file = file;
        load();
    }

    public List<Movement> movements() {
        return movements.stream().sorted(Comparator.comparing(Movement::date).reversed()).toList();
    }

    public BigDecimal total(Type type) {
        return movements.stream().filter(movement -> movement.type() == type).map(Movement::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal incomeFundedContributions(YearMonth month) {
        return movements.stream()
                .filter(movement -> movement.type() == Type.APORTE && movement.source() == Source.RECEITA
                        && YearMonth.from(movement.date()).equals(month))
                .map(Movement::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal balance() { return total(Type.APORTE).subtract(total(Type.RESGATE)); }

    public BigDecimal balanceAt(LocalDate date) {
        BigDecimal balance = BigDecimal.ZERO;
        for (Movement movement : movements) {
            if (!movement.date().isAfter(date)) balance = apply(balance, movement);
        }
        return balance;
    }

    public BigDecimal balanceAfter(Movement target) {
        BigDecimal balance = BigDecimal.ZERO;
        for (Movement movement : chronological()) {
            balance = apply(balance, movement);
            if (movement.id().equals(target.id())) return balance;
        }
        return BigDecimal.ZERO;
    }

    public Map<String, BigDecimal> balancesAfterEach() {
        Map<String, BigDecimal> balances = new LinkedHashMap<>();
        BigDecimal balance = BigDecimal.ZERO;
        for (Movement movement : chronological()) {
            balance = apply(balance, movement);
            balances.put(movement.id(), balance);
        }
        return Map.copyOf(balances);
    }

    public void add(LocalDate date, Type type, BigDecimal amount, String note) throws IOException {
        add(date, type, amount, note, Source.EXTERNO);
    }

    public void add(LocalDate date, Type type, BigDecimal amount, String note, Source source) throws IOException {
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("Digite um valor maior que zero.");
        Movement movement = new Movement(UUID.randomUUID().toString(), date, type, amount, note, source);
        List<Movement> previous = new ArrayList<>(movements);
        movements.add(movement);
        BigDecimal running = BigDecimal.ZERO;
        for (Movement item : chronological()) {
            running = apply(running, item);
            if (running.signum() < 0) {
                movements.clear();
                movements.addAll(previous);
                throw new IllegalArgumentException("O resgate não pode ser maior que o saldo investido disponível nessa data.");
            }
        }
        try { save(); }
        catch (IOException ex) {
            movements.clear();
            movements.addAll(previous);
            throw ex;
        }
    }

    private List<Movement> chronological() {
        return movements.stream().sorted(Comparator.comparing(Movement::date)).toList();
    }

    private static BigDecimal apply(BigDecimal balance, Movement movement) {
        return movement.type() == Type.APORTE ? balance.add(movement.amount()) : balance.subtract(movement.amount());
    }

    private void load() throws IOException {
        if (!Files.exists(file)) return;
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(file)) { properties.load(in); }
        int count;
        try { count = Integer.parseInt(properties.getProperty("movement.count", "0")); }
        catch (NumberFormatException ex) { throw new IOException("O histórico de investimentos está inválido.", ex); }
        if (count < 0 || count > 100000) throw new IOException("Número de movimentos inválido no histórico de investimentos.");
        try {
            for (int i = 0; i < count; i++) {
                String prefix = "movement." + i + ".";
                String id = properties.getProperty(prefix + "id", UUID.randomUUID().toString());
                LocalDate date = LocalDate.parse(properties.getProperty(prefix + "date"));
                Type type = Type.valueOf(properties.getProperty(prefix + "type"));
                BigDecimal amount = new BigDecimal(properties.getProperty(prefix + "amount"));
                String note = properties.getProperty(prefix + "note", "");
                Source source = Source.valueOf(properties.getProperty(prefix + "source", Source.EXTERNO.name()));
                movements.add(new Movement(id, date, type, amount, note, source));
            }
        } catch (RuntimeException ex) {
            throw new IOException("Um movimento do histórico de investimentos está inválido.", ex);
        }
        BigDecimal running = BigDecimal.ZERO;
        for (Movement movement : chronological()) {
            running = apply(running, movement);
            if (running.signum() < 0) throw new IOException("O histórico de investimentos contém um resgate acima do saldo disponível.");
        }
    }

    private void save() throws IOException {
        Properties properties = new Properties();
        properties.setProperty("movement.count", Integer.toString(movements.size()));
        for (int i = 0; i < movements.size(); i++) {
            Movement movement = movements.get(i);
            String prefix = "movement." + i + ".";
            properties.setProperty(prefix + "id", movement.id());
            properties.setProperty(prefix + "date", movement.date().toString());
            properties.setProperty(prefix + "type", movement.type().name());
            properties.setProperty(prefix + "amount", movement.amount().toPlainString());
            properties.setProperty(prefix + "note", movement.note());
            properties.setProperty(prefix + "source", movement.source().name());
        }
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        try (OutputStream out = Files.newOutputStream(file)) { properties.store(out, "Aportes e resgates do OrçaBem"); }
    }
}

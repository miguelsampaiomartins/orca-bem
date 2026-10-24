package br.com.bolsoclaro;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/** Registra contas de investimento e seus movimentos, preservando a origem dos aportes. */
public final class InvestmentStore {
    public enum Type { APORTE, RESGATE }
    public enum Source { RECEITA, EXTERNO }

    public static final String LEGACY_INVESTMENT_ID = "legacy-investment";
    private static final String LEGACY_INVESTMENT_NAME = "Investimento legado";

    public record Investment(String id, String name, BigDecimal goal) {
        public Investment {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("Identificador de investimento inválido.");
            name = name == null ? "" : name.strip();
            if (name.isEmpty()) throw new IllegalArgumentException("Informe um nome para o investimento.");
            if (name.length() > 60) throw new IllegalArgumentException("Use no máximo 60 caracteres no nome do investimento.");
            if (name.codePoints().anyMatch(Character::isISOControl)) throw new IllegalArgumentException("O nome não pode conter caracteres de controle.");
            goal = goal == null ? BigDecimal.ZERO : goal.setScale(2, RoundingMode.HALF_UP);
            if (goal.signum() < 0) throw new IllegalArgumentException("A meta deve ser positiva ou zero.");
        }
        public boolean hasGoal() { return goal.signum() > 0; }
        @Override public String toString() { return name; }
    }

    public record Movement(String id, LocalDate date, Type type, BigDecimal amount, String note, Source source, String investmentId) {
        public Movement {
            if (id == null || date == null || type == null || amount == null) throw new IllegalArgumentException("Movimento incompleto.");
            amount = amount.setScale(2, RoundingMode.HALF_UP);
            if (amount.signum() <= 0) throw new IllegalArgumentException("O valor precisa ser positivo.");
            note = note == null ? "" : note.strip();
            source = source == null ? Source.EXTERNO : source;
            investmentId = investmentId == null || investmentId.isBlank() ? LEGACY_INVESTMENT_ID : investmentId;
        }
        public Movement(String id, LocalDate date, Type type, BigDecimal amount, String note, Source source) {
            this(id, date, type, amount, note, source, LEGACY_INVESTMENT_ID);
        }
        public Movement(String id, LocalDate date, Type type, BigDecimal amount, String note) {
            this(id, date, type, amount, note, Source.EXTERNO, LEGACY_INVESTMENT_ID);
        }
        public String label() { return type == Type.APORTE ? "Aporte" : "Resgate"; }
        public String sourceLabel() {
            if (type != Type.APORTE) return "—";
            return source == Source.RECEITA ? "Receita registrada" : "Externo";
        }
    }

    private final Path file;
    private final List<Investment> investments = new ArrayList<>();
    private final List<Movement> movements = new ArrayList<>();

    public InvestmentStore(Path file) throws IOException {
        this.file = file;
        load();
    }

    public List<Investment> investments() { return List.copyOf(investments); }

    public Investment investment(String id) {
        return investments.stream().filter(item -> item.id().equals(id)).findFirst().orElse(null);
    }

    public String investmentName(String id) {
        Investment investment = investment(id);
        return investment == null ? LEGACY_INVESTMENT_NAME : investment.name();
    }

    public boolean hasMovements(String investmentId) {
        return movements.stream().anyMatch(movement -> movement.investmentId().equals(investmentId));
    }

    public List<Movement> movements() {
        return movements.stream().sorted(Comparator.comparing(Movement::date).reversed()).toList();
    }

    public Movement movement(String id) {
        return movements.stream().filter(item -> item.id().equals(id)).findFirst().orElse(null);
    }

    public BigDecimal total(Type type) {
        return movements.stream().filter(movement -> movement.type() == type).map(Movement::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal total(Type type, String investmentId) {
        return movements.stream().filter(movement -> movement.type() == type && movement.investmentId().equals(investmentId))
                .map(Movement::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal incomeFundedContributions(YearMonth month) {
        return movements.stream()
                .filter(movement -> movement.type() == Type.APORTE && movement.source() == Source.RECEITA
                        && YearMonth.from(movement.date()).equals(month))
                .map(Movement::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal balance() { return total(Type.APORTE).subtract(total(Type.RESGATE)); }

    public BigDecimal balance(String investmentId) {
        return total(Type.APORTE, investmentId).subtract(total(Type.RESGATE, investmentId));
    }

    public BigDecimal balanceAt(LocalDate date) {
        BigDecimal balance = BigDecimal.ZERO;
        for (Movement movement : chronological()) if (!movement.date().isAfter(date)) balance = apply(balance, movement);
        return balance;
    }

    public BigDecimal balanceAt(String investmentId, LocalDate date) {
        BigDecimal balance = BigDecimal.ZERO;
        for (Movement movement : chronological()) {
            if (movement.investmentId().equals(investmentId) && !movement.date().isAfter(date)) balance = apply(balance, movement);
        }
        return balance;
    }

    public BigDecimal balanceAfter(Movement target) {
        return balancesAfterEach().getOrDefault(target.id(), BigDecimal.ZERO);
    }

    public Map<String, BigDecimal> balancesAfterEach() {
        Map<String, BigDecimal> balances = new HashMap<>();
        Map<String, BigDecimal> afterEach = new LinkedHashMap<>();
        for (Movement movement : chronological()) {
            BigDecimal balance = apply(balances.getOrDefault(movement.investmentId(), BigDecimal.ZERO), movement);
            balances.put(movement.investmentId(), balance);
            afterEach.put(movement.id(), balance);
        }
        return Map.copyOf(afterEach);
    }

    public Investment addInvestment(String name, BigDecimal goal) throws IOException {
        ensureUniqueName(name, null);
        Investment item = new Investment(UUID.randomUUID().toString(), name, goal);
        investments.add(item);
        try { save(); }
        catch (IOException ex) { investments.remove(item); throw ex; }
        return item;
    }

    public void updateInvestment(String id, String name, BigDecimal goal) throws IOException {
        int index = indexOfInvestment(id);
        Investment previous = investments.get(index);
        ensureUniqueName(name, id);
        investments.set(index, new Investment(id, name, goal));
        try { save(); }
        catch (IOException ex) { investments.set(index, previous); throw ex; }
    }

    public void removeInvestment(String id) throws IOException {
        int index = indexOfInvestment(id);
        if (hasMovements(id)) throw new IllegalArgumentException("Não é possível excluir um investimento que já tem movimentos registrados.");
        Investment removed = investments.remove(index);
        try { save(); }
        catch (IOException ex) { investments.add(index, removed); throw ex; }
    }

    public void add(LocalDate date, Type type, BigDecimal amount, String note) throws IOException {
        add(date, type, amount, note, Source.EXTERNO);
    }

    /** Compatibilidade para clientes antigos da classe; novos movimentos devem indicar o investimento. */
    public void add(LocalDate date, Type type, BigDecimal amount, String note, Source source) throws IOException {
        boolean createLegacy = investments.isEmpty();
        if (createLegacy) investments.add(new Investment(LEGACY_INVESTMENT_ID, LEGACY_INVESTMENT_NAME, BigDecimal.ZERO));
        String investmentId = investments.get(0).id();
        try { add(date, type, amount, note, source, investmentId); }
        catch (IOException | RuntimeException ex) {
            if (createLegacy) investments.removeIf(item -> item.id().equals(LEGACY_INVESTMENT_ID));
            throw ex;
        }
    }

    public void add(LocalDate date, Type type, BigDecimal amount, String note, Source source, String investmentId) throws IOException {
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("Digite um valor maior que zero.");
        requireInvestment(investmentId);
        Movement movement = new Movement(UUID.randomUUID().toString(), date, type, amount, note, source, investmentId);
        movements.add(movement);
        try { validateChronologicalBalances(); save(); }
        catch (IOException | RuntimeException ex) { movements.remove(movement); throw ex; }
    }

    public void updateMovement(String id, LocalDate date, Type type, BigDecimal amount, String note, Source source, String investmentId) throws IOException {
        int index = indexOfMovement(id);
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("Digite um valor maior que zero.");
        requireInvestment(investmentId);
        Movement previous = movements.get(index);
        movements.set(index, new Movement(id, date, type, amount, note, source, investmentId));
        try { validateChronologicalBalances(); save(); }
        catch (IOException | RuntimeException ex) { movements.set(index, previous); throw ex; }
    }

    public void removeMovement(String id) throws IOException {
        int index = indexOfMovement(id);
        Movement removed = movements.remove(index);
        try { validateChronologicalBalances(); save(); }
        catch (IOException | RuntimeException ex) { movements.add(index, removed); throw ex; }
    }

    private void ensureUniqueName(String name, String exceptId) {
        String clean = name == null ? "" : name.strip();
        if (clean.isEmpty()) throw new IllegalArgumentException("Informe um nome para o investimento.");
        if (clean.length() > 60) throw new IllegalArgumentException("Use no máximo 60 caracteres no nome do investimento.");
        if (clean.codePoints().anyMatch(Character::isISOControl)) throw new IllegalArgumentException("O nome não pode conter caracteres de controle.");
        if (investments.stream().anyMatch(item -> !item.id().equals(exceptId) && item.name().equalsIgnoreCase(clean)))
            throw new IllegalArgumentException("Já existe um investimento com esse nome.");
    }

    private int indexOfInvestment(String id) {
        for (int i = 0; i < investments.size(); i++) if (investments.get(i).id().equals(id)) return i;
        throw new IllegalArgumentException("O investimento selecionado não foi encontrado.");
    }

    private int indexOfMovement(String id) {
        for (int i = 0; i < movements.size(); i++) if (movements.get(i).id().equals(id)) return i;
        throw new IllegalArgumentException("O movimento selecionado não foi encontrado.");
    }

    private void requireInvestment(String id) {
        if (investment(id) == null) throw new IllegalArgumentException("Selecione um investimento válido.");
    }

    private List<Movement> chronological() {
        return movements.stream().sorted(Comparator.comparing(Movement::date)).toList();
    }

    private void validateChronologicalBalances() {
        Map<String, BigDecimal> balances = new HashMap<>();
        for (Movement movement : chronological()) {
            BigDecimal balance = apply(balances.getOrDefault(movement.investmentId(), BigDecimal.ZERO), movement);
            if (balance.signum() < 0) {
                throw new IllegalArgumentException("O resgate de '" + investmentName(movement.investmentId()) + "' não pode ser maior que o saldo disponível nessa data.");
            }
            balances.put(movement.investmentId(), balance);
        }
    }

    private static BigDecimal apply(BigDecimal balance, Movement movement) {
        return movement.type() == Type.APORTE ? balance.add(movement.amount()) : balance.subtract(movement.amount());
    }

    private void load() throws IOException {
        if (!Files.exists(file)) return;
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(file)) { properties.load(in); }
        boolean hasInvestmentData = properties.containsKey("investment.count");
        int investmentCount = parseCount(properties, "investment.count", 1000, "Número de investimentos inválido.");
        int movementCount = parseCount(properties, "movement.count", 100000, "Número de movimentos inválido no histórico de investimentos.");
        try {
            for (int i = 0; i < investmentCount; i++) {
                String prefix = "investment." + i + ".";
                investments.add(new Investment(properties.getProperty(prefix + "id"), properties.getProperty(prefix + "name"),
                        new BigDecimal(properties.getProperty(prefix + "goal", "0"))));
            }
            for (int i = 0; i < movementCount; i++) {
                String prefix = "movement." + i + ".";
                String id = properties.getProperty(prefix + "id", UUID.randomUUID().toString());
                LocalDate date = LocalDate.parse(properties.getProperty(prefix + "date"));
                Type type = Type.valueOf(properties.getProperty(prefix + "type"));
                BigDecimal amount = new BigDecimal(properties.getProperty(prefix + "amount"));
                String note = properties.getProperty(prefix + "note", "");
                Source source = Source.valueOf(properties.getProperty(prefix + "source", Source.EXTERNO.name()));
                String investmentId = properties.getProperty(prefix + "investmentId", LEGACY_INVESTMENT_ID);
                movements.add(new Movement(id, date, type, amount, note, source, investmentId));
            }
        } catch (RuntimeException ex) {
            throw new IOException("Um investimento ou movimento do histórico está inválido.", ex);
        }

        boolean migrated = !hasInvestmentData && !movements.isEmpty();
        if (migrated && investment(LEGACY_INVESTMENT_ID) == null)
            investments.add(new Investment(LEGACY_INVESTMENT_ID, LEGACY_INVESTMENT_NAME, BigDecimal.ZERO));
        for (int i = 0; i < movements.size(); i++) {
            Movement movement = movements.get(i);
            if (investment(movement.investmentId()) == null) {
                if (investment(LEGACY_INVESTMENT_ID) == null)
                    investments.add(new Investment(LEGACY_INVESTMENT_ID, LEGACY_INVESTMENT_NAME, BigDecimal.ZERO));
                movements.set(i, new Movement(movement.id(), movement.date(), movement.type(), movement.amount(), movement.note(), movement.source(), LEGACY_INVESTMENT_ID));
                migrated = true;
            }
        }
        try { validateChronologicalBalances(); }
        catch (IllegalArgumentException ex) { throw new IOException("O histórico de investimentos contém um resgate acima do saldo disponível.", ex); }
        if (migrated) save();
    }

    private static int parseCount(Properties properties, String key, int max, String message) throws IOException {
        try {
            int count = Integer.parseInt(properties.getProperty(key, "0"));
            if (count < 0 || count > max) throw new NumberFormatException();
            return count;
        } catch (NumberFormatException ex) { throw new IOException(message, ex); }
    }

    private void save() throws IOException {
        Properties properties = new Properties();
        properties.setProperty("investment.count", Integer.toString(investments.size()));
        for (int i = 0; i < investments.size(); i++) {
            Investment investment = investments.get(i);
            String prefix = "investment." + i + ".";
            properties.setProperty(prefix + "id", investment.id());
            properties.setProperty(prefix + "name", investment.name());
            properties.setProperty(prefix + "goal", investment.goal().toPlainString());
        }
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
            properties.setProperty(prefix + "investmentId", movement.investmentId());
        }
        Path destination = file.toAbsolutePath();
        Path parent = destination.getParent();
        if (parent != null) Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, "orcabem-investimentos-", ".tmp");
        try {
            try (OutputStream out = Files.newOutputStream(temporary)) { properties.store(out, "Investimentos e movimentos do OrçaBem"); }
            try { Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (java.nio.file.AtomicMoveNotSupportedException ex) { Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
}

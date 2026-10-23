package br.com.bolsoclaro;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CsvStore {
    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    private CsvStore() { }

    public static List<Transaction> load(Path file) throws IOException {
        List<Transaction> result = new ArrayList<>();
        if (!Files.exists(file)) return result;
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (lines.isEmpty()) return result;
        int start = looksLikeHeader(parseLine(lines.get(0))) ? 1 : 0;
        for (int i = start; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) continue;
            List<String> f = parseLine(line);
            if (f.size() < 5) throw new IOException("Linha " + (i + 1) + " inválida: esperava 5 colunas.");
            try {
                LocalDate date = parseDate(f.get(0));
                Transaction.Type type = parseType(f.get(1));
                result.add(new Transaction(date, type, f.get(2), f.get(3), parseAmount(f.get(4))));
            } catch (RuntimeException ex) {
                throw new IOException("Erro na linha " + (i + 1) + ": " + ex.getMessage(), ex);
            }
        }
        return result;
    }

    public static void save(Path file, List<Transaction> transactions) throws IOException {
        Files.createDirectories(file.getParent());
        List<String> lines = new ArrayList<>();
        lines.add("data,tipo,categoria,descricao,valor");
        for (Transaction t : transactions) {
            lines.add(csv(t.getDate().toString()) + "," + csv(t.getType() == Transaction.Type.INCOME ? "receita" : "despesa") + "," +
                    csv(t.getCategory()) + "," + csv(t.getDescription()) + "," + t.getAmount().toPlainString());
        }
        Files.write(file, lines, StandardCharsets.UTF_8);
    }

    public static String exportText(List<Transaction> transactions) {
        StringBuilder out = new StringBuilder("data,tipo,categoria,descricao,valor\r\n");
        for (Transaction t : transactions) {
            out.append(csv(t.getDate().toString())).append(',')
                    .append(csv(t.getType() == Transaction.Type.INCOME ? "receita" : "despesa")).append(',')
                    .append(csv(t.getCategory())).append(',').append(csv(t.getDescription())).append(',')
                    .append(t.getAmount().toPlainString()).append("\r\n");
        }
        return out.toString();
    }

    static List<String> parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        char delimiter = hasUnquotedSemicolon(line) ? ';' : ',';
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') { field.append('"'); i++; }
                else quoted = !quoted;
            } else if (c == delimiter && !quoted) {
                fields.add(field.toString().trim()); field.setLength(0);
            } else field.append(c);
        }
        fields.add(field.toString().trim());
        return fields;
    }

    private static boolean hasUnquotedSemicolon(String line) {
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') i++;
                else quoted = !quoted;
            } else if (c == ';' && !quoted) return true;
        }
        return false;
    }

    private static boolean looksLikeHeader(List<String> fields) {
        if (fields.isEmpty()) return false;
        String first = normalize(fields.get(0));
        return first.equals("data") || first.equals("date");
    }

    private static LocalDate parseDate(String value) {
        String v = value.trim();
        try { return LocalDate.parse(v); }
        catch (DateTimeParseException ignored) { return LocalDate.parse(v, BR_DATE); }
    }

    private static Transaction.Type parseType(String value) {
        String v = normalize(value);
        if (v.startsWith("rec") || v.equals("income") || v.equals("entrada")) return Transaction.Type.INCOME;
        if (v.startsWith("des") || v.equals("expense") || v.equals("saida")) return Transaction.Type.EXPENSE;
        throw new IllegalArgumentException("tipo deve ser receita ou despesa");
    }

    private static BigDecimal parseAmount(String value) {
        String v = value.trim().replace("R$", "").replace(" ", "");
        if (v.contains(",")) v = v.replace(".", "").replace(',', '.');
        BigDecimal amount = new BigDecimal(v);
        if (amount.signum() <= 0) throw new IllegalArgumentException("valor precisa ser maior que zero");
        return amount;
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).trim().replace("á", "a").replace("ã", "a").replace("é", "e")
                .replace("ê", "e").replace("í", "i").replace("ó", "o").replace("ô", "o")
                .replace("õ", "o").replace("ú", "u").replace("ç", "c");
    }

    private static String csv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}

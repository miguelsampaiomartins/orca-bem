package br.com.bolsoclaro;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class Transaction {
    public enum Type { INCOME, EXPENSE }

    private final String id;
    private final LocalDate date;
    private final Type type;
    private final String category;
    private final String description;
    private final BigDecimal amount;

    public Transaction(LocalDate date, Type type, String category, String description, BigDecimal amount) {
        this(UUID.randomUUID().toString(), date, type, category, description, amount);
    }

    public Transaction(String id, LocalDate date, Type type, String category, String description, BigDecimal amount) {
        this.id = Objects.requireNonNull(id);
        this.date = Objects.requireNonNull(date);
        this.type = Objects.requireNonNull(type);
        this.category = clean(category, "Outros");
        this.description = clean(description, "Sem descrição");
        this.amount = Objects.requireNonNull(amount).abs();
        if (this.amount.signum() <= 0) throw new IllegalArgumentException("O valor deve ser maior que zero.");
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) return fallback;
        return value.trim();
    }

    public String getId() { return id; }
    public LocalDate getDate() { return date; }
    public Type getType() { return type; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
}

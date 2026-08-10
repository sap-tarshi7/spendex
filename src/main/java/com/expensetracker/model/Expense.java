package com.expensetracker.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Expense — core domain model.
 * Demonstrates OOP: encapsulation with private fields + getters/setters.
 * Uses LocalDate for type-safe date storage.
 */
public class Expense {

    private String id;
    private double amount;
    private String category;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate date;

    private String description;

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter CSV_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    /** Default constructor — required for Jackson deserialization */
    public Expense() {
        this.id = UUID.randomUUID().toString();
    }

    /** Full constructor used when creating a new expense */
    public Expense(double amount, String category, LocalDate date, String description) {
        this.id = UUID.randomUUID().toString();
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.description = (description != null) ? description.trim() : "";
    }

    // ─── Getters ───────────────────────────────────────────────────────────────

    public String getId()          { return id; }
    public double getAmount()      { return amount; }
    public String getCategory()    { return category; }
    public LocalDate getDate()     { return date; }
    public String getDescription() { return description; }

    /** Display-formatted date: "09 Aug 2026" */
    public String getFormattedDate() {
        return date != null ? date.format(DISPLAY_FORMAT) : "";
    }

    /** Short formatted date for list view: "09 AUG" */
    public String getShortDate() {
        return date != null ? date.format(DateTimeFormatter.ofPattern("dd MMM")).toUpperCase() : "";
    }

    // ─── Setters ───────────────────────────────────────────────────────────────

    public void setId(String id)               { this.id = id; }
    public void setAmount(double amount)       { this.amount = amount; }
    public void setCategory(String category)   { this.category = category; }
    public void setDate(LocalDate date)        { this.date = date; }
    public void setDescription(String desc)    { this.description = desc != null ? desc.trim() : ""; }

    /**
     * Converts this expense to a CSV row.
     * Format: date,category,description,amount
     */
    public String toCsvLine() {
        String safeDesc = description.replace(",", ";");
        return String.format("%s,%s,%s,%.2f",
                date.format(CSV_FORMAT), category, safeDesc, amount);
    }

    @Override
    public String toString() {
        return String.format("Expense{id=%s, date=%s, category=%s, amount=%.2f, desc=%s}",
                id, date, category, amount, description);
    }
}

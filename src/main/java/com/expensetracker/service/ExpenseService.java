package com.expensetracker.service;

import com.expensetracker.model.Expense;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Collectors;

/**
 * ExpenseService — core business logic layer.
 * Manages an ArrayList<Expense> and provides:
 *   - Add / retrieve / filter expenses
 *   - Monthly totals using YearMonth
 *   - Category aggregation using Collections
 *   - Sorting using Comparator
 *   - Statistical calculations
 */
@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    public ExpenseService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    // ─── Core Operations ───────────────────────────────────────────────────────

    /**
     * Validates and adds a new expense.
     * Throws IllegalArgumentException with human-friendly messages on invalid input.
     */
    public Expense addExpense(double amount, String category, LocalDate date, String description) {
        // Validation
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than ₹0.");
        }
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Please select a category.");
        }
        if (!VALID_CATEGORIES.contains(category)) {
            throw new IllegalArgumentException("Invalid category: " + category);
        }
        if (date == null) {
            throw new IllegalArgumentException("Please enter a valid date.");
        }

        Expense expense = new Expense(amount, category.trim(), date,
                description != null ? description.trim() : "");
        
        return expenseRepository.save(expense);
    }

    /** Returns all expenses sorted newest first */
    public List<Expense> getAllExpenses() {
        return expenseRepository.findAll().stream()
                .sorted(Comparator.comparing(Expense::getDate).reversed())
                .collect(Collectors.toList());
    }

    /** Returns expenses filtered by optional category and/or month */
    public List<Expense> getFilteredExpenses(String category, YearMonth yearMonth) {
        return expenseRepository.findAll().stream()
                .filter(e -> {
                    boolean catOk = (category == null || category.isEmpty()
                            || category.equalsIgnoreCase("All")
                            || e.getCategory().equalsIgnoreCase(category));
                    boolean monthOk = (yearMonth == null
                            || YearMonth.from(e.getDate()).equals(yearMonth));
                    return catOk && monthOk;
                })
                .sorted(Comparator.comparing(Expense::getDate).reversed())
                .collect(Collectors.toList());
    }

    /** Returns the most recent N expenses */
    public List<Expense> getRecentExpenses(int count) {
        return getAllExpenses().stream().limit(count).collect(Collectors.toList());
    }

    // ─── Summary Statistics ────────────────────────────────────────────────────

    public double getTotalSpending() {
        return expenseRepository.findAll().stream().mapToDouble(Expense::getAmount).sum();
    }

    public double getTotalSpendingForMonth(YearMonth ym) {
        return getExpensesForMonth(ym).stream().mapToDouble(Expense::getAmount).sum();
    }

    public double getAverageExpense() {
        List<Expense> all = expenseRepository.findAll();
        return all.isEmpty() ? 0.0 : getTotalSpending() / all.size();
    }

    public double getAverageExpenseForMonth(YearMonth ym) {
        List<Expense> monthly = getExpensesForMonth(ym);
        return monthly.isEmpty() ? 0.0
                : monthly.stream().mapToDouble(Expense::getAmount).sum() / monthly.size();
    }

    public double getHighestSingleExpenseForMonth(YearMonth ym) {
        return getExpensesForMonth(ym).stream()
                .mapToDouble(Expense::getAmount).max().orElse(0.0);
    }

    public int getTransactionCount() { return (int) expenseRepository.count(); }

    public int getTransactionCountForMonth(YearMonth ym) {
        return (int) expenseRepository.findAll().stream()
                .filter(e -> YearMonth.from(e.getDate()).equals(ym)).count();
    }

    /**
     * Returns category → total spending, sorted descending.
     * Demonstrates HashMap + LinkedHashMap for ordered results.
     */
    public Map<String, Double> getCategoryTotals(List<Expense> source) {
        Map<String, Double> raw = new HashMap<>();
        for (Expense e : source) {
            raw.merge(e.getCategory(), e.getAmount(), Double::sum);
        }
        // Sort by value descending, preserve order in LinkedHashMap
        return raw.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    public Map<String, Double> getAllCategoryTotals() {
        return getCategoryTotals(expenseRepository.findAll());
    }

    public String getHighestCategory() {
        Map<String, Double> totals = getAllCategoryTotals();
        return totals.isEmpty() ? "—" : totals.entrySet().iterator().next().getKey();
    }

    public double getHighestCategoryAmount() {
        Map<String, Double> totals = getAllCategoryTotals();
        return totals.isEmpty() ? 0.0 : totals.entrySet().iterator().next().getValue();
    }

    public String getHighestCategoryForMonth(YearMonth ym) {
        List<Expense> monthly = getExpensesForMonth(ym);
        Map<String, Double> totals = getCategoryTotals(monthly);
        return totals.isEmpty() ? "—" : totals.entrySet().iterator().next().getKey();
    }

    public double getHighestCategoryAmountForMonth(YearMonth ym) {
        List<Expense> monthly = getExpensesForMonth(ym);
        Map<String, Double> totals = getCategoryTotals(monthly);
        return totals.isEmpty() ? 0.0 : totals.entrySet().iterator().next().getValue();
    }

    public List<Expense> getExpensesForMonth(YearMonth ym) {
        return expenseRepository.findAll().stream()
                .filter(e -> YearMonth.from(e.getDate()).equals(ym))
                .sorted(Comparator.comparing(Expense::getDate).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Deletes an expense by its ID.
     * Returns true if found and removed, false if not found.
     */
    public boolean deleteExpense(Long id) {
        if (expenseRepository.existsById(id)) {
            expenseRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public boolean isEmpty() { return expenseRepository.count() == 0; }

    public YearMonth getCurrentMonth() { return YearMonth.now(); }

    // ─── Constants ─────────────────────────────────────────────────────────────

    public static final List<String> VALID_CATEGORIES = List.of(
            "Food", "Transport", "Shopping", "Bills",
            "Education", "Entertainment", "Travel", "Healthcare", "Other"
    );
}

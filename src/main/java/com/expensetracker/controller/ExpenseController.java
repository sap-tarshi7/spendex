package com.expensetracker.controller;

import com.expensetracker.model.Expense;
import com.expensetracker.service.ExpenseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * ExpenseController — REST API endpoints for the Spendex web application.
 * All responses are JSON consumed by the vanilla JS frontend.
 *
 * Endpoints:
 *   GET  /api/summary              — overall dashboard stats
 *   GET  /api/expenses             — all expenses (with optional filters)
 *   POST /api/expenses             — add a new expense
 *   GET  /api/report               — monthly report (?month=8&year=2026)
 *   GET  /api/categories           — list of valid categories
 */
@RestController
@RequestMapping("/api")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    // ─── GET /api/summary ─────────────────────────────────────────────────────

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        YearMonth currentMonth = YearMonth.now();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalSpending",          expenseService.getTotalSpending());
        summary.put("transactionCount",        expenseService.getTransactionCount());
        summary.put("currentMonthSpending",    expenseService.getTotalSpendingForMonth(currentMonth));
        summary.put("currentMonthTxCount",     expenseService.getTransactionCountForMonth(currentMonth));
        summary.put("highestCategory",         expenseService.getHighestCategory());
        summary.put("highestCategoryAmount",   expenseService.getHighestCategoryAmount());
        summary.put("averageExpense",          expenseService.getAverageExpense());
        summary.put("currentMonthLabel",       currentMonth.getMonth().getDisplayName(
                java.time.format.TextStyle.FULL, Locale.ENGLISH) + " " + currentMonth.getYear());
        summary.put("isEmpty",                 expenseService.isEmpty());
        summary.put("recentExpenses",          expenseService.getRecentExpenses(8));
        summary.put("categoryTotals",          expenseService.getAllCategoryTotals());

        return ResponseEntity.ok(summary);
    }

    // ─── GET /api/expenses ────────────────────────────────────────────────────

    @GetMapping("/expenses")
    public ResponseEntity<Map<String, Object>> getExpenses(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {

        YearMonth ym = null;
        if (month != null && year != null) {
            try {
                ym = YearMonth.of(year, month);
            } catch (Exception e) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid month/year combination."));
            }
        }

        List<Expense> filtered = expenseService.getFilteredExpenses(category, ym);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("expenses", filtered);
        response.put("count", filtered.size());
        response.put("categories", ExpenseService.VALID_CATEGORIES);

        return ResponseEntity.ok(response);
    }

    // ─── POST /api/expenses ───────────────────────────────────────────────────

    @PostMapping("/expenses")
    public ResponseEntity<Map<String, Object>> addExpense(
            @RequestBody Map<String, String> body) {

        // Extract and validate amount
        String amountStr = body.getOrDefault("amount", "").trim();
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Please enter a valid numeric amount."));
        }
        if (amount <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Amount must be greater than ₹0."));
        }

        // Validate category
        String category = body.getOrDefault("category", "").trim();
        if (category.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Please select a category."));
        }
        if (!ExpenseService.VALID_CATEGORIES.contains(category)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid category selected."));
        }

        // Validate date
        String dateStr = body.getOrDefault("date", "").trim();
        LocalDate date;
        try {
            date = LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Please enter a valid date (YYYY-MM-DD)."));
        }

        String description = body.getOrDefault("description", "").trim();

        try {
            Expense expense = expenseService.addExpense(amount, category, date, description);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("expense", expense);
            response.put("message", String.format("₹%.0f · %s · %s added successfully.",
                    amount, category, date.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"))));
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ─── GET /api/report ──────────────────────────────────────────────────────

    @GetMapping("/report")
    public ResponseEntity<Map<String, Object>> getReport(
            @RequestParam(defaultValue = "0") int month,
            @RequestParam(defaultValue = "0") int year) {

        // Default to current month if not provided
        YearMonth ym;
        try {
            if (month == 0 || year == 0) {
                ym = YearMonth.now();
            } else {
                ym = YearMonth.of(year, month);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid month or year."));
        }

        List<Expense> monthExpenses = expenseService.getExpensesForMonth(ym);
        Map<String, Double> categoryTotals = expenseService.getCategoryTotals(monthExpenses);

        String monthLabel = ym.getMonth().getDisplayName(
                java.time.format.TextStyle.FULL, Locale.ENGLISH) + " " + ym.getYear();

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("month",                   ym.getMonthValue());
        report.put("year",                    ym.getYear());
        report.put("monthLabel",              monthLabel);
        report.put("totalSpending",           expenseService.getTotalSpendingForMonth(ym));
        report.put("transactionCount",        expenseService.getTransactionCountForMonth(ym));
        report.put("averageExpense",          expenseService.getAverageExpenseForMonth(ym));
        report.put("highestSingleExpense",    expenseService.getHighestSingleExpenseForMonth(ym));
        report.put("highestCategory",         expenseService.getHighestCategoryForMonth(ym));
        report.put("highestCategoryAmount",   expenseService.getHighestCategoryAmountForMonth(ym));
        report.put("categoryTotals",          categoryTotals);
        report.put("expenses",               monthExpenses);
        report.put("isEmpty",                monthExpenses.isEmpty());

        return ResponseEntity.ok(report);
    }

    // ─── DELETE /api/expenses/{id} ────────────────────────────────────────────

    @DeleteMapping("/expenses/{id}")
    public ResponseEntity<Map<String, Object>> deleteExpense(@PathVariable Long id) {
        if (id == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid expense ID."));
        }
        boolean deleted = expenseService.deleteExpense(id);
        if (deleted) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Expense deleted."));
        }
        return ResponseEntity.status(404).body(Map.of("error", "Expense not found."));
    }

    // ─── GET /api/categories ──────────────────────────────────────────────────

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(ExpenseService.VALID_CATEGORIES);
    }
}

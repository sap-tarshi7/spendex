package com.expensetracker.util;

import com.expensetracker.model.Expense;
import com.expensetracker.model.Investment;
import com.expensetracker.model.InvestmentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FileHandler — manages CSV file persistence for expense data.
 * Demonstrates Java File I/O, exception handling, and malformed data recovery.
 */
@Component
public class FileHandler {

    private static final Logger LOGGER = Logger.getLogger(FileHandler.class.getName());
    private static final String CSV_HEADER = "date,category,description,amount";
    private static final String INV_CSV_HEADER = "id,name,symbol,exchange,type,quantity,purchasePrice,currentPrice,purchaseDate,notes";

    @Value("${expense.data.file:data/expenses.csv}")
    private String filePath;

    @Value("${investment.data.file:data/investments.csv}")
    private String invFilePath;

    /**
     * Loads all expenses from the CSV file.
     * Returns empty list if file doesn't exist or is empty.
     * Skips any malformed line without crashing.
     */
    public List<Expense> loadExpenses() {
        List<Expense> expenses = new ArrayList<>();
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            LOGGER.info("Data file not found at: " + path.toAbsolutePath() + " — starting fresh.");
            return expenses;
        }

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();

                // Skip header and blank lines
                if (line.isEmpty() || line.equalsIgnoreCase(CSV_HEADER)) {
                    continue;
                }

                Expense parsed = parseLine(line, lineNumber);
                if (parsed != null) {
                    expenses.add(parsed);
                }
            }

            LOGGER.info("Loaded " + expenses.size() + " expenses from: " + path.toAbsolutePath());

        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error reading file: " + e.getMessage(), e);
        }

        return expenses;
    }

    /**
     * Saves all expenses to the CSV file.
     * Creates parent directories if they don't exist.
     */
    public boolean saveExpenses(List<Expense> expenses) {
        Path path = Paths.get(filePath);

        try {
            // Ensure parent directory exists
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                writer.write(CSV_HEADER);
                writer.newLine();
                for (Expense e : expenses) {
                    writer.write(e.toCsvLine());
                    writer.newLine();
                }
            }

            LOGGER.info("Saved " + expenses.size() + " expenses to file.");
            return true;

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error saving expenses: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Parses a single CSV line into an Expense object.
     * Returns null if the line is malformed, with a warning log.
     * CSV format: date,category,description,amount
     */
    private Expense parseLine(String line, int lineNumber) {
        // Split on comma, max 4 parts (description may contain semicolons)
        String[] parts = line.split(",", 4);

        if (parts.length < 4) {
            LOGGER.warning("Skipping malformed line " + lineNumber + " (needs 4 fields): " + line);
            return null;
        }

        try {
            LocalDate date = LocalDate.parse(parts[0].trim());
            String category = parts[1].trim();
            String description = parts[2].trim().replace(";", ",");
            double amount = Double.parseDouble(parts[3].trim());

            if (category.isEmpty()) {
                LOGGER.warning("Skipping line " + lineNumber + ": empty category");
                return null;
            }
            if (amount <= 0) {
                LOGGER.warning("Skipping line " + lineNumber + ": amount must be > 0, got: " + amount);
                return null;
            }

            return new Expense(amount, category, date, description);

        } catch (DateTimeParseException e) {
            LOGGER.warning("Skipping line " + lineNumber + ": bad date '" + parts[0] + "'");
            return null;
        } catch (NumberFormatException e) {
            LOGGER.warning("Skipping line " + lineNumber + ": bad amount '" + parts[3] + "'");
            return null;
        }
    }

    public String getFilePath() {
        return Paths.get(filePath).toAbsolutePath().toString();
    }

    public List<Investment> loadInvestments() {
        List<Investment> investments = new ArrayList<>();
        Path path = Paths.get(invFilePath);

        if (!Files.exists(path)) {
            LOGGER.info("Investment data file not found at: " + path.toAbsolutePath() + " — starting fresh.");
            return investments;
        }

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();

                if (line.isEmpty() || line.equalsIgnoreCase(INV_CSV_HEADER)) {
                    continue;
                }

                Investment parsed = parseInvLine(line, lineNumber);
                if (parsed != null) {
                    investments.add(parsed);
                }
            }
            LOGGER.info("Loaded " + investments.size() + " investments from: " + path.toAbsolutePath());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error reading investment file: " + e.getMessage(), e);
        }
        return investments;
    }

    public boolean saveInvestments(List<Investment> investments) {
        Path path = Paths.get(invFilePath);
        try {
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                writer.write(INV_CSV_HEADER);
                writer.newLine();
                for (Investment i : investments) {
                    writer.write(i.toCsvLine());
                    writer.newLine();
                }
            }
            LOGGER.info("Saved " + investments.size() + " investments to file.");
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error saving investments: " + e.getMessage(), e);
            return false;
        }
    }

    private Investment parseInvLine(String line, int lineNumber) {
        String[] parts = line.split(",", 10);
        
        // Handle migration from old 8-field format
        if (parts.length == 8 && !line.contains(",STOCKS,") && !line.contains(",MUTUAL_FUNDS,") && !line.contains(",ETFS,") && !line.contains(",FIXED_DEPOSITS,") && !line.contains(",GOLD,") && !line.contains(",OTHER,")) {
             LOGGER.warning("Skipping malformed investment line " + lineNumber + ": " + line);
             return null;
        }

        try {
            String id = parts[0].trim();
            String name = parts[1].trim().replace(";", ",");
            String symbol = "";
            String exchange = "";
            int typeIdx = 2;
            
            // Check if this is the new 10-field format or old 8-field format
            if (parts.length >= 9 || (parts.length == 8 && (parts[4].equals("STOCKS") || parts[4].equals("MUTUAL_FUNDS") || parts[4].equals("ETFS") || parts[4].equals("FIXED_DEPOSITS") || parts[4].equals("GOLD") || parts[4].equals("OTHER")))) {
                 // It's likely the new format but might be truncated if notes are empty
                 // Or it's definitely new if it has 9 or 10 fields
                 // Wait, if it has 8 fields but type is at index 4, it means symbol and exchange are there, but notes/date might be missing?
                 // Let's rely on standard parsing for the new format:
            }
            
            // Safer parsing: try to detect where the type is (old format type is at index 2, new format type is at index 4)
            if (parts.length >= 4 && isValidType(parts[4].trim())) {
                symbol = parts[2].trim();
                exchange = parts[3].trim();
                typeIdx = 4;
            } else if (parts.length >= 3 && isValidType(parts[2].trim())) {
                // Old format
                typeIdx = 2;
            } else {
                LOGGER.warning("Could not determine type index for line " + lineNumber);
                return null;
            }

            InvestmentType type = InvestmentType.fromString(parts[typeIdx].trim());
            BigDecimal quantity = new BigDecimal(parts[typeIdx + 1].trim());
            BigDecimal purchasePrice = new BigDecimal(parts[typeIdx + 2].trim());
            BigDecimal currentPrice = new BigDecimal(parts[typeIdx + 3].trim());
            LocalDate purchaseDate = LocalDate.parse(parts[typeIdx + 4].trim());
            String notes = parts.length > (typeIdx + 5) ? parts[typeIdx + 5].trim().replace(";", ",") : "";

            if (name.isEmpty() || quantity.compareTo(BigDecimal.ZERO) <= 0 || purchasePrice.compareTo(BigDecimal.ZERO) <= 0 || currentPrice.compareTo(BigDecimal.ZERO) < 0) {
                LOGGER.warning("Skipping line " + lineNumber + ": invalid investment values.");
                return null;
            }
            return new Investment(id, name, symbol, exchange, type, quantity, purchasePrice, currentPrice, purchaseDate, notes);
        } catch (DateTimeParseException | NumberFormatException e) {
            LOGGER.warning("Skipping line " + lineNumber + " due to parsing error: " + e.getMessage());
            return null;
        } catch (Exception e) {
            LOGGER.warning("Skipping line " + lineNumber + " due to unexpected error: " + e.getMessage());
            return null;
        }
    }
    
    private boolean isValidType(String val) {
        for (InvestmentType b : InvestmentType.values()) {
            if (b.name().equalsIgnoreCase(val) || b.getDisplayName().equalsIgnoreCase(val)) {
                return true;
            }
        }
        return false;
    }
}

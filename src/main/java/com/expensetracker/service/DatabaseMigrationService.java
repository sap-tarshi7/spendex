package com.expensetracker.service;

import com.expensetracker.model.Expense;
import com.expensetracker.model.Investment;
import com.expensetracker.util.FileHandler;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class DatabaseMigrationService {

    private static final Logger LOGGER = Logger.getLogger(DatabaseMigrationService.class.getName());

    private final ExpenseRepository expenseRepository;
    private final InvestmentRepository investmentRepository;
    private final FileHandler fileHandler;

    public DatabaseMigrationService(ExpenseRepository expenseRepository,
                                    InvestmentRepository investmentRepository,
                                    FileHandler fileHandler) {
        this.expenseRepository = expenseRepository;
        this.investmentRepository = investmentRepository;
        this.fileHandler = fileHandler;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void migrateCsvToDatabase() {
        LOGGER.info("Checking for CSV data to migrate to MySQL...");
        
        migrateExpenses();
        migrateInvestments();
    }

    private void migrateExpenses() {
        Path csvPath = Paths.get(fileHandler.getFilePath());
        if (Files.exists(csvPath)) {
            LOGGER.info("Found expenses.csv. Starting migration...");
            List<Expense> expenses = fileHandler.loadExpenses();
            
            // Re-assign IDs to null so MySQL auto-generates them (Long)
            for (Expense e : expenses) {
                e.setId(null);
            }
            
            expenseRepository.saveAll(expenses);
            LOGGER.info("Migrated " + expenses.size() + " expenses to MySQL.");
            
            // Mark as migrated
            try {
                Path migratedPath = Paths.get(csvPath.toString() + ".migrated");
                Files.move(csvPath, migratedPath, StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("Renamed expenses.csv to expenses.csv.migrated to prevent future re-imports.");
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to rename expenses.csv after migration", e);
            }
        } else {
            LOGGER.info("No expenses.csv found for migration (already migrated or fresh start).");
        }
    }

    private void migrateInvestments() {
        Path csvPath = Paths.get(fileHandler.getInvFilePath());
        if (Files.exists(csvPath)) {
            LOGGER.info("Found investments.csv. Starting migration...");
            List<Investment> investments = fileHandler.loadInvestments();
            
            for (Investment i : investments) {
                i.setId(null);
            }
            
            investmentRepository.saveAll(investments);
            LOGGER.info("Migrated " + investments.size() + " investments to MySQL.");
            
            try {
                Path migratedPath = Paths.get(csvPath.toString() + ".migrated");
                Files.move(csvPath, migratedPath, StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("Renamed investments.csv to investments.csv.migrated to prevent future re-imports.");
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to rename investments.csv after migration", e);
            }
        } else {
            LOGGER.info("No investments.csv found for migration (already migrated or fresh start).");
        }
    }
}

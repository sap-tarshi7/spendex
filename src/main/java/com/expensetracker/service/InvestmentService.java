package com.expensetracker.service;

import com.expensetracker.model.Investment;
import com.expensetracker.model.InvestmentSummary;
import com.expensetracker.model.InvestmentType;
import com.expensetracker.service.market.MarketDataService;
import com.expensetracker.service.market.MarketQuote;
import com.expensetracker.service.market.MarketStatus;
import com.expensetracker.util.FileHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InvestmentService {

    private final FileHandler fileHandler;
    private final MarketDataService marketDataService;
    private final List<Investment> investments = new ArrayList<>();

    @Autowired
    public InvestmentService(FileHandler fileHandler, MarketDataService marketDataService) {
        this.fileHandler = fileHandler;
        this.marketDataService = marketDataService;
    }

    @PostConstruct
    public void init() {
        investments.addAll(fileHandler.loadInvestments());
    }

    public List<Investment> getAllInvestments() {
        return new ArrayList<>(investments);
    }

    public Investment addInvestment(Investment investment) {
        investment.setId(UUID.randomUUID().toString());
        investments.add(investment);
        save();
        return investment;
    }

    public Optional<Investment> getInvestmentById(String id) {
        return investments.stream().filter(i -> i.getId().equals(id)).findFirst();
    }

    public Optional<Investment> updateInvestment(String id, Investment updatedData) {
        return getInvestmentById(id).map(existing -> {
            existing.setName(updatedData.getName());
            existing.setSymbol(updatedData.getSymbol());
            existing.setExchange(updatedData.getExchange());
            existing.setType(updatedData.getType());
            existing.setQuantity(updatedData.getQuantity());
            existing.setPurchasePrice(updatedData.getPurchasePrice());
            existing.setCurrentPrice(updatedData.getCurrentPrice());
            existing.setPurchaseDate(updatedData.getPurchaseDate());
            existing.setNotes(updatedData.getNotes());
            save();
            return existing;
        });
    }

    public boolean deleteInvestment(String id) {
        boolean removed = investments.removeIf(i -> i.getId().equals(id));
        if (removed) {
            save();
        }
        return removed;
    }

    public InvestmentSummary getSummary() {
        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal currentValue = BigDecimal.ZERO;
        Map<String, BigDecimal> allocationSum = new HashMap<>();
        
        MarketStatus overallStatus = MarketStatus.CLOSED;
        java.time.LocalDateTime latestUpdate = null;

        for (Investment i : investments) {
            // Dynamically attach the latest quote if applicable
            if ((i.getType() == InvestmentType.STOCKS || i.getType() == InvestmentType.ETFS || i.getType() == InvestmentType.MUTUAL_FUNDS) 
                 && i.getSymbol() != null && !i.getSymbol().isEmpty()) {
                MarketQuote quote = marketDataService.getLatestQuote(i.getSymbol(), i.getExchange());
                i.setLastQuote(quote);
                
                if (quote != null) {
                    if (quote.getStatus() == MarketStatus.LIVE) overallStatus = MarketStatus.LIVE;
                    else if (quote.getStatus() == MarketStatus.DELAYED && overallStatus != MarketStatus.LIVE) overallStatus = MarketStatus.DELAYED;
                    
                    if (latestUpdate == null || (quote.getTimestamp() != null && quote.getTimestamp().isAfter(latestUpdate))) {
                        latestUpdate = quote.getTimestamp();
                    }
                }
            }
            
            totalInvested = totalInvested.add(i.getInvestedAmount());
            currentValue = currentValue.add(i.getCurrentValue());
            
            String typeName = i.getType() != null ? i.getType().getDisplayName() : InvestmentType.OTHER.getDisplayName();
            allocationSum.put(typeName, allocationSum.getOrDefault(typeName, BigDecimal.ZERO).add(i.getCurrentValue()));
        }

        BigDecimal profitLoss = currentValue.subtract(totalInvested);
        BigDecimal returnPercentage = BigDecimal.ZERO;
        if (totalInvested.compareTo(BigDecimal.ZERO) > 0) {
            returnPercentage = profitLoss.divide(totalInvested, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
        }

        Map<String, Double> assetAllocation = new HashMap<>();
        if (currentValue.compareTo(BigDecimal.ZERO) > 0) {
            for (Map.Entry<String, BigDecimal> entry : allocationSum.entrySet()) {
                double pct = entry.getValue().divide(currentValue, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).doubleValue();
                assetAllocation.put(entry.getKey(), pct);
            }
        }

        List<Investment> recent = investments.stream()
                .sorted(Comparator.comparing(Investment::getPurchaseDate).reversed())
                .collect(Collectors.toList());

        if (latestUpdate == null) {
            latestUpdate = java.time.LocalDateTime.now();
            overallStatus = MarketStatus.UNAVAILABLE;
        }

        return new InvestmentSummary(totalInvested, currentValue, profitLoss, returnPercentage, assetAllocation, recent, latestUpdate, overallStatus);
    }

    private void save() {
        fileHandler.saveInvestments(investments);
    }
}

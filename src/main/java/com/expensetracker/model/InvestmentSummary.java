package com.expensetracker.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import com.expensetracker.service.market.MarketStatus;

public class InvestmentSummary {
    private BigDecimal totalInvested;
    private BigDecimal currentValue;
    private BigDecimal totalProfitLoss;
    private BigDecimal returnPercentage;
    private Map<String, Double> assetAllocation;
    private List<Investment> recentInvestments;
    private LocalDateTime lastUpdated;
    private MarketStatus marketStatus;

    public InvestmentSummary() {}

    public InvestmentSummary(BigDecimal totalInvested, BigDecimal currentValue, BigDecimal totalProfitLoss, BigDecimal returnPercentage, Map<String, Double> assetAllocation, List<Investment> recentInvestments, LocalDateTime lastUpdated, MarketStatus marketStatus) {
        this.totalInvested = totalInvested;
        this.currentValue = currentValue;
        this.totalProfitLoss = totalProfitLoss;
        this.returnPercentage = returnPercentage;
        this.assetAllocation = assetAllocation;
        this.recentInvestments = recentInvestments;
        this.lastUpdated = lastUpdated;
        this.marketStatus = marketStatus;
    }

    public BigDecimal getTotalInvested() { return totalInvested; }
    public void setTotalInvested(BigDecimal totalInvested) { this.totalInvested = totalInvested; }

    public BigDecimal getCurrentValue() { return currentValue; }
    public void setCurrentValue(BigDecimal currentValue) { this.currentValue = currentValue; }

    public BigDecimal getTotalProfitLoss() { return totalProfitLoss; }
    public void setTotalProfitLoss(BigDecimal totalProfitLoss) { this.totalProfitLoss = totalProfitLoss; }

    public BigDecimal getReturnPercentage() { return returnPercentage; }
    public void setReturnPercentage(BigDecimal returnPercentage) { this.returnPercentage = returnPercentage; }

    public Map<String, Double> getAssetAllocation() { return assetAllocation; }
    public void setAssetAllocation(Map<String, Double> assetAllocation) { this.assetAllocation = assetAllocation; }
    
    public List<Investment> getRecentInvestments() { return recentInvestments; }
    public void setRecentInvestments(List<Investment> recentInvestments) { this.recentInvestments = recentInvestments; }
    
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    public MarketStatus getMarketStatus() { return marketStatus; }
    public void setMarketStatus(MarketStatus marketStatus) { this.marketStatus = marketStatus; }
}

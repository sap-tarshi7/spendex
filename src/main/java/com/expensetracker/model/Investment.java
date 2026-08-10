package com.expensetracker.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import com.expensetracker.service.market.MarketQuote;

@Entity
@Table(name = "investments")
public class Investment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String symbol;
    private String exchange;
    
    @Enumerated(EnumType.STRING)
    private InvestmentType type;
    private BigDecimal quantity;
    @Column(name = "purchase_price")
    private BigDecimal purchasePrice;
    
    @Column(name = "current_price")
    private BigDecimal currentPrice;
    
    @Column(name = "purchase_date")
    private LocalDate purchaseDate;
    
    private String notes;
    
    @Transient
    private transient MarketQuote lastQuote;

    public Investment() {
    }

    public Investment(Long id, String name, String symbol, String exchange, InvestmentType type, BigDecimal quantity, BigDecimal purchasePrice, BigDecimal currentPrice, LocalDate purchaseDate, String notes) {
        this.id = id;
        this.name = name;
        this.symbol = symbol;
        this.exchange = exchange;
        this.type = type;
        this.quantity = quantity != null ? quantity : BigDecimal.ZERO;
        this.purchasePrice = purchasePrice != null ? purchasePrice : BigDecimal.ZERO;
        this.currentPrice = currentPrice != null ? currentPrice : BigDecimal.ZERO;
        this.purchaseDate = purchaseDate != null ? purchaseDate : LocalDate.now();
        this.notes = notes != null ? notes : "";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }

    public InvestmentType getType() { return type; }
    public void setType(InvestmentType type) { this.type = type; }
    
    // Virtual property for JSON serialization
    public String getTypeName() {
        return type != null ? type.getDisplayName() : "";
    }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }

    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }

    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public MarketQuote getLastQuote() { return lastQuote; }
    public void setLastQuote(MarketQuote lastQuote) { this.lastQuote = lastQuote; }

    // Calculated fields
    public BigDecimal getInvestedAmount() {
        if (quantity == null || purchasePrice == null) return BigDecimal.ZERO;
        return quantity.multiply(purchasePrice).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getCurrentValue() {
        if (quantity == null) return BigDecimal.ZERO;
        
        BigDecimal priceToUse = currentPrice != null ? currentPrice : BigDecimal.ZERO;
        if ((type == InvestmentType.STOCKS || type == InvestmentType.ETFS || type == InvestmentType.MUTUAL_FUNDS) && lastQuote != null && lastQuote.getPrice() != null) {
            priceToUse = lastQuote.getPrice();
        }
        
        return quantity.multiply(priceToUse).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getProfitLoss() {
        return getCurrentValue().subtract(getInvestedAmount());
    }

    public BigDecimal getReturnPercentage() {
        BigDecimal invested = getInvestedAmount();
        if (invested.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        
        return getProfitLoss().divide(invested, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }
    
    public String toCsvLine() {
        return String.join(",", 
            id != null ? String.valueOf(id) : "", 
            name.replace(",", ";"), 
            symbol != null ? symbol.replace(",", "") : "",
            exchange != null ? exchange.replace(",", "") : "",
            type.name(),
            quantity.toString(), 
            purchasePrice.toString(), 
            currentPrice.toString(), 
            purchaseDate.toString(), 
            notes.replace(",", ";")
        );
    }
}

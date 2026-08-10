package com.expensetracker.service.market;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MarketQuote {
    private String symbol;
    private String exchange;
    private BigDecimal price;
    private LocalDateTime timestamp;
    private String source;
    private MarketStatus status;

    public MarketQuote() {}

    public MarketQuote(String symbol, String exchange, BigDecimal price, LocalDateTime timestamp, String source, MarketStatus status) {
        this.symbol = symbol;
        this.exchange = exchange;
        this.price = price;
        this.timestamp = timestamp;
        this.source = source;
        this.status = status;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public MarketStatus getStatus() { return status; }
    public void setStatus(MarketStatus status) { this.status = status; }
}

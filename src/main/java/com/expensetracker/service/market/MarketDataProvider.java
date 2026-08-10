package com.expensetracker.service.market;

public interface MarketDataProvider {
    MarketQuote getQuote(String symbol, String exchange);
}

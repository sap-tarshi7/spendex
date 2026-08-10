package com.expensetracker.service.market;

import org.springframework.context.ApplicationEvent;

public class MarketQuoteUpdatedEvent extends ApplicationEvent {
    private final MarketQuote quote;

    public MarketQuoteUpdatedEvent(Object source, MarketQuote quote) {
        super(source);
        this.quote = quote;
    }

    public MarketQuote getQuote() {
        return quote;
    }
}

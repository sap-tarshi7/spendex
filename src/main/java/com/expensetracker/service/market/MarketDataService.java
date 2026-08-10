package com.expensetracker.service.market;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@Service
public class MarketDataService {

    private static final Logger LOGGER = Logger.getLogger(MarketDataService.class.getName());
    
    private final MarketDataProvider fallbackProvider;
    private final ApplicationEventPublisher eventPublisher;
    
    // Simple thread-safe cache: "SYMBOL:EXCHANGE" -> MarketQuote
    private final Map<String, MarketQuote> cache = new ConcurrentHashMap<>();

    @Autowired
    public MarketDataService(@Qualifier("yahooFinanceProvider") MarketDataProvider fallbackProvider, ApplicationEventPublisher eventPublisher) {
        this.fallbackProvider = fallbackProvider;
        this.eventPublisher = eventPublisher;
    }

    public MarketQuote getLatestQuote(String symbol, String exchange) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return null;
        }
        String cacheKey = generateKey(symbol, exchange);
        return cache.get(cacheKey);
    }

    /**
     * Called by the real-time websocket provider when a tick arrives.
     */
    public void onMarketTick(MarketQuote quote) {
        if (quote == null || quote.getSymbol() == null) return;
        
        String cacheKey = generateKey(quote.getSymbol(), quote.getExchange());
        cache.put(cacheKey, quote);
        
        // Immediately notify listeners so the SSE stream pushes without waiting for the scheduler
        eventPublisher.publishEvent(new MarketQuoteUpdatedEvent(this, quote));
    }

    /**
     * Refreshes a quote using the fallback (delayed) provider, but ONLY if
     * we don't already have a LIVE quote from the realtime provider.
     */
    public void refreshQuoteWithFallback(String symbol, String exchange) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return;
        }
        
        String cacheKey = generateKey(symbol, exchange);
        MarketQuote existing = cache.get(cacheKey);
        
        // Don't overwrite a LIVE realtime quote with a DELAYED fallback quote
        if (existing != null && existing.getStatus() == MarketStatus.LIVE) {
            return;
        }
        
        MarketQuote newQuote = fallbackProvider.getQuote(symbol, exchange);
        
        if (newQuote != null && newQuote.getStatus() != MarketStatus.UNAVAILABLE && newQuote.getPrice() != null) {
            cache.put(cacheKey, newQuote);
        } else {
            // If API failed, keep the old quote but mark it UNAVAILABLE to signal freshness issues
            if (existing != null) {
                existing.setStatus(MarketStatus.UNAVAILABLE);
                existing.setTimestamp(LocalDateTime.now());
            } else {
                cache.put(cacheKey, newQuote != null ? newQuote : new MarketQuote(symbol, exchange, null, LocalDateTime.now(), "Unknown", MarketStatus.UNAVAILABLE));
            }
        }
    }

    /**
     * Marks all cached LIVE quotes as CONNECTION_LOST when the websocket disconnects.
     */
    public void markProviderDisconnected() {
        boolean changed = false;
        for (MarketQuote quote : cache.values()) {
            if (quote.getStatus() == MarketStatus.LIVE) {
                quote.setStatus(MarketStatus.CONNECTION_LOST);
                changed = true;
            }
        }
        if (changed) {
            eventPublisher.publishEvent(new MarketQuoteUpdatedEvent(this, null));
        }
    }

    private String generateKey(String symbol, String exchange) {
        return symbol.toUpperCase().trim() + ":" + (exchange != null ? exchange.toUpperCase().trim() : "NONE");
    }
}

package com.expensetracker.service.market;

import java.util.Set;

public interface StreamingMarketDataProvider extends MarketDataProvider {
    /**
     * Updates the active subscriptions for the streaming provider.
     * @param symbols A set of unique string identifiers to subscribe to (e.g. RELIANCE:NSE)
     */
    void updateSubscriptions(Set<String> symbols);
    
    /**
     * Checks if the streaming provider is currently active and connected.
     */
    boolean isConnected();
}

package com.expensetracker.service.market;

import com.expensetracker.model.Investment;
import com.expensetracker.model.InvestmentType;
import com.expensetracker.service.InvestmentService;
import com.expensetracker.service.InvestmentStreamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.logging.Logger;

@Component
public class MarketUpdateScheduler {

    private static final Logger LOGGER = Logger.getLogger(MarketUpdateScheduler.class.getName());
    
    private final InvestmentService investmentService;
    private final MarketDataService marketDataService;
    private final InvestmentStreamService streamService;
    private final StreamingMarketDataProvider realtimeMarketProvider;
    
    private boolean isUpdating = false;

    @Autowired
    public MarketUpdateScheduler(InvestmentService investmentService, 
                                 MarketDataService marketDataService, 
                                 InvestmentStreamService streamService,
                                 @Autowired(required = false) StreamingMarketDataProvider realtimeMarketProvider) {
        this.investmentService = investmentService;
        this.marketDataService = marketDataService;
        this.streamService = streamService;
        this.realtimeMarketProvider = realtimeMarketProvider;
    }

    // Listens for ticks pushed by the Real-Time WebSocket and broadcasts them
    @EventListener
    public void handleMarketQuoteUpdatedEvent(MarketQuoteUpdatedEvent event) {
        streamService.broadcastUpdate(investmentService.getSummary());
    }

    // Run every 15 seconds. Ensure they don't overlap.
    // This now strictly serves as the DELAYED FALLBACK poller.
    @Scheduled(fixedDelay = 15000)
    public void fetchLatestPricesAndBroadcast() {
        if (isUpdating) return;
        isUpdating = true;
        
        try {
            // 1. Find all unique market symbols in the portfolio
            Set<String> uniqueSymbols = investmentService.getAllInvestments().stream()
                    .filter(i -> (i.getType() == InvestmentType.STOCKS || i.getType() == InvestmentType.ETFS || i.getType() == InvestmentType.MUTUAL_FUNDS))
                    .filter(i -> i.getSymbol() != null && !i.getSymbol().trim().isEmpty())
                    .map(i -> i.getSymbol() + ":" + (i.getExchange() != null ? i.getExchange() : ""))
                    .collect(Collectors.toSet());

            if (uniqueSymbols.isEmpty()) {
                return;
            }
            
            // 2. Sync subscriptions with the real-time provider (if one is active)
            if (realtimeMarketProvider != null) {
                realtimeMarketProvider.updateSubscriptions(uniqueSymbols);
            }
            
            // 3. Fetch latest quotes for each unique symbol using the FALLBACK provider
            // MarketDataService.refreshQuoteWithFallback handles skipping if a LIVE quote exists
            for (String composite : uniqueSymbols) {
                String[] parts = composite.split(":", 2);
                String symbol = parts[0];
                String exchange = parts.length > 1 && !parts[1].isEmpty() ? parts[1] : null;
                
                marketDataService.refreshQuoteWithFallback(symbol, exchange);
            }
            
            // 4. Broadcast the updated summary to all connected clients
            streamService.broadcastUpdate(investmentService.getSummary());
            
        } catch (Exception e) {
            LOGGER.warning("Error during delayed fallback update: " + e.getMessage());
        } finally {
            isUpdating = false;
        }
    }
}

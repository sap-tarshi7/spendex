package com.expensetracker.service.market;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class YahooFinanceProvider implements MarketDataProvider {

    private static final Logger LOGGER = Logger.getLogger(YahooFinanceProvider.class.getName());
    private final HttpClient httpClient;
    private static final String YAHOO_URL = "https://query1.finance.yahoo.com/v8/finance/chart/%s";

    public YahooFinanceProvider() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public MarketQuote getQuote(String symbol, String exchange) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return null;
        }

        String formattedSymbol = formatSymbol(symbol.trim(), exchange);
        
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(String.format(YAHOO_URL, formattedSymbol)))
                    .timeout(Duration.ofSeconds(5))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseYahooResponse(response.body(), symbol, exchange);
            } else {
                LOGGER.warning("Yahoo Finance returned status " + response.statusCode() + " for " + formattedSymbol);
                return createUnavailableQuote(symbol, exchange);
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to fetch quote for " + formattedSymbol + ": " + e.getMessage());
            return createUnavailableQuote(symbol, exchange);
        }
    }

    private String formatSymbol(String symbol, String exchange) {
        String base = symbol.toUpperCase();
        if (exchange == null) return base;
        
        return switch (exchange.toUpperCase().trim()) {
            case "NSE" -> base + ".NS";
            case "BSE" -> base + ".BO";
            case "LSE" -> base + ".L";
            default -> base;
        };
    }

    private MarketQuote parseYahooResponse(String json, String originalSymbol, String originalExchange) {
        // Quick regex to extract regularMarketPrice and regularMarketTime/marketState without heavy Jackson mapping
        // since the JSON structure from Yahoo v8 is deeply nested.
        
        Pattern pricePattern = Pattern.compile("\"regularMarketPrice\":([0-9.]+)");
        Pattern statePattern = Pattern.compile("\"marketState\":\"([A-Z]+)\"");
        
        Matcher priceMatcher = pricePattern.matcher(json);
        Matcher stateMatcher = statePattern.matcher(json);
        
        if (priceMatcher.find()) {
            BigDecimal price = new BigDecimal(priceMatcher.group(1));
            
            MarketStatus status = MarketStatus.DELAYED; // Default safe assumption for Yahoo
            if (stateMatcher.find()) {
                String state = stateMatcher.group(1);
                if ("REGULAR".equals(state)) {
                    status = MarketStatus.LIVE;
                } else if ("CLOSED".equals(state) || "POSTPOST".equals(state) || "PREPRE".equals(state)) {
                    status = MarketStatus.CLOSED;
                }
            }
            
            return new MarketQuote(
                    originalSymbol, 
                    originalExchange, 
                    price, 
                    LocalDateTime.now(), 
                    "Yahoo Finance", 
                    status
            );
        }
        
        return createUnavailableQuote(originalSymbol, originalExchange);
    }
    
    private MarketQuote createUnavailableQuote(String symbol, String exchange) {
        return new MarketQuote(symbol, exchange, null, LocalDateTime.now(), "Yahoo Finance", MarketStatus.UNAVAILABLE);
    }
}

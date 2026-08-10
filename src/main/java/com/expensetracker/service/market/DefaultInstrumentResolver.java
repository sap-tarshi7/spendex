package com.expensetracker.service.market;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;

@Service
public class DefaultInstrumentResolver implements InstrumentResolver {
    
    private static final Logger log = LoggerFactory.getLogger(DefaultInstrumentResolver.class);
    
    public void init() {
        log.info("Initializing InstrumentResolver. Using Yahoo Finance fallback resolution.");
    }

    @Override
    public String resolveInternalIdentifier(String symbol, String exchange, String type) {
        if (symbol == null || exchange == null) {
            return null;
        }
        
        // Fallback for Yahoo if Upstox mapping isn't found or downloading failed
        if ("NSE".equalsIgnoreCase(exchange)) {
            return symbol.toUpperCase() + ".NS";
        } else if ("BSE".equalsIgnoreCase(exchange)) {
            return symbol.toUpperCase() + ".BO";
        }
        
        return symbol.toUpperCase();
    }
}

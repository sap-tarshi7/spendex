package com.expensetracker.service.market;

public interface InstrumentResolver {
    /**
     * Resolves a standard symbol and exchange to a provider-specific instrument token/identifier.
     * @param symbol Standard symbol (e.g., RELIANCE)
     * @param exchange Exchange (e.g., NSE)
     * @param type Asset type (e.g., EQUITY, DERIVATIVE)
     * @return The provider's instrument identifier, or null if unknown.
     */
    String resolveInternalIdentifier(String symbol, String exchange, String type);
}

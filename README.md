# SPENDEX

SPENDEX is an India-first personal finance and wealth-management application built with Java and Spring Boot. It provides a comprehensive platform that combines everyday expense tracking with active investment management, portfolio valuation, and profit/loss monitoring for Indian market-linked investments. SPENDEX is designed with a professional, editorial European aesthetic to deliver a premium wealth-management experience.

## Features

- **Expense CRUD**: Track, categorize, and manage your daily expenses.
- **Investment CRUD**: Add, edit, and delete portfolio holdings.
- **MySQL Persistence**: Robust data storage using Spring Data JPA.
- **Indian Stock Support**: Auto-completion and tracking for NSE-focused symbols (e.g., RELIANCE · NSE, TCS · NSE, INFY · NSE, HDFCBANK · NSE, ICICIBANK · NSE, SBIN · NSE, ITC · NSE).
- **Indian Financial Formatting**: Built-in support for INR currency presentation and Lakhs/Crores number formatting.
- **Portfolio Valuation**: Dynamic calculation of current portfolio worth.
- **Profit/Loss Calculations**: Automatic computation of total returns and monetary gains or losses.
- **Return Percentage**: Track the relative performance of your investments.
- **Asset Allocation**: Understand your exposure across different asset classes (Stocks, Gold, Fixed Deposits).
- **SSE-Based Browser Updates**: Targeted DOM updates deliver fresh financial data without full page reloads.
- **Responsive Editorial UI**: Clean, premium, paper-like interface with subtle depth and charcoal typography.
- **Market-Price Retrieval**: Fetches market quotes from Yahoo Finance.

## Market Data

SPENDEX currently uses Yahoo Finance for market-data retrieval. These quotes are delayed and are explicitly labelled `DELAYED` in the application. SPENDEX does not generate fake or simulated market prices. If a quote cannot be retrieved due to network or rate limits, the application preserves the last known value rather than incorrectly replacing it with zero. 

The SSE architecture provides dynamic delivery of updated values to the browser, but it does not turn delayed Yahoo Finance data into real-time market data. No Upstox or Zerodha integration is currently active.

## Profit & Loss

SPENDEX uses the following formulas to compute portfolio performance whenever new market quotes are received:

- **Invested Value** = Quantity × Purchase Price
- **Current Value** = Quantity × Current Market Price
- **Profit/Loss** = Current Value − Invested Value
- **Return %** = (Profit/Loss ÷ Invested Value) × 100

## Real-Time UI / SSE

SPENDEX employs a unidirectional data flow for dynamic UI updates:

Yahoo Finance
↓
Market Data Service
↓
Investment Service
↓
Server-Sent Events (SSE)
↓
Browser
↓
Targeted DOM updates

- SSE avoids full-page refreshes.
- Only affected financial values are updated dynamically.
- Changed values receive subtle visual feedback.
- The frontend features a 1-second timer that only updates the local "Updated X sec ago" text. This timer does not poll the backend.
- SSE delivers updates cleanly, but it does not make delayed Yahoo Finance data real-time.

## Database Architecture

SPENDEX previously used CSV files but has now migrated to MySQL for robust persistence. 

Current architecture:

Spring Boot
↓
Spring Data JPA
↓
Hibernate
↓
MySQL

Main tables include:
- `expenses`
- `investments`

The backend relies on `ExpenseRepository` and `InvestmentRepository` interfaces for data access, alongside a `DatabaseMigrationService` for safe transitions. Market quotes are runtime data and are not permanently stored as investment records in the database.

## CSV → MySQL Migration

Earlier versions of SPENDEX used CSV files for persistence. Existing historical expense and investment records were migrated into MySQL through a specially designed `DatabaseMigrationService`. 

The migration was designed to be idempotent: successfully migrated legacy CSV files were renamed with `.migrated` to ensure data was transferred only once. MySQL is now the active persistence mechanism, and CSV is no longer used for normal CRUD operations.

## Technology Stack

| Layer | Technology |
|---|---|
| Language | Java |
| Backend | Spring Boot |
| Persistence | Spring Data JPA |
| ORM | Hibernate |
| Database | MySQL |
| Frontend | HTML, CSS, Vanilla JavaScript |
| Market Data | Yahoo Finance |
| Browser Updates | Server-Sent Events (SSE) |
| Build | Maven |

## Project Structure

```text
spendex/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/expensetracker/
│       │       ├── controller/
│       │       ├── model/
│       │       ├── service/
│       │       └── util/
│       └── resources/
│           ├── static/
│           └── application.properties
├── .gitignore
├── pom.xml
└── README.md
```

## Getting Started

### Prerequisites

- Java 21
- Maven
- MySQL
- Git

### Installation

```bash
git clone https://github.com/sap-tarshi7/spendex.git
cd spendex
```

Configure your MySQL connection in `src/main/resources/application.properties` (or via environment variables) and run the application:

```bash
mvn spring-boot:run
```

Access the application at `http://localhost:8080`.

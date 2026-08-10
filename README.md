# SPENDEX

**SPENDEX** is a premium, India-first personal finance and wealth-management application. It is designed to provide users with a clear, calm, and visually sophisticated interface for tracking both everyday expenses and long-term investments. 

The application utilizes a private wealth-management aesthetic, prioritizing editorial typography, muted colors, and subtle physical depth to present financial data without the clutter of generic SaaS dashboards.

---

## 🎯 Project Purpose

To deliver a secure, localized (India-first), and elegant personal finance experience. SPENDEX unifies expense tracking with equity portfolio management, offering users a comprehensive "wealth snapshot" powered by modern web technologies.

---

## ✨ Features

- **Expense Management:** Track, categorize, and filter daily expenses. Features client-side search, category breakdowns, and monthly reporting.
- **Investment Management:** Maintain a portfolio of Stocks, ETFs, Mutual Funds, Fixed Deposits, and Gold.
- **Indian Stock Support:** First-class support for NSE and BSE equities with built-in autocomplete for major Indian instruments (e.g., Reliance, TCS, HDFC Bank).
- **Portfolio Analytics:** Real-time calculation of Total Invested, Current Value, Profit/Loss (P/L), and Return Percentages. Includes visual asset allocation breakdowns.
- **Market-Data Architecture:** Uses an asynchronous event-driven backend. Market quotes are fetched using a delayed Yahoo Finance provider and pushed to the browser.
- **Server-Sent Events (SSE):** The frontend receives market updates through SSE, allowing the UI to gracefully flash updated values without requiring a page refresh or aggressive client-side polling.
- **P/L Calculations:** Mathematically accurate formulations ensuring precise financial tracking.
- **Premium UI/Design:** Designed with a European editorial aesthetic—off-white backgrounds, dark charcoal serif typography, and extremely subtle shadow elevations.

---

## 🛠 Tech Stack

- **Backend:** Java 21, Spring Boot 3.2, Spring Web, Spring WebFlux (for WebClient)
- **Frontend:** Vanilla HTML5, CSS3, JavaScript (ES6+). No heavy frontend frameworks to ensure maximum performance and minimal bloat.
- **Data Persistence:** Local CSV flat-file storage for lightweight, portable deployment.
- **Build Tool:** Apache Maven

---

## 🏗 Architecture

SPENDEX relies on a clean `MarketDataProvider` abstraction. 

Currently, it utilizes the `YahooFinanceProvider` to fetch quotes. A `MarketUpdateScheduler` runs on a dedicated thread, requesting updates for active symbols, which are then cached and broadcasted to connected browsers via the `InvestmentStreamService` using Server-Sent Events (SSE). 

This decoupled architecture means SPENDEX is fully prepared for a genuine WebSocket-based Indian real-time provider to be plugged in without requiring any frontend refactoring.

---

## 📂 Project Structure

```text
SPENDEX/
├── data/                      # Local CSV storage (ignored in git)
├── src/
│   └── main/
│       ├── java/              # Spring Boot backend
│       └── resources/
│           ├── application.properties
│           └── static/        # HTML, CSS, JS frontend assets
├── README.md
├── .gitignore
└── pom.xml
```

---

## 🚀 Installation & Running Locally

### Prerequisites
- **Java 21** or higher
- **Maven** 3.9+

### Steps
1. Clone the repository.
   ```bash
   git clone https://github.com/yourusername/spendex.git
   cd spendex
   ```
2. Build the application.
   ```bash
   mvn clean package
   ```
3. Run the application.
   ```bash
   mvn spring-boot:run
   ```
4. Access the UI.
   Open your browser and navigate to `http://localhost:8080`

---

## 🧪 Testing

To run the test suite, execute:
```bash
mvn test
```

---

## ⚠️ Current Limitations

- **Delayed Market Data:** The current Yahoo Finance integration provides quotes that are delayed by 15-20 minutes. The UI explicitly flags this data as **DELAYED**. SPENDEX does not simulate or fake real-time ticks.
- **CSV Storage:** Data is currently persisted in flat `.csv` files. This is excellent for local, single-user operation but lacks the concurrency features of a traditional relational database (RDBMS).

---

## 🔮 Future Scope

- **Real-Time Indian Brokerage Integration:** Plug a genuine Indian broker WebSocket API (e.g., Upstox, Zerodha) into the `StreamingMarketDataProvider` interface for live tick data.
- **Database Migration:** Transition the `FileHandler` storage mechanism to Spring Data JPA using PostgreSQL for multi-tenant capabilities.
- **Extended Asset Classes:** Add support for tracking Provident Funds (EPF/PPF) and real estate.

---

## 📄 License

This project currently has no license. All rights reserved.

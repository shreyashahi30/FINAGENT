# FinAgent — SEC Dataset Integration Guide

## Dataset expected by the application

FinAgent accepts an **unzipped SEC Financial Statement Data Set** folder. The uploaded folder should contain at least:

- `num.txt` — numerical XBRL facts
- `sub.txt` — filing/submission and company information

The SEC 2026 Q2 package also contains `tag.txt`, `pre.txt` and `readme.htm`; FinAgent accepts these files too, but the current application only needs `num.txt` and `sub.txt` for company-level extraction.

## How to run

1. Download the SEC quarterly dataset ZIP.
2. Unzip it. For example, `2026q2.zip` becomes a folder named `2026q2`.
3. Start FinAgent with `mvn spring-boot:run` or `run-windows.bat`.
4. Open `http://localhost:8080`.
5. Click **Upload & Process SEC Dataset** and select the **unzipped folder**. The browser uploads the files in that folder.
6. FinAgent streams `num.txt` rather than loading the complete 600+ MB file into memory.
7. Search for a company and select it.
8. Run **Standard Analysis** or **Adaptive Analysis**.

The raw SEC dataset is deliberately not bundled into the project ZIP. This keeps the application package small and allows the user to upload a different SEC quarter later.

## What FinAgent extracts

The preprocessing layer maps selected XBRL concepts into a company-level record. It uses consolidated, latest-date facts when available and prefers the non-segmented fact when several facts have the same date.

Main fields:

- Revenue
- Operating Income
- Net Income
- Cash and Cash Equivalents
- Total Assets
- Current Assets
- Total Liabilities
- Current Liabilities
- Stockholders' Equity
- Accounts Receivable
- Inventory
- Operating Cash Flow
- Debt (current/non-current long-term debt and selected short-term debt tags)

Derived indicators:

- Current Ratio = Current Assets / Current Liabilities
- Debt-to-Assets = extracted debt / Total Assets
- Operating Margin = Operating Income / Revenue × 100

If a required source value is missing, the corresponding risk trigger is not enabled rather than treating the missing value as zero risk or zero financial data.

## How the dataset maps to the agents

**Financial Statement Agent** receives the extracted financial profile and identifies the overall financial condition.

**Liquidity Risk Agent** can be created when Current Ratio < 1.0 and the baseline performance gate is satisfied.

**Credit Risk Agent** can be created when Debt-to-Assets > 0.60 and the baseline performance gate is satisfied.

**Market Agent** and **News/Sentiment Agent** remain part of the original baseline team, but the SEC financial-statement dataset does not provide live stock-price or news-sentiment data. In Dataset Mode the UI explicitly marks these signals as unavailable; FinAgent does not invent them.

**Risk Assessment Agent** remains the final aggregator.

## Dataset size and memory design

The uploaded 2026 Q2 package contains a very large `num.txt`. The application processes the file line-by-line and retains only the selected financial concepts needed for company-level analysis. It does not put the raw `num.txt` file into the application JAR or database.

The processed in-memory company index is written under the local runtime directory `data/sec-dataset/` so the dataset does not need to be uploaded again after restarting from the same project directory.

## Important project limitation

The SEC dataset is the source of **financial analysis inputs**. It is not the source of FinAgent's runtime performance metrics. Contribution, redundancy, efficiency and execution time remain operational performance signals used by the adaptive orchestration layer. They are not claimed to be direct measurements of financial-analysis quality, CPU utilization or token usage.

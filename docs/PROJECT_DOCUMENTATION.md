# FinAgent Project Documentation

## 1. Problem statement

A fixed multi-agent financial analysis team may perform unnecessary work in stable situations, while the same fixed team may lack specialist coverage when a specific risk becomes important. FinAgent demonstrates an adaptive alternative in which team composition is changed using explicit, inspectable rules and performance signals.

## 2. Objective

To build a local financial risk-analysis application that:

- uses multiple specialized agents;
- measures basic execution/performance signals;
- dynamically changes the team through CREATE, MERGE, REMOVE or CONTINUE;
- compares the initial and adapted teams; and
- provides an explainable report suitable for academic demonstration.

## 3. Research/evaluation question

> Can a financial-risk agent team dynamically change its composition based on risk conditions and observed team behavior, and can the resulting team reduce unnecessary work or increase specialist coverage compared with a fixed team?

## 4. Modules

| Module | Responsibility |
|---|---|
| REST Controller | Receives requests, validates input, exposes history and health endpoints |
| Analysis Service | Orchestrates baseline execution, adaptation and final execution |
| Agent layer | Performs specialist financial-risk checks |
| Performance Monitor | Aggregates time, workload, success, contribution, redundancy and efficiency |
| Evolution Decision Service | Selects CREATE / MERGE / REMOVE / CONTINUE |
| History Service | Stores recent analyses in memory for session comparison |
| Frontend | Input, Standard/Adaptive controls, report, history and export |

## 5. Why Standard Analysis exists

Standard Analysis is the experimental baseline. It keeps the four core agents fixed. Adaptive Analysis starts from the same team and is allowed to change it. This makes the two modes comparable.

## 6. Adaptive decision policy

Adaptive Analysis makes the lifecycle decision automatically; the user does not select CREATE, MERGE, REMOVE or CONTINUE. The decision service evaluates rules in priority order: CREATE for an identified specialist coverage gap, REMOVE for a low-value/high-overlap News/Sentiment case under stable conditions, MERGE for high-overlap stable Market and News analysis, and CONTINUE when no earlier rule is justified. Standard Analysis never changes the team and serves as the fixed baseline.

## 7. Threshold design

The CREATE, MERGE, REMOVE and CONTINUE decisions use a transparent heuristic policy. Financial conditions identify possible coverage gaps, while performance conditions determine whether changing the team is justified. The exact numerical thresholds are initial prototype values rather than universal standards or values claimed to be directly prescribed by the literature. The complete threshold rationale is documented in `THRESHOLD_SELECTION.md`.

## 8. Limitations

- Demo Mode uses deterministic rules rather than an external LLM.
- No live market/news API is used.
- Workload, contribution and redundancy values are transparent proxies.
- History is session-based and stored in memory; it is not a production database.
- Execution time depends on the local machine and should be interpreted as a demonstration metric rather than a benchmark.

## 9. Extension path

A future version could replace the deterministic signal parser with real financial documents, a database, live data sources and an LLM-based planner while retaining the same performance-monitoring and lifecycle architecture.

# FinAgent — Threshold Selection and Rule Design

## Purpose

FinAgent uses explicit heuristic thresholds so that the adaptive lifecycle decision is transparent and reproducible in Demo Mode.

The thresholds are **prototype operating values**, not financial-industry standards and not values learned from a historical dataset. The literature and project design motivate the use of team-level performance signals; the exact cut-offs below operationalize those ideas for a small, deterministic semester-project prototype.

## How the rules were designed

The rule design follows this sequence:

1. Identify a financial or external condition that may create a capability gap.
2. Identify performance signals that indicate whether the current team is healthy enough to change.
3. Use stronger overlap/low-value conditions for MERGE and REMOVE so that an agent is not removed for a single weak signal.
4. Use CONTINUE when no rule provides a clear benefit.
5. Keep the thresholds centralized in `PerformanceThresholds.java` so they are auditable and easy to calibrate in future experiments.

## Threshold policy

| Value | Current use | Design justification |
|---|---|---|
| LOW=1, MEDIUM=2, HIGH=3 | Workload units | Simple ordinal normalization of workload levels. |
| Workload <= 8 | CREATE health gate | 8 is the workload of the four-agent baseline team in the prototype. |
| Success >= 80% | CREATE health gate | Minimum reliability guardrail before adding another specialist. |
| Contribution >= 50 | CREATE health gate | Midpoint of the 0–100 prototype contribution scale; represents acceptable average usefulness. |
| Contribution <= 35 | REMOVE | Low-value region used with other removal signals. |
| Redundancy <= 60 | CREATE health gate | General acceptable-overlap boundary. |
| Redundancy >= 70 | MERGE / REMOVE | Strong-overlap trigger; deliberately stricter than the general health boundary. |
| Efficiency >= 25 | CREATE health gate | Minimum acceptable efficiency proxy before adding workload. |
| Efficiency <= 35 | REMOVE | Weak-efficiency condition used together with low contribution and high redundancy. |
| Current Ratio < 1.0 | CREATE Liquidity | Prototype trigger for a liquidity coverage gap. |
| Debt-to-Assets > 0.60 | CREATE Credit | Prototype trigger for a high-leverage/credit coverage gap. |
| Current Ratio >= 1.8, Debt-to-Assets < 0.35, Operating Margin >= 20 | REMOVE stability check | Prevents removal of News/Sentiment during a financially stressed case. |
| Runtime budget = max(10 ms, team size × 5 ms) | CREATE health gate | Simple local runtime guardrail; not a production latency benchmark. |
| Combined time >= 2 ms | MERGE | Ensures the merge decision has a measurable runtime signal in Demo Mode. |
| Combined efficiency < 35 OR average redundancy >= 40 | MERGE | Requires evidence that consolidation is useful because of efficiency or team-level overlap. |

## What is research-derived and what is not

Research motivates the architecture and evaluation dimensions: specialized agents, multi-agent coordination, and the importance of system-level performance/cost considerations. The exact numerical thresholds in this prototype are **not claimed to be directly prescribed by the literature**.

This distinction is important for academic reporting. The project contribution is the implementation of a performance-aware adaptive orchestration workflow for financial risk analysis and its comparison with a fixed-team baseline. The thresholds are the initial policy used to make that workflow executable and explainable.

## Future calibration

A stronger experimental version can test threshold sensitivity, for example:

- redundancy: 60 / 70 / 80
- contribution: 40 / 50 / 60
- success: 70% / 80% / 90%
- efficiency: 20 / 25 / 30

The selected policy can then be based on measured trade-offs among risk-analysis quality, workload, redundancy, efficiency, execution time and adaptation frequency.

## Viva answer

> “We did not claim that the exact thresholds came directly from a paper. The literature motivated us to monitor team-level performance and coordination, while the financial domain motivated specialist coverage rules. We converted those ideas into transparent heuristic thresholds for our prototype. The thresholds are centralized, reproducible and intended to be calibrated through future experiments.”

## Implemented threshold sensitivity experiment

The application exposes `GET /api/experiments/threshold-sensitivity` and a matching UI panel. It runs a one-factor-at-a-time experiment using the same four deterministic demo scenarios while varying these centralized thresholds:

- Maximum average redundancy: 60, 70, 80
- Minimum average contribution: 40, 50, 60
- Minimum success rate: 70%, 80%, 90%
- Minimum average efficiency: 20, 25, 30

The response reports the adaptation-decision counts and average final team size/workload for each setting. The default policy is restored in a `finally` block after the experiment. Results are exploratory sensitivity measurements for this prototype, not evidence that any one threshold is universally optimal. Run experiments sequentially; threshold overrides are process-wide while an experiment is running.

## Implemented multi-company SEC validation

The application exposes `GET /api/experiments/sec-validation?limit=20` (supported range 1–50) and a matching UI panel. It runs Adaptive analysis on a slice of companies in the loaded SEC dataset and reports each company's CIK, filing date, decision, risk category, final team size/workload, plus aggregate decision/risk counts and availability of liquidity, debt and margin indicators. The service does not add live market or news data. This validates pipeline execution and behavior across real SEC-derived profiles; it does not claim predictive accuracy without labeled ground truth.

## Sensitivity experiment interpretation (updated)

The experiment uses four deterministic synthetic scenarios with liquidity, debt, and margin data-availability flags explicitly enabled. The sweep ranges are intentionally broad exploratory settings: redundancy 20/50/80, contribution 20/50/80, success rate 50/80/100 percent, and efficiency 10/30/60. Each setting changes one threshold at a time, the other thresholds are restored to their baseline values, and defaults are restored in a `finally` block. Results include observed baseline contribution, redundancy, efficiency, and success rate so that unchanged decisions can be interpreted rather than assumed to indicate sensitivity.

These values are not estimated from the SEC dataset and are not industry-standard cut-offs. If observed runtime metrics do not cross a tested threshold, identical decisions are expected and should be reported as such. In particular, a success-rate sweep may not change decisions while every synthetic agent succeeds; it must not be described as evidence of a success-rate effect unless observed success rates cross the tested values.

## SEC fact selection (updated)

Company profiles use annual/quarterly periodic filings (10-K, 10-Q, 20-F, and 40-F), rather than selecting event filings such as 8-K as the latest financial profile. Facts are selected per accession and tag, prefer the filing's reporting period and consolidated facts, and select instant facts for balance-sheet tags. Duration facts prefer a full-year duration for annual filings and a single-quarter duration for quarterly filings, with year-to-date duration used only as a fallback. Ratios with missing/invalid denominators remain zero-valued for compatibility with the current model, but availability flags must be used to distinguish missing facts from verified zeros.

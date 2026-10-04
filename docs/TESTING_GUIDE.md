# Testing Guide

## Automated tests

Run:

```cmd
mvn clean test
```

The tests cover the core evolution decisions, performance gates, and Spring application context. They also verify that changing runtime performance signals can change the lifecycle decision.

## Manual test matrix

| Scenario | Expected decision | Expected team change |
|---|---|---|
| Current Ratio = 0.8 | CREATE | Liquidity Risk Agent added |
| Debt-to-Assets = 0.70 | CREATE | Credit Risk Agent added |
| News contains regulatory | CREATE | Regulatory Risk Agent added |
| Strong ratios + stable signals | REMOVE | News/Sentiment Agent removed |
| Stable market + positive news | MERGE | Market + News replaced by External Conditions |
| Uncertain/mixed signals | CONTINUE | No change |
| Low redundancy | CONTINUE | No merge |
| Useful news contribution | CONTINUE | No removal |
| High existing workload | CONTINUE | No new specialist |

## API smoke tests

### Health

`GET /api/health`

Expected response contains `status: UP`.

### Analysis

`POST /api/analyze` with a valid JSON request from the web application.

Expected response contains:
- overall risk;
- decision;
- active agents;
- baseline performance;
- final performance;
- adaptation history.

### History

`GET /api/history` returns recent session analyses.

`DELETE /api/history` clears the session history.

## Performance-awareness checks

The evolution engine uses baseline runtime signals before applying a lifecycle change:
- success rate
- average contribution
- average redundancy
- average efficiency
- workload units
- execution time

The CREATE decision requires a healthy runtime state in addition to the financial coverage trigger. MERGE requires high overlap plus a runtime-efficiency benefit. REMOVE requires low contribution, high redundancy, and low efficiency for the candidate agent.

The analysis response also exposes **Before Adaptation** and **After Adaptation** performance values so the effect of the lifecycle decision can be inspected directly.

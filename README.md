# FinAgent — Performance-Aware Adaptive Financial Risk Analysis

FinAgent is a semester-level MCA application that demonstrates a **domain-specific adaptive multi-agent system for financial risk analysis**.

The application compares two modes:

- **Standard Analysis:** fixed four-agent baseline.
- **Adaptive Analysis:** evaluates the baseline, monitors performance signals, applies an explicit lifecycle decision (**CREATE / MERGE / REMOVE / CONTINUE**), executes the resulting team again, and reports the before/after comparison.

## Data input

FinAgent supports a real **SEC Financial Statement Data Set** upload. Unzip the quarterly SEC package first, then use the browser folder picker to upload the unzipped folder. The application streams the large `num.txt` file and builds a company-level financial index from `num.txt` and `sub.txt`. The raw dataset is not bundled in the project ZIP. See `docs/DATASET_GUIDE.md`.

## Architecture

```text
SEC Dataset Folder
   |
   v
Dataset Upload + Streaming Processor
   |
   v
Company Financial Profile
   |
   v
REST Controller
   |
   v
Analysis Service
   |----------------------|
   v                      v
Fixed Agent Team     Performance Monitor
   |                      |
   |              contribution / redundancy /
   |              workload / success / time
   |                      |
   |----------------------v
   |               Evolution Decision Engine
   |                 /   |   |   \
   |              CREATE MERGE REMOVE CONTINUE
   |                    |
   v                    v
Final Agent Team --> Risk Assessment --> Report
                         |
                         v
                 Session History / Export
```

## Agents

### Core team
1. Financial Statement Agent
2. Market Agent
3. News/Sentiment Agent
4. Risk Assessment Agent

### Optional specialists
5. Liquidity Risk Agent
6. Credit Risk Agent
7. Regulatory Risk Agent
8. External Conditions Agent

## How adaptation works

1. The same four-agent team is used as the baseline.
2. Each agent reports a result with execution time, workload level, success, contribution, redundancy and efficiency scores.
3. `PerformanceMonitor` aggregates these into a baseline snapshot.
4. `EvolutionDecisionService` evaluates financial triggers together with performance evidence.
5. The team is changed when appropriate.
6. The final team executes again.
7. The UI compares baseline and final team size, workload, success, contribution, redundancy and efficiency.

### Decision rules

- **CREATE Liquidity Risk Agent:** Current Ratio < 1.0 **and** the baseline runtime team is successful, efficient and within the workload/time budget.
- **CREATE Credit Risk Agent:** Debt-to-Assets > 0.60 **and** the same runtime performance gate passes.
- **CREATE Regulatory Risk Agent:** News Signal contains `regulatory` **and** the runtime performance gate passes.
- **REMOVE News/Sentiment Agent:** low contribution + high redundancy + low efficiency, with strong financial health.
- **MERGE Market + News/Sentiment:** both agents succeed, redundancy is high, and one merged agent has lower workload/time cost.
- **CONTINUE:** no adaptation provides a clear runtime-performance benefit.

The decision engine therefore uses two signal groups: financial/risk conditions identify the possible need for adaptation, while runtime performance determines whether the change is worthwhile.

## Performance model

This project intentionally uses a transparent student-level performance model:

- execution time = measured locally with Java `System.nanoTime()`
- workload = LOW 1, MEDIUM 2, HIGH 3 units
- contribution = deterministic 0–100 usefulness proxy in Demo Mode
- redundancy = deterministic 0–100 overlap proxy in Demo Mode
- efficiency = deterministic 0–100 workload-efficiency proxy in Demo Mode
- success rate = successful agent executions / total executions

Workload, contribution, redundancy and efficiency are **proxies**, not claims of real CPU, token or cloud-cost measurement.

## Threshold policy

The adaptive rules use explicit heuristic thresholds. They are centralized in `PerformanceThresholds.java` and documented in `docs/THRESHOLD_SELECTION.md`. These values are initial prototype operating points, not universal financial standards or research-derived constants. The project distinguishes research motivation (performance-aware coordination) from the exact numerical policy used by the Demo Mode.

## Application features

- Standard vs Adaptive comparison
- Explainable decision rule and performance evidence
- Before/after performance comparison
- Adaptation log
- Recent session history
- Four-scenario evaluation dashboard covering CREATE, MERGE, REMOVE and CONTINUE
- JSON report export
- Printable report
- Input validation
- Health endpoint: `/api/health`
- Unit tests for evolution rules and performance monitoring
- Runs locally without an external LLM/API key

## Run on Windows

Prerequisites:
- Java 17+ (the project source level is Java 17)
- Maven

```cmd
mvn clean test
mvn spring-boot:run
```

Then open `http://localhost:8080`.

## Suggested viva demonstration

1. Run **Standard Analysis** with the default input.
2. Run **Adaptive Analysis** with the default input.
3. Change the company inputs to test different financial conditions and run Adaptive Analysis again.
4. Observe that FinAgent automatically selects CREATE, MERGE, REMOVE or CONTINUE.
5. Explain the baseline → monitor → decision → final team flow.
6. Show the before/after performance metrics and adaptation log.
7. Export the JSON report.

## Evaluation dashboard

The evaluation service can run four reproducible internal scenarios repeatedly and compare Standard vs Adaptive workload and team size. These scenarios are part of the evaluation backend, not manual controls in the analysis UI. This provides a small, reproducible experiment for the semester project rather than relying only on screenshots.

## Important scope statement

FinAgent is a **rule-based adaptive orchestration prototype in Demo Mode**. It is not presented as a fully autonomous self-evolving LLM system and does not ingest live financial data. Its semester-project contribution is the implementation and evaluation of an interpretable adaptive team-management workflow for financial risk analysis.


## Performance-aware decision logic

Adaptive decisions are made from both financial/external conditions and baseline runtime signals. CREATE requires a coverage trigger plus healthy success, contribution, efficiency, redundancy, workload, and execution-time conditions. MERGE requires high overlap and a measurable workload/runtime benefit. REMOVE requires low contribution, high redundancy, and low efficiency for the candidate agent. CONTINUE is used when no change is justified.

Each adaptive run records performance **before adaptation** and **after adaptation**, including team size, workload, contribution, redundancy, efficiency, success, and execution time.

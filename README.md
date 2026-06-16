# DPQL Execution Engine

A holistic constraint-driven dependency discovery engine for the **Data Profiling Query Language (DPQL)**.

This project implements the missing *Constraint-Driven Dependency Discovery* component of the DPQL engine, as proposed in [Seeger et al. (2023)](https://hpi.de/oldsite/fileadmin/user_upload/fachgebiete/naumann/publications/PDFs/2023_seeger_dpql.pdf). Instead of running profiling algorithms independently and matching results afterward, this engine coordinates the discovery of Unique Column Combinations (UCCs), Functional Dependencies (FDs), and Inclusion Dependencies (INDs) simultaneously, guided by minimality constraints derived from the query pattern.

---

## Overview

![System Architecture](docs/images/architecture.png)

The engine takes a DPQL query pattern and a set of relation files as input. It derives a minimality constraint for each primitive dependency in the query, constructs a level-wise execution graph, and profiles each dependency using a restricted search space derived from upstream results.

**Supported dependency types:**
- Functional Dependencies (FDs) — using a HyFD-inspired profiler
- Unique Column Combinations (UCCs) — using a HyUCC-inspired profiler
- Inclusion Dependencies (INDs) — using a DeMarchi-inspired profiler

**Supported profiling modes per dependency:**

| Dependency | Modes |
|---|---|
| FD | Free/Free, Free/Lock, Lock/Free, Lock/Lock |
| UCC | Free, Lock |
| IND | Free/Free, Free/Lock, Lock/Free, Lock/Lock |

---

## System Components

### Instructor
The central orchestrator. Receives the constraint map and relation map from the query parser, constructs the execution graph, schedules all execution nodes, and collects results.

### Profiling Context
A shared resource provider for all profiling components. Maintains lazily initialized resources including `Relation` objects, `Sampler`, `FDValidator`, `UCCValidator`, and `INDUnaryCover`. All resources are protected with double-checked locking for thread safety.

### Execution Graph
A directed graph of `ExecutionNode` objects built level-wise. Each node encapsulates a single dependency at a specific LHS size level. Nodes wait for parent nodes to complete before starting, and pass results downstream as locked search spaces.

![Execution Graph](docs/images/execution_graph.png)

### Profilers
Three profilers handle the actual dependency discovery:
- `FDProfiler` — extends HyFD with a validated bitset and concurrent read/write locking on the FD positive cover tree
- `UCCProfiler` — extends HyUCC similarly for UCC discovery
- `INDProfiler` — uses DeMarchi-style inverted index intersection for unary IND discovery and in-memory tuple validation for n-ary INDs

### Execution Timeline

![Execution Timeline](docs/images/execution_timeline.png)

---

## Key Design Decisions

- **Shared Sampler**: A single `Sampler` instance is shared between the FD and UCC profilers, avoiding redundant record pair comparisons and sharing the negative cover (agree-sets).
- **Extended Positive Cover**: The FD and UCC positive cover trees are extended with a `validated` bitset per node, enabling efficient candidate inference for locked search spaces without redundant re-validation.
- **Lazy Loading**: Relation data is loaded on demand. Only the schema (column count) is read at initialization; actual records are loaded when first requested by a profiler.
- **Level-wise Pipeline**: Results at level `ℓ` are passed downstream while the anchor profiler continues to level `ℓ+1`, enabling pipeline parallelism across dependency stages.

---

## Requirements

- Java JDK 1.8 or later
- Maven 3.1.0 or later
- Git

**Dependencies:**
- [org.reflections](https://github.com/ronmamo/reflections)
- [antlr](https://github.com/antlr/antlr4) — for DPQL query parsing
- [akka](https://github.com/akka/akka)
- [junit](https://github.com/junit-team/junit4) — for evaluation test suites
- [logback](https://github.com/qos-ch/logback) — for logging

---

## Usage

1. Place your CSV dataset files in the `data/` folder.
2. Write a DPQL query following the [DPQL guidelines](https://github.com/SeegerM/DPQL/wiki).
3. Run the engine and read results in the console.

Example query:
```sql
SELECT X AS ForeignKey, Y AS Key
FROM CC(*) X, CC(*) Y
WHERE IND(X,Y) AND UCC(Y)
```

---

## Evaluation

The engine was evaluated against 38 DPQL query patterns across 5 datasets:

| Dataset | Size | Relations |
|---|---|---|
| WDC (Astrology) | 0.02 MB | 11 |
| TPC-H Small | 11.2 MB | 8 |
| TPC-H Large | 425 MB | 7 |
| Sakila | 2.9 MB | 15 |
| AdventureWorks | 90.4 MB | 67 |

The holistic engine consistently outperforms the baseline (independent execution + post-processing matching) in both result completeness and execution time.

---

## Executor Modes

The engine supports two executor modes selectable via a parameter:

- `BASELINE` — runs HyFD, HyUCC, and BINDER independently over the entire dataset, then matches results to the query pattern in post-processing. This replicates the current DPQL implementation.
- `HOLISTIC` — runs the constraint-driven execution engine proposed in this work.

Both modes reside in the same codebase and can be switched without changing any profiling logic.

---

## Limitations

- Requires at least one individually minimal primitive dependency in the query pattern as an anchor node. Queries without such a dependency (e.g. all constraints in `F+`, `U+`, or `I+`) produce incomplete results.
- No persistent state between sessions. All in-memory structures are rebuilt from scratch on each engine start.
- Tested on datasets up to approximately 500 MB. Larger datasets may cause out-of-memory errors due to the absence of systematic memory management.

---

## Related Work

This engine builds on the following algorithms and frameworks:
- [HyFD](https://hpi.de/oldsite/fileadmin/user_upload/fachgebiete/naumann/publications/PDFs/2016_papenbrock_a.pdf) — Hybrid Functional Dependency Discovery
- [HyUCC](https://hpi.de/fileadmin/user_upload/fachgebiete/naumann/publications/2017/paper.pdf) — Hybrid Unique Column Combination Discovery
- [DeMarchi et al.](https://www.researchgate.net/publication/225160065_Efficient_Algorithms_for_Mining_Inclusion_Dependencies) — Efficient Algorithms for Mining Inclusion Dependencies
- [DPQL](https://hpi.de/oldsite/fileadmin/user_upload/fachgebiete/naumann/publications/PDFs/2023_seeger_dpql.pdf) — Data Profiling Query Language

---

## Supervisor

Developed under the supervision of **Prof. Dr. Thorsten Papenbrock** and **Marcian Seeger** at the Big Data Analytics Research Group, Philipps-Universität Marburg.
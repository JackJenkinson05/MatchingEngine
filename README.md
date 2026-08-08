# Matching Engine

A limit-order-book matching engine written in core Java, built as a study project for
low-latency Java engineering. The goal is not just to make it *work*, but to make it
*fast* — and to measure every step of that journey.

The plan is to start from a deliberately simple, idiomatic implementation and then
iterate: change one thing at a time, re-run the benchmark, and record the latency
improvement (or regression). The README, benchmark numbers, and commit history together
tell the story of how a naive engine is turned into a low-latency one.

## What it does

The engine maintains a two-sided limit order book and matches incoming orders against
resting liquidity following standard **price–time priority**:

- **Price priority** — the best-priced resting order trades first (highest bid / lowest ask).
- **Time priority** — at equal price, the earliest resting order trades first.
- Incoming orders execute at the **resting order's price** (the passive side sets the price).
- An order that cannot be fully filled **rests** on the book for its remaining quantity.
- A single aggressive order can **sweep** multiple resting orders across price levels.

`MatchingEngine.submitOrder(Order)` is the single entry point. It assigns a sequence
timestamp, places the order on the book, runs matching, and returns the list of `Trade`s
generated.

## Design (current baseline)

The starting implementation favours clarity over speed — it's the baseline every later
optimisation is measured against.

| Concern | Data structure |
|---|---|
| Bids (buy side) | `TreeMap<Integer, LinkedList<Order>>`, descending by price |
| Asks (sell side) | `TreeMap<Integer, LinkedList<Order>>`, ascending by price |
| Order lookup by id | `HashMap<Long, Order>` |

Each price level holds a `LinkedList<Order>` in arrival order, giving FIFO time priority.
The `TreeMap` keeps price levels sorted so the best bid/ask is always `firstEntry()`.

### Domain model

- **`Order`** — id, side, price, quantity, timestamp (Lombok-generated getters/setters).
- **`Side`** — `BUY` / `SELL`.
- **`Trade`** — an immutable `record` of `(buyOrderId, sellOrderId, price, quantity)`.
- **`MatchingEngine`** — the order book and matching logic.

Prices and quantities are modelled as `int` to keep the baseline free of floating-point
and `BigDecimal` overhead.

## Project layout

```
src/
├── main/java/com/jackjenkinson/
│   ├── MatchingEngine.java     # order book + matching logic
│   ├── Order.java
│   ├── Side.java
│   └── Trade.java
└── test/java/com/jackjenkinson/
    ├── MatchingEngineTest.java                      # JUnit 5 correctness tests
    └── performanceTests/
        └── SubmitOrderBenchmark.java                # JMH latency benchmark
```

## Requirements

- **JDK 21** (the build targets Java 21)
- **Maven 3.9+**

## Building

```bash
mvn clean compile
```

## Running the correctness tests

The JUnit 5 suite covers resting orders, exact and partial matches, multi-level sweeps,
and price/time priority.

```bash
mvn test
```

## Running the latency benchmark

Latency is measured with [JMH](https://github.com/openjdk/jmh). The benchmark
(`SubmitOrderBenchmark`) pre-loads the book with resting orders, then measures the latency
of a single crossing `submitOrder` call. It runs in `SampleTime` mode so the output is a
distribution of per-call latencies (mean plus p50/p90/p99/p99.9 percentiles) in
nanoseconds, across 3 forked JVMs with warmup iterations to let the JIT settle.

The benchmark lives in the test sources. The simplest way to run it:

**From IntelliJ IDEA** — open `SubmitOrderBenchmark` and run it with the JMH plugin
(the gutter run icon on the class / `@Benchmark` method).

**From the command line** — compile the test sources, then invoke the JMH runner on the
test classpath:

```bash
mvn test-compile
mvn exec:java -Dexec.classpathScope=test -Dexec.mainClass=org.openjdk.jmh.Main
```

> Note: the `exec:java` route needs the `exec-maven-plugin`. If it isn't configured yet,
> either add it to `pom.xml` or run the benchmark from the IDE. A common alternative is to
> add the JMH `maven-shade-plugin` setup to build a runnable `benchmarks.jar`
> (`java -jar target/benchmarks.jar`) — this will likely be added as the benchmarking
> workflow matures.

## Roadmap

This project is iterative. Each step changes the implementation and records the resulting
latency against the baseline:

- [x] Baseline: `TreeMap` + `LinkedList` order book, JMH benchmark in place
- [ ] Record baseline latency numbers here
- [ ] Iterate on data structures and allocation, measuring the delta at each step

### Latency log

| Step | Change | p50 (ns) | p99 (ns) | Notes |
|---|---|---|---|---|
| Baseline | `TreeMap` + `LinkedList` | _tbd_ | _tbd_ | |

_(Fill in as benchmarks are recorded.)_

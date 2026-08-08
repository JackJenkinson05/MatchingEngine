# Matching Engine

A limit-order-book matching engine written in core Java, built as a study project for
low-latency Java engineering. The goal is not just to make it *work*, but to make it
*fast* — and to measure every step of that journey.

The plan is to start from a deliberately simple implementation and then iterate: change
one thing at a time, re-run the benchmark, and record the latency improvement (or
regression).

Rather than mutating a single class and hunting through git history for old approaches,
each iteration is kept as its own **numbered solution**. `MatchingEngine` is an interface —
the behavioural contract — and every attempt (`MatchingEngineSolution1`,
`MatchingEngineSolution2`, …) is a separate implementation of it. All solutions live side
by side, are held to the *same* set of contract tests, and can be benchmarked
interchangeably. The README, benchmark numbers, and the growing list of solutions together
tell the story of how a naive engine is turned into a low-latency one.

## What it does

The engine maintains a two-sided limit order book and matches incoming orders against
resting liquidity following standard **price–time priority**:

- **Price priority** — the best-priced resting order trades first (highest bid / lowest ask).
- **Time priority** — at equal price, the earliest resting order trades first.
- Incoming orders execute at the **resting order's price** (the passive side sets the price).
- An order that cannot be fully filled **rests** on the book for its remaining quantity.
- A single aggressive order can **sweep** multiple resting orders across price levels.

`MatchingEngine.submitOrder(Order)` is the single entry point of the contract. Each
solution assigns a sequence timestamp, places the order on the book, runs matching, and
returns the list of `Trade`s generated.

## Requirements

- **JDK 21** (the build targets Java 21)
- **Maven 3.9+**

## Running the latency benchmark

Latency is measured with [JMH](https://github.com/openjdk/jmh). The benchmark
(`SubmitOrderBenchmark`) pre-loads the book with resting orders, then measures the latency
of a single crossing `submitOrder` call. It runs in `SampleTime` mode so the output is a
distribution of per-call latencies (mean plus p50/p90/p99/p99.9 percentiles) in
nanoseconds, across 3 forked JVMs with warmup iterations to let the JIT settle.

**Choosing which solution to benchmark** — the engine is selected by a JMH `@Param` over
the `EngineSolution` registry:


```bash
mvn test-compile
mvn exec:java -Dexec.classpathScope=test -Dexec.mainClass=org.openjdk.jmh.Main
```

> Note: the `exec:java` route needs the `exec-maven-plugin`. If it isn't configured yet,
> either add it to `pom.xml` or run the benchmark from the IDE. A common alternative is to
> add the JMH `maven-shade-plugin` setup to build a runnable `benchmarks.jar`
> (`java -jar target/benchmarks.jar`) — this will likely be added as the benchmarking
> workflow matures.



### Solution 1 — the baseline
#### Implementation
The first implementation favours clarity over speed — it's the baseline every later
solution is measured against.

| Concern | Data structure |
|---|---|
| Bids (buy side) | `TreeMap<Integer, LinkedList<Order>>`, descending by price |
| Asks (sell side) | `TreeMap<Integer, LinkedList<Order>>`, ascending by price |
| Order lookup by id | `HashMap<Long, Order>` |

Each price level holds a `LinkedList<Order>` in arrival order, giving FIFO time priority.
The `TreeMap` keeps price levels sorted so the best bid/ask is always `firstEntry()`.

#### Latency results
Latency of a single crossing `submitOrder` call, JMH `SampleTime` mode, 888,579 samples:

```
Benchmark                                          Mode     Cnt      Score      Error  Units
submitCrossingOrder                              sample  888579    420.956 ± 500.946  ns/op
submitCrossingOrder:p0.00                        sample                ≈ 0            ns/op
submitCrossingOrder:p0.50                        sample             42.000            ns/op
submitCrossingOrder:p0.90                        sample             84.000            ns/op
submitCrossingOrder:p0.95                        sample            208.000            ns/op
submitCrossingOrder:p0.99                        sample            250.000            ns/op
submitCrossingOrder:p0.999                       sample           3916.000            ns/op
submitCrossingOrder:p0.9999                      sample          68918.528            ns/op
submitCrossingOrder:p1.00                        sample       95944704.000            ns/op
```

Note that while our P99 = 250ns (we can do better) our slowest result is 95944704ns over 300,000 times slower than our p99.
This shows that the variance of our results is very high, the likely causes of this are Garbage Collections or JIT compilation.

Our current approach creates multiple objects per method call and this will result in more frequent GC's than necessary.

#### Next Step
Reduce per method call object allocation to reduce GC pause times.






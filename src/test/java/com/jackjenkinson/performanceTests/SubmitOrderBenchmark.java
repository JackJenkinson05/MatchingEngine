package com.jackjenkinson.performanceTests;

// JMH annotations — all from org.openjdk.jmh.annotations
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.jackjenkinson.MatchingEngine;
import com.jackjenkinson.MatchingEngineSolutions.EngineSolution;
import com.jackjenkinson.Order;
import com.jackjenkinson.Side;
import com.jackjenkinson.Trade;

@BenchmarkMode(Mode.SampleTime)          // records per-call latency samples → percentiles
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)        // let the JIT warm up before measuring
@Measurement(iterations = 10, time = 1)
@Fork(3)                                 // fresh JVM per fork → isolates JIT/GC luck
@State(Scope.Thread)
public class SubmitOrderBenchmark {

    // Which solution(s) to benchmark. List names to compare several; remove the values
    // entirely to run every registered solution. Overridable on the CLI: -p solution=SOLUTION_1
    @Param({"SOLUTION_1"})
    public EngineSolution solution;

    private MatchingEngine engine;
    private long nextId;

    @Setup(Level.Iteration)
    public void setup() {
        engine = solution.create();
        for (int p = 90; p <= 110; p++) {
            engine.submitOrder(new Order(nextId++, Side.BUY,  p, 100));
            engine.submitOrder(new Order(nextId++, Side.SELL, p + 20, 100));
        }
    }

    @Benchmark
    public List<Trade> submitCrossingOrder() {
        return engine.submitOrder(new Order(nextId++, Side.SELL, 95, 10));
    }
}

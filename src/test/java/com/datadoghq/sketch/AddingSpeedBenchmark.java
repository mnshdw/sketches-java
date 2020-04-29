/* Unless explicitly stated otherwise all files in this repository are licensed under the Apache License 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2019 Datadog, Inc.
 */

package com.datadoghq.sketch;

import com.datadoghq.sketch.ddsketch.DDSketch;
import com.datadoghq.sketch.ddsketch.DDSketchWithNegativeNumbers;
import com.datadoghq.sketch.ddsketch.mapping.IndexMapping;
import com.datadoghq.sketch.ddsketch.mapping.LogarithmicMapping;
import com.datadoghq.sketch.ddsketch.store.CollapsingLowestDenseStore;
import com.datadoghq.sketch.ddsketch.store.Store;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;
import org.apache.commons.math3.distribution.ParetoDistribution;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 1, time = 5)
@Measurement(iterations = 3, time = 15)
@Fork(1)
public class AddingSpeedBenchmark {

    public static void main(String... args) throws Exception {
        // configure and run benchmark
        Options opt = new OptionsBuilder()
                .include(AddingSpeedBenchmark.class.getSimpleName() + ".*")
                .shouldFailOnError(true)
                .shouldDoGC(true)
                .build();
        new Runner(opt).run();
    }

    @State(Scope.Thread)
    public static class BenchmarkState {

        @Param({ "DDSketch", "DDSketchWithNegativeNumbers" })
        String sketchImpl;

        @Param({ "pareto" })
        String datasetImpl;

        QuantileSketch<?> sketch;
        Dataset dataset;
        double[] values;

        @Setup
        public void setup() throws Exception {
            IndexMapping mapping = new LogarithmicMapping(1e-2);
            Supplier<Store> store = () -> new CollapsingLowestDenseStore(2048);
            switch (sketchImpl) {
                case "DDSketch":
                    sketch = new DDSketch(mapping, store);
                    break;
                case "DDSketchWithNegativeNumbers":
                    sketch = new DDSketchWithNegativeNumbers(mapping, store);
                    break;
                default:
                    throw new IllegalStateException("Unsupported sketch:" + sketchImpl);
            }
            switch (datasetImpl) {
                case "pareto":
                    dataset = new SyntheticDataset(
                            new DoubleSupplier() {
                                private final ParetoDistribution paretoDistribution = new ParetoDistribution();

                                @Override
                                public double getAsDouble() {
                                    return paretoDistribution.sample();
                                }
                            });
                    break;
                default:
                    throw new IllegalStateException("Unsupported dataset:" + datasetImpl);
            }
            values = dataset.get(100_000_000);
        }

        @TearDown
        public void tearDown() throws Exception {
        }

    }

    @Benchmark
    public void benchmark(BenchmarkState state, Blackhole bh) throws Exception {
        for (double value : state.values) {
            state.sketch.accept(value);
        }
    }

}

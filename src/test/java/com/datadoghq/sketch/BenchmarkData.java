package com.datadoghq.sketch;

import java.util.Map;
import java.util.function.DoubleSupplier;
import org.apache.commons.math3.distribution.ParetoDistribution;

final class BenchmarkData {

    static final Map<String, Dataset> DATASETS = Map.of(

            "pareto",
            new SyntheticDataset(
                    new DoubleSupplier() {
                        private final ParetoDistribution paretoDistribution = new ParetoDistribution();

                        @Override
                        public double getAsDouble() {
                            return paretoDistribution.sample();
                        }
                    })

    );
}
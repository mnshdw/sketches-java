/* Unless explicitly stated otherwise all files in this repository are licensed under the Apache License 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2019 Datadog, Inc.
 */

package com.datadoghq.sketch.ddsketch.mapping;

import com.datadoghq.sketch.util.accuracy.RelativeAccuracyTester;
import java.util.stream.DoubleStream;
import org.junit.jupiter.api.Test;

abstract class IndexMappingTest {

    private final double minTestedRelativeAccuracy = 1e-8;
    private final double maxTestedRelativeAccuracy = 1 - 1e-3;
    private final double multiplier = 1 + Math.sqrt(2) * 1e-1;

    abstract IndexMapping getMapping(double relativeAccuracy);

    @Test
    void testAccuracy() {
        getRelativeAccuraciesToTest().forEach(relativeAccuracy -> {

            final IndexMapping mapping = getMapping(relativeAccuracy);

            // Assert that the stated relative accuracy of the mapping is less than or equal to the requested one.
            RelativeAccuracyTester.assertAccurate(relativeAccuracy, mapping.relativeAccuracy());

            final double maxRelativeAccuracy = assertRelativelyAccurate(mapping);

            // Handy to check that the actual accuracy is consistent with the claimed one (i.e., not much lower).
            /*
            System.out.println(String.format(
                    "Relative accuracy - Requested: %g, claimed: %g, actual: %g",
                    relativeAccuracy,
                    mapping.relativeAccuracy(),
                    maxRelativeAccuracy
            ));
             */
        });
    }

    private DoubleStream getRelativeAccuraciesToTest() {
        return DoubleStream.iterate(
            maxTestedRelativeAccuracy,
            relativeAccuracy -> relativeAccuracy >= minTestedRelativeAccuracy,
            relativeAccuracy -> relativeAccuracy * maxTestedRelativeAccuracy
        );
    }

    private static double assertRelativelyAccurate(IndexMapping mapping, double value) {

        final double relativeAccuracy = RelativeAccuracyTester.compute(
            value,
            mapping.value(mapping.index(value))
        );

        RelativeAccuracyTester.assertAccurate(mapping.relativeAccuracy(), relativeAccuracy);
        return relativeAccuracy;
    }

    private double assertRelativelyAccurate(IndexMapping mapping) {

        double maxRelativeAccuracy = 0;
        for (
            double value = mapping.minIndexableValue();
            value < mapping.maxIndexableValue();
            value *= multiplier
        ) {
            maxRelativeAccuracy = Math.max(
                maxRelativeAccuracy,
                assertRelativelyAccurate(mapping, value)
            );
        }
        maxRelativeAccuracy = Math.max(
            maxRelativeAccuracy,
            assertRelativelyAccurate(mapping, mapping.maxIndexableValue())
        );

        return maxRelativeAccuracy;
    }
}

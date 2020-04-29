package com.datadoghq.sketch;

public interface Dataset {

    default int getMaxSize() {
        return Integer.MAX_VALUE;
    }

    double[] get(int size);
}
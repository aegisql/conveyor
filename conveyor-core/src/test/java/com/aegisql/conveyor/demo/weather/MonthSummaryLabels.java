package com.aegisql.conveyor.demo.weather;

import com.aegisql.conveyor.SmartLabel;

import java.util.function.BiConsumer;

public enum MonthSummaryLabels implements SmartLabel<MonthSummaryCollector> {
    WEATHER_RECORD(MonthSummaryCollector::weatherRecord),
    ;
    private final BiConsumer<MonthSummaryCollector, Object> consumer;

    <T> MonthSummaryLabels(BiConsumer<MonthSummaryCollector, T> consumer) {
        this.consumer = (BiConsumer<MonthSummaryCollector, Object>) consumer;
    }

    @Override
    public BiConsumer<MonthSummaryCollector, Object> get() {
        return consumer;
    }

}

package com.aegisql.conveyor.demo.weather;

import com.aegisql.conveyor.Testing;
import com.aegisql.conveyor.TimeoutAction;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class MonthSummaryCollector implements Supplier<MonthSummary>, Testing, TimeoutAction {

    private String name;

    private Map<Date,WeatherRecord> records = new HashMap<>();

    private int month = -1;
    private int maxDays = -1;
    private boolean ready = false;
    private int year;

    @Override
    public MonthSummary get() {
        try {
            MonthSummary monthSummary = new MonthSummary(name
                    , year
                    , toMonth(month)
                    ,month
                    , sumPrecipitation()
                    , lowTemperatureMin()
                    , lowTemperatureMax()
                    , highTemperatureMin()
                    , highTemperatureMax()
                    , fastestWind());
            return monthSummary;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private Float fastestWind() {
        Stream<Float> stream1 = records.values().stream().map(WeatherRecord::getFastest2MinWindSpeed).filter(Objects::nonNull);
        Stream<Float> stream2 = records.values().stream().map(WeatherRecord::getFastest5SecWindSpeed).filter(Objects::nonNull);
        return Stream.concat(stream1,stream2).max(Float::compare).orElse(null);
    }

    private Integer lowTemperatureMin() {
        return records.values().stream().map(WeatherRecord::gettMin).filter(Objects::nonNull).min(Integer::compare).orElse(null);
    }

    private Integer lowTemperatureMax() {
        return records.values().stream().map(WeatherRecord::gettMin).filter(Objects::nonNull).max(Integer::compare).orElse(null);
    }

    private Integer highTemperatureMin() {
        return records.values().stream().map(WeatherRecord::gettMax).filter(Objects::nonNull).min(Integer::compare).orElse(null);
    }

    private Integer highTemperatureMax() {
        return records.values().stream().map(WeatherRecord::gettMax).filter(Objects::nonNull).max(Integer::compare).orElse(null);
    }

    private Float sumPrecipitation() {
        return records.values().stream().map(WeatherRecord::getPrecipitation).filter(Objects::nonNull).reduce(0.0f,Float::sum);
    }

    private String toMonth(int month) {
        return switch (month) {
            case 0->"JAN";
            case 1->"FEB";
            case 2->"MAR";
            case 3->"APR";
            case 4->"MAY";
            case 5->"JUN";
            case 6->"JUL";
            case 7->"AUG";
            case 8->"SEP";
            case 9->"OCT";
            case 10->"NOV";
            case 11->"DEC";
            default -> throw new RuntimeException("Unexpected month "+month);

        };
    }

    public void weatherRecord(WeatherRecord weatherRecord) {
        this.name = weatherRecord.getName();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(weatherRecord.getDate());
        this.year = calendar.get(Calendar.YEAR);
        this.month = calendar.get(Calendar.MONTH);
        this.maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        records.put(weatherRecord.getDate(),weatherRecord);
        ready = records.size() == this.maxDays;
    }



    @Override
    public boolean test() {
        return ready;
    }

    @Override
    public void onTimeout() {
        // received at least some records
        ready = records.size() > 0;
    }
}

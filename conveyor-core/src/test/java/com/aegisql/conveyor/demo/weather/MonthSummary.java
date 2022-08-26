package com.aegisql.conveyor.demo.weather;

public class MonthSummary implements Comparable<MonthSummary> {

    private String station;
    private final String name;
    private int year;
    private String month;
    private int monthNumber;
    private Float totalPrecipitation;
    private Integer lowTemperatureMin;
    private Integer lowTemperatureMax;
    private Integer highTemperatureMin;
    private Integer highTemperatureMax;
    private Float fastestWind;

    public MonthSummary(String name, int year, String month, int monthNumber, Float totalPrecipitation, Integer lowTemperatureMin, Integer lowTemperatureMax, Integer highTemperatureMin, Integer highTemperatureMax, Float fastestWind) {
        this.name = name;
        this.year = year;
        this.month = month;
        this.monthNumber = monthNumber;
        this.totalPrecipitation = totalPrecipitation;
        this.lowTemperatureMin = lowTemperatureMin;
        this.lowTemperatureMax = lowTemperatureMax;
        this.highTemperatureMin = highTemperatureMin;
        this.highTemperatureMax = highTemperatureMax;
        this.fastestWind = fastestWind;
    }

    public void setStation(String station) {
        this.station = station;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MonthSummary{");
        sb.append("station='").append(station).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", observed=").append(month);
        sb.append(" ").append(year);
        if(totalPrecipitation != null) sb.append(", totalPrecipitation=").append(totalPrecipitation);
        if(lowTemperatureMin != null) sb.append(", lowTemperatureMin=").append(lowTemperatureMin);
        if(lowTemperatureMax != null) sb.append(", lowTemperatureMax=").append(lowTemperatureMax);
        if(highTemperatureMin != null) sb.append(", highTemperatureMin=").append(highTemperatureMin);
        if(highTemperatureMax != null) sb.append(", highTemperatureMax=").append(highTemperatureMax);
        if(fastestWind != null) sb.append(", fastestWind=").append(fastestWind);
        sb.append('}').append('\n');
        return sb.toString();
    }

    @Override
    public int compareTo(MonthSummary o) {
        int res;
        res = station.compareTo(o.station);
        if(res == 0) {
            res = Integer.compare(monthNumber,o.monthNumber);
        }
        return res;
    }
}

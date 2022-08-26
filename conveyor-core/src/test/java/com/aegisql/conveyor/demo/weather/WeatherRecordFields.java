package com.aegisql.conveyor.demo.weather;

import com.aegisql.conveyor.SmartLabel;

import java.util.function.BiConsumer;

public enum WeatherRecordFields implements SmartLabel<WeatherRecordCollector> {

    NAME(WeatherRecordCollector::name),
    DAPR(WeatherRecordCollector::dayOfPrecipitation), // - Number of days included in the multiday precipitation total (MDPR)
    WSF2(WeatherRecordCollector::fastest2MinWindSpeed), // - Fastest 2-minute wind speed
    WSF5(WeatherRecordCollector::fastest5SecWindSpeed), // - Fastest 5-second wind speed
    SNOW(WeatherRecordCollector::snowfall), // - Snowfall
    WT03(WeatherRecordCollector::thunder), //- Thunder
    WESF(WeatherRecordCollector::waterEqSnowfall), // - Water equivalent of snowfall
    WT04(WeatherRecordCollector::sleet), // - Ice pellets, sleet, snow pellets, or small hail"
    PRCP(WeatherRecordCollector::precipitation), // - Precipitation
    WT05(WeatherRecordCollector::hail), // - Hail (may include small hail)
    TOBS(WeatherRecordCollector::temperatureAtObservationTime), // - Temperature at the time of observation
    WT08(WeatherRecordCollector::smoke), // - Smoke or haze
    SNWD(WeatherRecordCollector::snowDepth), // - Snow depth
    WDF2(WeatherRecordCollector::directionOfFastest2MinWind), // - Direction of fastest 2-minute wind
    AWND(WeatherRecordCollector::avgWindSpeed), // - Average wind speed
    WDF5(WeatherRecordCollector::directionOfFastest5SecWind), // - Direction of fastest 5-second wind
    PGTM(WeatherRecordCollector::peakGustTime), // - Peak gust time
    WT11(WeatherRecordCollector::highWinds), // - High or damaging winds
    WT01(WeatherRecordCollector::fog), // - Fog, ice fog, or freezing fog (may include heavy fog)
    TMAX(WeatherRecordCollector::temperatureMax), // - Maximum temperature
    TMIN(WeatherRecordCollector::temperatureMin), // - Minimum temperature
    TAVG(WeatherRecordCollector::temperatureAvg), // - Average Temperature.
    WESD(WeatherRecordCollector::waterEqSnowOnGround), // - Water equivalent of snow on the ground
    WT02(WeatherRecordCollector::heavyFog), // - Heavy fog or heaving freezing fog (not always distinguished from fog)
    MDPR(WeatherRecordCollector::multidayPrecipTotal), // - Multiday precipitation total (use with DAPR and DWPR, if available)
    DONE((b,o)->{});
    ;
    private final BiConsumer<WeatherRecordCollector, Object> consumer;

    <T> WeatherRecordFields(BiConsumer<WeatherRecordCollector, T> consumer) {
        this.consumer = (BiConsumer<WeatherRecordCollector, Object>) consumer;
    }

    @Override
    public BiConsumer<WeatherRecordCollector, Object> get() {
        return consumer;
    }

}

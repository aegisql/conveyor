package com.aegisql.conveyor.demo.weather;

import java.util.Date;

public class WeatherRecord {
    private String station;
    private String name;
    private Date date;
    private Integer dayOfPrecipitation;
    private Float fastest2MinWindSpeed;
    private Float fastest5SecWindSpeed;
    private Float snowfall;
    private Boolean thunder;
    private Float waterEqSnowfall;
    private Boolean sleet;
    private Float precipitation;
    private Boolean hail;
    private Integer temperatureAtObservationTime;
    private Boolean smoke;
    private Float snowDepth;
    private Integer directionOfFastest2MinWind;
    private Float avgWindSpeed;
    private Integer directionOfFastest5SecWind;
    private Integer peakGustTime;
    private Boolean highWinds;
    private Boolean fog;
    private Integer tMax;
    private Integer tMin;
    private Integer tAvg;
    private Float waterEqSnowOnGround;
    private Boolean heavyFog;
    private Float multidayPrecipTotal;

    public String getStation() {
        return station;
    }

    public Integer getDayOfPrecipitation() {
        return dayOfPrecipitation;
    }

    public Float getFastest2MinWindSpeed() {
        return fastest2MinWindSpeed;
    }

    public Float getFastest5SecWindSpeed() {
        return fastest5SecWindSpeed;
    }

    public Float getSnowfall() {
        return snowfall;
    }

    public Boolean getThunder() {
        return thunder;
    }

    public Float getWaterEqSnowfall() {
        return waterEqSnowfall;
    }

    public Boolean getSleet() {
        return sleet;
    }

    public Float getPrecipitation() {
        return precipitation;
    }

    public Boolean getHail() {
        return hail;
    }

    public Integer getTemperatureAtObservationTime() {
        return temperatureAtObservationTime;
    }

    public Boolean getSmoke() {
        return smoke;
    }

    public Float getSnowDepth() {
        return snowDepth;
    }

    public Integer getDirectionOfFastest2MinWind() {
        return directionOfFastest2MinWind;
    }

    public Float getAvgWindSpeed() {
        return avgWindSpeed;
    }

    public Integer getDirectionOfFastest5SecWind() {
        return directionOfFastest5SecWind;
    }

    public Integer getPeakGustTime() {
        return peakGustTime;
    }

    public Boolean getHighWinds() {
        return highWinds;
    }

    public Boolean getFog() {
        return fog;
    }

    public Integer gettMax() {
        return tMax;
    }

    public Integer gettMin() {
        return tMin;
    }

    public Integer gettAvg() {
        return tAvg;
    }

    public Float getWaterEqSnowOnGround() {
        return waterEqSnowOnGround;
    }

    public Boolean getHeavyFog() {
        return heavyFog;
    }

    public Float getMultidayPrecipTotal() {
        return multidayPrecipTotal;
    }

    public WeatherRecord(String name, Integer dayOfPrecipitation, Float fastest2MinWindSpeed, Float fastest5SecWindSpeed, Float snowfall, Boolean thunder, Float waterEqSnowfall, Boolean sleet, Float precipitation, Boolean hail, Integer temperatureAtObservationTime, Boolean smoke, Float snowDepth, Integer directionOfFastest2MinWind, Float avgWindSpeed, Integer directionOfFastest5SecWind, Integer peakGustTime, Boolean highWinds, Boolean fog, Integer tMax, Integer tMin, Integer tAvg, Float waterEqSnowOnGround, Boolean heavyFog, Float multidayPrecipTotal) {
        this.name = name;
        this.dayOfPrecipitation = dayOfPrecipitation;
        this.fastest2MinWindSpeed = fastest2MinWindSpeed;
        this.fastest5SecWindSpeed = fastest5SecWindSpeed;
        this.snowfall = snowfall;
        this.thunder = thunder;
        this.waterEqSnowfall = waterEqSnowfall;
        this.sleet = sleet;
        this.precipitation = precipitation;
        this.hail = hail;
        this.temperatureAtObservationTime = temperatureAtObservationTime;
        this.smoke = smoke;
        this.snowDepth = snowDepth;
        this.directionOfFastest2MinWind = directionOfFastest2MinWind;
        this.avgWindSpeed = avgWindSpeed;
        this.directionOfFastest5SecWind = directionOfFastest5SecWind;
        this.peakGustTime = peakGustTime;
        this.highWinds = highWinds;
        this.fog = fog;
        this.tMax = tMax;
        this.tMin = tMin;
        this.tAvg = tAvg;
        this.waterEqSnowOnGround = waterEqSnowOnGround;
        this.heavyFog = heavyFog;
        this.multidayPrecipTotal = multidayPrecipTotal;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setStation(String station) {
        this.station = station;
    }

    public Date getDate() {
        return date;
    }



/*
DAPR - Number of days included in the multiday precipitation total (MDPR)
WSF2 - Fastest 2-minute wind speed
WSF5 - Fastest 5-second wind speed
SNOW - Snowfall
WT03 - Thunder
WESF - Water equivalent of snowfall
WT04 - Ice pellets, sleet, snow pellets, or small hail"
PRCP - Precipitation
WT05 - Hail (may include small hail)
TOBS - Temperature at the time of observation
WT08 - Smoke or haze
SNWD - Snow depth
WDF2 - Direction of fastest 2-minute wind
AWND - Average wind speed
WDF5 - Direction of fastest 5-second wind
PGTM - Peak gust time
WT11 - High or damaging winds
WT01 - Fog, ice fog, or freezing fog (may include heavy fog)
TMAX - Maximum temperature
WESD - Water equivalent of snow on the ground
WT02 - Heavy fog or heaving freezing fog (not always distinguished from fog)
TAVG - Average Temperature.
TMIN - Minimum temperature
MDPR - Multiday precipitation total (use with DAPR and DWPR, if available)
*/

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("WeatherRecord{");
        sb.append("station='").append(station).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", date=").append(date);
        if(dayOfPrecipitation != null) sb.append(", dayOfPrecipitation=").append(dayOfPrecipitation);
        if(fastest2MinWindSpeed != null) sb.append(", fastest2MinWindSpeed=").append(fastest2MinWindSpeed);
        if(fastest5SecWindSpeed != null) sb.append(", fastest5SecWindSpeed=").append(fastest5SecWindSpeed);
        if(snowfall != null) sb.append(", snowfall=").append(snowfall);
        if(thunder != null) sb.append(", thunder=").append(thunder);
        if(waterEqSnowfall != null) sb.append(", waterEqSnowfall=").append(waterEqSnowfall);
        if(sleet != null) sb.append(", sleet=").append(sleet);
        if(precipitation != null) sb.append(", precipitation=").append(precipitation);
        if(hail != null) sb.append(", hail=").append(hail);
        if(temperatureAtObservationTime != null) sb.append(", temperatureAtObservationTime=").append(temperatureAtObservationTime);
        if(smoke != null) sb.append(", smoke=").append(smoke);
        if(snowDepth != null) sb.append(", snowDepth=").append(snowDepth);
        if(directionOfFastest2MinWind != null) sb.append(", directionOfFastest2MinWind=").append(directionOfFastest2MinWind);
        if(avgWindSpeed != null) sb.append(", avgWindSpeed=").append(avgWindSpeed);
        if(directionOfFastest5SecWind != null) sb.append(", directionOfFastest5SecWind=").append(directionOfFastest5SecWind);
        if(peakGustTime != null) sb.append(", peakGustTime=").append(peakGustTime);
        if(highWinds != null) sb.append(", highWinds=").append(highWinds);
        if(fog != null) sb.append(", fog=").append(fog);
        if(tMax != null) sb.append(", tMax=").append(tMax);
        if(tMin != null) sb.append(", tMin=").append(tMin);
        if(tAvg != null) sb.append(", tAvg=").append(tAvg);
        if(waterEqSnowOnGround != null) sb.append(", waterEqSnowOnGround=").append(waterEqSnowOnGround);
        if(heavyFog != null) sb.append(", heavyFog=").append(heavyFog);
        if(multidayPrecipTotal != null) sb.append(", multidayPrecipTotal=").append(multidayPrecipTotal);
        sb.append('}');
        return sb.toString();
    }

    public String getName() {
        return name;
    }

}

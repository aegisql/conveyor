package com.aegisql.conveyor.demo.weather;

import java.util.function.Supplier;

public class WeatherRecordCollector implements Supplier<WeatherRecord> {
    private String name;
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

    @Override
    public WeatherRecord get() {
        return new WeatherRecord(name
        ,dayOfPrecipitation
        ,fastest2MinWindSpeed
        ,fastest5SecWindSpeed
        ,snowfall
        ,thunder
        ,waterEqSnowfall
        ,sleet
        ,precipitation
        ,hail
        ,temperatureAtObservationTime
        ,smoke
        ,snowDepth
        ,directionOfFastest2MinWind
        ,avgWindSpeed
        ,directionOfFastest5SecWind
        ,peakGustTime
        ,highWinds
        ,fog
        ,tMax
        ,tMin
        ,tAvg
        ,waterEqSnowOnGround
        ,heavyFog
        ,multidayPrecipTotal);
    }

    public void name(String name) {
        this.name = name.substring(1).substring(0,name.lastIndexOf("\"")-1);
    }

    public void dayOfPrecipitation(String dapr) {
        this.dayOfPrecipitation = Integer.parseInt(dapr);
    }

    public void fastest2MinWindSpeed(String wsf2) {
        this.fastest2MinWindSpeed = Float.parseFloat(wsf2);
    }

    public void fastest5SecWindSpeed(String wsf5) {
        this.fastest5SecWindSpeed = Float.parseFloat(wsf5);
    }

    public void snowfall(String snow) {
        this.snowfall = Float.parseFloat(snow);
    }

    public void thunder(String wt03) {
        this.thunder = wt03.equals("1");
    }

    public void waterEqSnowfall(String wesf) {
        this.waterEqSnowfall = Float.parseFloat(wesf);
    }

    public void sleet(String wt04) {
        this.sleet = wt04.equals("1");
    }

    public void precipitation(String prcp) {
        this.precipitation = Float.parseFloat(prcp);
    }

    public void hail(String wt05 ) {
        this.hail = wt05.equals("1");
    }

    public void temperatureAtObservationTime(String tobs) {
        this.temperatureAtObservationTime = Integer.parseInt(tobs);
    }

    public <T> void smoke(T wt08) {
        this.smoke = wt08.equals("1");
    }

    public void snowDepth(String snwd) {
        this.snowDepth = Float.parseFloat(snwd);
    }

    public void directionOfFastest2MinWind(String wdf2) {
        this.directionOfFastest2MinWind = Integer.parseInt(wdf2);
    }

    public void avgWindSpeed(String awnd) {
        this.avgWindSpeed = Float.parseFloat(awnd);
    }

    public void directionOfFastest5SecWind(String wdf5) {
        this.directionOfFastest5SecWind = Integer.parseInt(wdf5);
    }

    public void peakGustTime(String pgtm) {
        this.peakGustTime = Integer.parseInt(pgtm);
    }

    public void highWinds(String wt11) {
        this.highWinds = wt11.equals("1");
    }

    public void fog(String wt01) {
        this.fog = wt01.equals("1");
    }

    public void temperatureMax(String tmax) {
        this.tMax = Integer.parseInt(tmax);
    }

    public void temperatureMin(String tmin) {
        this.tMin = Integer.parseInt(tmin);
    }

    public void temperatureAvg(String tavg) {
        this.tAvg = Integer.parseInt(tavg);
    }

    public void waterEqSnowOnGround(String wesd) {
        this.waterEqSnowOnGround = Float.parseFloat(wesd);
    }

    public void heavyFog(String wt02) {
        this.heavyFog = wt02.equals("1");
    }

    public void multidayPrecipTotal(String mdpr) {
        this.multidayPrecipTotal = Float.parseFloat(mdpr);
    }

}

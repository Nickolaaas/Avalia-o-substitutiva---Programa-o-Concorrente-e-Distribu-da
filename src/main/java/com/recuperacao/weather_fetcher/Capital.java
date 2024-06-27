package com.recuperacao.weather_fetcher;

public class Capital {
    private String name;
    private double latitude;
    private double longitude;

    public Capital(String name, double latitude, double longitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}

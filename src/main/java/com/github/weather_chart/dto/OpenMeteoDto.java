package com.github.weather_chart.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OpenMeteoDto {
    private double latitude;
    private double longitude;
    private double generationtime_ms;
    private double utc_offset_seconds;
    private String timezone;
    private String timezone_abbreviation;
    private double elevation;
    private HourlyUnits hourly_units;
    private Hourly hourly;

    @Data
    public static class HourlyUnits {
        private String time;
        private String temperature_2m;
    }

    @Data
    public static class Hourly {
        private LocalDateTime[] time;
        private double[] temperature_2m;
    }
}

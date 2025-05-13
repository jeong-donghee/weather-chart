package com.github.weather_chart.controller;

import com.github.weather_chart.service.WeatherChartService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;

@Controller
@RequiredArgsConstructor
public class WeatherChartController {

    private final WeatherChartService weatherChartService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/download")
    public void download(HttpServletResponse response) throws IOException {
        weatherChartService.download(response);
    }
}

package com.example.demo.controller;

import com.example.demo.service.CustomMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TraceAndLogDemoController {

    private static final Logger log = LoggerFactory.getLogger(TraceAndLogDemoController.class);
    @Autowired
    private CustomMetricsService customMetricsService;

    @GetMapping("traceAndLog")
    public String test() {
        log.info("[OpenTelemetry] Demo request started");
        log.info("[OpenTelemetry] Business logic step 1 completed");
        log.info("[OpenTelemetry] Business logic step 2 completed");
        log.info("[OpenTelemetry] Demo request finished");
        return "OpenTelemetry demo response";
    }

    @GetMapping("customMetrics")
    public String customMetrics() {
        log.info("[OpenTelemetry] Demo customMetrics requested");
        customMetricsService.incrementRequestCounter();
        return "simple custom metircs";
    }



}

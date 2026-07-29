package com.example.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/test")
public class DemoController {

    private static final Logger log = LoggerFactory.getLogger(DemoController.class);

    @GetMapping
    public String test() {
        log.info("[OpenTelemetry] Demo request started");
        log.info("[OpenTelemetry] Processing demo request, traceId={}", generateTraceId());
        log.info("[OpenTelemetry] Business logic step 1 completed");
        log.info("[OpenTelemetry] Business logic step 2 completed");
        log.info("[OpenTelemetry] Demo request finished");
        return "OpenTelemetry demo response";
    }

    private String generateTraceId() {
        return Long.toHexString(ThreadLocalRandom.current().nextLong());
    }
}

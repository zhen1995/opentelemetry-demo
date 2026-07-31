package com.example.demo.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/metrics")
public class MetricsController {

    private static final String CONTENT_TYPE = "text/plain; version=0.0.4; charset=utf-8";

    private final MeterRegistry meterRegistry;

    public MetricsController(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> metrics() {
        Optional<PrometheusMeterRegistry> prometheus = findPrometheusRegistry(meterRegistry);
        if (prometheus.isPresent()) {
            return ResponseEntity.ok()
                    .header("Content-Type", CONTENT_TYPE)
                    .body(prometheus.get().scrape());
        }
        return ResponseEntity.ok()
                .header("Content-Type", CONTENT_TYPE)
                .body("# Prometheus metrics export is disabled.");
    }

    private Optional<PrometheusMeterRegistry> findPrometheusRegistry(MeterRegistry registry) {
        if (registry instanceof PrometheusMeterRegistry) {
            return Optional.of((PrometheusMeterRegistry) registry);
        }
        if (registry instanceof io.micrometer.core.instrument.composite.CompositeMeterRegistry) {
            return ((io.micrometer.core.instrument.composite.CompositeMeterRegistry) registry)
                    .getRegistries().stream()
                    .filter(PrometheusMeterRegistry.class::isInstance)
                    .map(PrometheusMeterRegistry.class::cast)
                    .findFirst();
        }
        return Optional.empty();
    }
}

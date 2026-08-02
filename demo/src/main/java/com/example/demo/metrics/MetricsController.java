package com.example.demo.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/metrics")
public class MetricsController {

    private static final String CONTENT_TYPE = "text/plain; version=0.0.4; charset=utf-8";
    private static final String OPENMETRICS_CONTENT_TYPE = "application/openmetrics-text; version=1.0.0; charset=utf-8";

    private final MeterRegistry meterRegistry;

    public MetricsController(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @GetMapping(produces = {MediaType.TEXT_PLAIN_VALUE, "application/openmetrics-text"})
    public ResponseEntity<String> metrics(@RequestHeader(value = "Accept", required = false) String accept) {
        Optional<PrometheusMeterRegistry> prometheus = findPrometheusRegistry(meterRegistry);
        if (prometheus.isPresent()) {
            boolean openMetrics = shouldUseOpenMetrics(accept);
            String content = openMetrics
                    ? prometheus.get().scrape(TextFormat.CONTENT_TYPE_OPENMETRICS_100)
                    : prometheus.get().scrape();
            String contentType = openMetrics ? OPENMETRICS_CONTENT_TYPE : CONTENT_TYPE;
            return ResponseEntity.ok()
                    .header("Content-Type", contentType)
                    .body(content);
        }
        return ResponseEntity.ok()
                .header("Content-Type", CONTENT_TYPE)
                .body("# Prometheus metrics export is disabled.");
    }

    private boolean shouldUseOpenMetrics(String accept) {
        if (accept == null || accept.isBlank()) {
            return true; // 默认返回 OpenMetrics，方便浏览器直接查看 exemplar
        }
        String lower = accept.toLowerCase();
        // 显式请求 OpenMetrics
        if (lower.contains("application/openmetrics-text")) {
            return true;
        }
        // 浏览器默认 Accept 或通配，也返回 OpenMetrics
        if (lower.contains("text/html") || lower.contains("*/*")) {
            return true;
        }
        // 其他情况（如 Prometheus 老版本的 text/plain）返回 Prometheus 文本格式
        return false;
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

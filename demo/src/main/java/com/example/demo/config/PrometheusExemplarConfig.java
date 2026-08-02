package com.example.demo.config;

import io.prometheus.client.exemplars.DefaultExemplarSampler;
import io.prometheus.client.exemplars.tracer.common.SpanContextSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 为 Prometheus 指标启用 Exemplar 支持。
 * <p>
 * 注册 SpanContextSupplier 和 DefaultExemplarSampler bean，
 * Spring Boot 2.7 会自动把 ExemplarSampler 注入到 PrometheusMeterRegistry。
 */
@Configuration
public class PrometheusExemplarConfig {

    @Bean
    public SpanContextSupplier spanContextSupplier() {
        return new OpenTelemetrySpanContextSupplier();
    }

    @Bean
    public DefaultExemplarSampler openTelemetryExemplarSampler(SpanContextSupplier spanContextSupplier) {
        return new DefaultExemplarSampler(spanContextSupplier);
    }
}

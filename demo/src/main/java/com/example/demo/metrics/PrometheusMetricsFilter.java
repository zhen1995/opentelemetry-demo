package com.example.demo.metrics;

import com.example.demo.config.OpenTelemetrySpanContextSupplier;
import io.micrometer.core.instrument.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PrometheusMetricsFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(PrometheusMetricsFilter.class);

    private final MeterRegistry meterRegistry;
    private final String appName;

    private final ConcurrentHashMap<String, AtomicInteger> requestsInProgress = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> requestCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> responseCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> exceptionCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Timer> requestTimers = new ConcurrentHashMap<>();

    public PrometheusMetricsFilter(MeterRegistry meterRegistry, @Value("${app.name:demo}") String appName) {
        this.meterRegistry = meterRegistry;
        this.appName = appName;

        Gauge.builder("fastapi.app.info", () -> 1)
                .description("FastAPI application information.")
                .tag("app_name", appName)
                .register(meterRegistry);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = getPath(httpRequest);
        String method = httpRequest.getMethod();

        if ("/metrics".equals(path)) {
            chain.doFilter(request, response);
            return;
        }

        // 在请求开始时保存当前请求的 traceId/spanId：
        // 优先从 traceparent header 解析，避免 OTel API 与 agent 版本不兼容；
        // 没有 traceparent 时回退到 Span.current()。
        String traceparent = httpRequest.getHeader("traceparent");
        OpenTelemetrySpanContextSupplier.setFromTraceParent(traceparent);
        log.info("traceparent={}, resolved traceId={}, spanId={}",
                traceparent,
                OpenTelemetrySpanContextSupplier.getCurrentTraceId(),
                OpenTelemetrySpanContextSupplier.getCurrentSpanId());

        AtomicInteger inProgress = getRequestsInProgress(method, path);
        inProgress.incrementAndGet();
        getRequestCounter(method, path).increment();

        long start = System.nanoTime();
        int statusCode = 500;
        try {
            chain.doFilter(request, response);
            statusCode = httpResponse.getStatus();
        } catch (Exception e) {
            statusCode = 500;
            getExceptionCounter(method, path, e.getClass().getSimpleName()).increment();
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            if (e instanceof ServletException) {
                throw (ServletException) e;
            }
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new ServletException(e);
        } finally {
            long elapsedNanos = System.nanoTime() - start;
            getRequestTimer(method, path).record(Duration.ofNanos(elapsedNanos));
            getResponseCounter(method, path, String.valueOf(statusCode)).increment();
            inProgress.decrementAndGet();
            OpenTelemetrySpanContextSupplier.clear();
        }
    }

    private String getPath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String requestUri = request.getRequestURI();
        if (contextPath != null && !contextPath.isEmpty() && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }

    private AtomicInteger getRequestsInProgress(String method, String path) {
        String key = method + "|" + path;
        return requestsInProgress.computeIfAbsent(key, k -> {
            AtomicInteger value = new AtomicInteger(0);
            Gauge.builder("fastapi.requests.in_progress", value, AtomicInteger::get)
                    .description("Gauge of requests")
                    .tag("method", method)
                    .tag("path", path)
                    .tag("app_name", appName)
                    .register(meterRegistry);
            return value;
        });
    }

    private Counter getRequestCounter(String method, String path) {
        String key = method + "|" + path;
        return requestCounters.computeIfAbsent(key, k -> Counter.builder("fastapi.requests.total")
                .description("Total count of request by method and path.")
                .tag("method", method)
                .tag("path", path)
                .tag("app_name", appName)
                .register(meterRegistry));
    }

    private Counter getResponseCounter(String method, String path, String statusCode) {
        String key = method + "|" + path + "|" + statusCode;
        return responseCounters.computeIfAbsent(key, k -> Counter.builder("fastapi.responses.total")
                .description("Total count of responses by method, path and status code")
                .tag("method", method)
                .tag("path", path)
                .tag("status_code", statusCode)
                .tag("app_name", appName)
                .register(meterRegistry));
    }

    private Counter getExceptionCounter(String method, String path, String exceptionType) {
        String key = method + "|" + path + "|" + exceptionType;
        return exceptionCounters.computeIfAbsent(key, k -> Counter.builder("fastapi.exceptions.total")
                .description("Total count of exceptions")
                .tag("method", method)
                .tag("path", path)
                .tag("exception_type", exceptionType)
                .tag("app_name", appName)
                .register(meterRegistry));
    }

    private Timer getRequestTimer(String method, String path) {
        String key = method + "|" + path;
        return requestTimers.computeIfAbsent(key, k -> Timer.builder("fastapi.request.duration")
                .description("Histogram of requests progressing time by path")
                .tag("method", method)
                .tag("path", path)
                .tag("app_name", appName)
                .publishPercentileHistogram()
                .register(meterRegistry));
    }
}

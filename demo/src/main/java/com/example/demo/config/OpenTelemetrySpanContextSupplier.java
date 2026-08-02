package com.example.demo.config;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.prometheus.client.exemplars.tracer.common.SpanContextSupplier;

/**
 * 为 Prometheus exemplar 提供 traceId/spanId。
 * <p>
 * 优先从 HTTP 请求的 traceparent header 中解析，避免依赖 OpenTelemetry API 与 agent 的版本兼容性；
 * 如果 traceparent 不存在，则回退到 {@link Span#current()}。
 * 使用 ThreadLocal 保存请求级别的 traceId/spanId，避免 span 关闭后无法获取。
 */
public class OpenTelemetrySpanContextSupplier implements SpanContextSupplier {

    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> SPAN_ID = new ThreadLocal<>();

    /**
     * 从 HTTP 请求的 traceparent header 中解析 traceId/spanId。
     * <p>
     * traceparent 格式：00-&lt;traceId&gt;-&lt;spanId&gt;-&lt;flags&gt;
     */
    public static void setFromTraceParent(String traceparent) {
        if (traceparent == null || traceparent.isBlank()) {
            // 没有 traceparent header 时回退到当前 span
            setCurrentSpan();
            return;
        }
        String[] parts = traceparent.trim().split("-");
        if (parts.length >= 3 && parts[1].length() == 32 && parts[2].length() == 16) {
            TRACE_ID.set(parts[1]);
            SPAN_ID.set(parts[2]);
        } else {
            setCurrentSpan();
        }
    }

    /**
     * 从当前线程的 OpenTelemetry span 中获取 traceId/spanId。
     */
    public static void setCurrentSpan() {
        SpanContext spanContext = Span.current().getSpanContext();
        if (spanContext.isValid()) {
            TRACE_ID.set(spanContext.getTraceId());
            SPAN_ID.set(spanContext.getSpanId());
        }
    }

    /**
     * 清理当前线程保存的 traceId/spanId，防止线程复用导致脏数据。
     */
    public static void clear() {
        TRACE_ID.remove();
        SPAN_ID.remove();
    }

    @Override
    public String getTraceId() {
        return TRACE_ID.get();
    }

    @Override
    public String getSpanId() {
        return SPAN_ID.get();
    }

    /**
     * 获取当前线程保存的 traceId（静态方法，供调试使用）。
     */
    public static String getCurrentTraceId() {
        return TRACE_ID.get();
    }

    /**
     * 获取当前线程保存的 spanId（静态方法，供调试使用）。
     */
    public static String getCurrentSpanId() {
        return SPAN_ID.get();
    }
}

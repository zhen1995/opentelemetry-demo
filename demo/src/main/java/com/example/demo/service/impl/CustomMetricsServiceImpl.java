package com.example.demo.service.impl;

import com.example.demo.service.CustomMetricsService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Description 自定义指标
 * @Author zhenpeng
 * @Date 2026/7/31 8:57
 * @Version 1.0.0
 */
@Service
public class CustomMetricsServiceImpl implements CustomMetricsService {
    @Autowired
    private MeterRegistry meterRegistry;

    @Override
    public void incrementRequestCounter() {
        Counter counter = Counter.builder("simpleRequestTotal")  //名称
                .description("total request") //描述
                .tag("app", "demo")  //标签
                .tag("method","customMetrics")
                .register(meterRegistry);//绑定的MeterRegistry
        counter.increment();
    }
}

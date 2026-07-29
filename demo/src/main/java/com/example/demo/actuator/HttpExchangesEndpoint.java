package com.example.demo.actuator;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.trace.http.HttpTrace;
import org.springframework.boot.actuate.trace.http.HttpTraceRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Endpoint(id = "httpexchanges")
public class HttpExchangesEndpoint {

    private final HttpTraceRepository httpTraceRepository;

    public HttpExchangesEndpoint(HttpTraceRepository httpTraceRepository) {
        this.httpTraceRepository = httpTraceRepository;
    }

    @ReadOperation
    public List<HttpTrace> exchanges() {
        return httpTraceRepository.findAll();
    }
}

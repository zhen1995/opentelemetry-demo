package com.example.demo.actuator;

import org.springframework.boot.actuate.trace.http.HttpTrace;
import org.springframework.boot.actuate.trace.http.HttpTraceRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/actuator/httptrace-new")
public class HttpTraceNewController {

    private final HttpTraceRepository httpTraceRepository;

    public HttpTraceNewController(HttpTraceRepository httpTraceRepository) {
        this.httpTraceRepository = httpTraceRepository;
    }

    @GetMapping
    public List<HttpTrace> traces() {
        return httpTraceRepository.findAll();
    }
}

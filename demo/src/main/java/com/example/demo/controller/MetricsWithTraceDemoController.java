package com.example.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/")
public class MetricsWithTraceDemoController {

    private static final Logger log = LoggerFactory.getLogger(MetricsWithTraceDemoController.class);
    private final RestTemplate restTemplate;

    @Value("${demo.target-one-host:app-b}")
    private String targetOneHost;

    @Value("${demo.target-two-host:app-c}")
    private String targetTwoHost;

    public MetricsWithTraceDemoController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/io_task")
    public String ioTask() throws InterruptedException {
        Thread.sleep(1000);
        log.error("io task");
        return "IO Bound task finish!";
    }

    @GetMapping("/cpu_task")
    public String cpuTask() {
        for (int i = 0; i < 1000; i++) {
            int ignored = i * i * i;
        }
        log.error("CPU TASK");
        return "CPU TASK FINISH";
    }
}

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
public class PythonPortController {

    private static final Logger log = LoggerFactory.getLogger(PythonPortController.class);
    private final RestTemplate restTemplate;

    @Value("${demo.target-one-host:app-b}")
    private String targetOneHost;

    @Value("${demo.target-two-host:app-c}")
    private String targetTwoHost;

    public PythonPortController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping
    public Map<String, String> readRoot() {
        log.error("Hello World");
        Map<String, String> result = new HashMap<>();
        result.put("Hello", "World");
        return result;
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

    @GetMapping("/chain")
    public Map<String, String> chain() {
        String localUrl = "http://localhost:8080/demo/";
        String ioUrl = String.format("http://%s:8000/io_task", targetOneHost);
        String cpuUrl = String.format("http://%s:8000/cpu_task", targetTwoHost);

        ResponseEntity<String> localResponse = restTemplate.getForEntity(localUrl, String.class);
        log.info("Local response: {}", localResponse.getBody());

        ResponseEntity<String> ioResponse = restTemplate.getForEntity(ioUrl, String.class);
        log.info("IO response: {}", ioResponse.getBody());

        ResponseEntity<String> cpuResponse = restTemplate.getForEntity(cpuUrl, String.class);
        log.info("CPU response: {}", cpuResponse.getBody());

        log.info("Chain Finished");
        Map<String, String> result = new HashMap<>();
        result.put("ok", "true");
        return result;
    }
}

package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.metrics.AutoConfigureMetrics;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureMetrics
class PythonPortControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rootReturnsHelloWorld() throws Exception {
        mockMvc.perform(get("/demo/").contextPath("/demo"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"Hello\":\"World\"}"));
    }

    @Test
    void ioTaskReturnsExpectedString() throws Exception {
        mockMvc.perform(get("/demo/io_task").contextPath("/demo"))
                .andExpect(status().isOk())
                .andExpect(content().string("IO Bound task finish!"));
    }

    @Test
    void cpuTaskReturnsExpectedString() throws Exception {
        mockMvc.perform(get("/demo/cpu_task").contextPath("/demo"))
                .andExpect(status().isOk())
                .andExpect(content().string("CPU TASK FINISH"));
    }

    @Test
    void metricsEndpointContainsCustomMetrics() throws Exception {
        mockMvc.perform(get("/demo/").contextPath("/demo"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/demo/metrics").contextPath("/demo"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("fastapi_app_info")))
                .andExpect(content().string(containsString("fastapi_requests_total")))
                .andExpect(content().string(containsString("fastapi_responses_total")))
                .andExpect(content().string(containsString("fastapi_request_duration_seconds")))
                .andExpect(content().string(containsString("fastapi_requests_in_progress")));
    }
}

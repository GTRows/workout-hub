package com.workouthub.common.web;

import java.nio.charset.StandardCharsets;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusOutputFormat;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusScrapeEndpoint;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MetricsController {

    private static final String PROMETHEUS_TEXT_CONTENT_TYPE =
            "text/plain; version=0.0.4; charset=utf-8";

    private final PrometheusScrapeEndpoint scrape;

    public MetricsController(PrometheusScrapeEndpoint scrape) {
        this.scrape = scrape;
    }

    @GetMapping(value = "/metrics", produces = PROMETHEUS_TEXT_CONTENT_TYPE)
    public ResponseEntity<String> metrics() {
        WebEndpointResponse<byte[]> response =
                scrape.scrape(PrometheusOutputFormat.CONTENT_TYPE_004, null);
        String body = new String(response.getBody(), StandardCharsets.UTF_8);
        return ResponseEntity.status(response.getStatus())
                .contentType(MediaType.parseMediaType(PROMETHEUS_TEXT_CONTENT_TYPE))
                .body(body);
    }
}

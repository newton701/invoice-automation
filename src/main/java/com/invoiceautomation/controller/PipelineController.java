package com.invoiceautomation.controller;

import com.invoiceautomation.service.PipelineService;
import com.invoiceautomation.service.PipelineSummary;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/pipeline")
public class PipelineController {
    private final PipelineService pipelineService;
    public PipelineController(PipelineService pipelineService) { this.pipelineService = pipelineService; }

    @PostMapping("/run")
    public ResponseEntity<PipelineSummary> run() throws IOException {
        PipelineSummary summary = pipelineService.runPipeline();
        return ResponseEntity.status(summary.success() ? 200 : 422).body(summary);
    }

    @GetMapping("/health")
    public Map<String, String> health() { return Map.of("status", "UP", "application", "invoice-automation"); }
}

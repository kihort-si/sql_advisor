package com.sql_advisor.api;

import com.sql_advisor.service.AdvisorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class AnalysisController {
    private final AdvisorService advisorService;

    public AnalysisController(AdvisorService advisorService) {
        this.advisorService = advisorService;
    }

    @PostMapping("/connections/test")
    public ConnectionTestResponse test(@Valid @RequestBody ConnectionDetails connection) {
        return advisorService.testConnection(connection);
    }

    @PostMapping("/analyses")
    public AnalysisResponse analyze(@Valid @RequestBody AnalysisRequest request) {
        return advisorService.analyze(request);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "sql-optimizer-advisor");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }
}

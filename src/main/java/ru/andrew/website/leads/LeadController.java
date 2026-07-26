package ru.andrew.website.leads;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
final class LeadController {
    private final LeadAcceptanceService acceptance;
    private final LeadMetrics metrics;

    LeadController(
            LeadAcceptanceService acceptance, LeadMetrics metrics) {
        this.acceptance = acceptance;
        this.metrics = metrics;
    }

    @PostMapping(path = "/api/leads", consumes = "application/json")
    ResponseEntity<Void> submit(@RequestBody LeadRequest request) {
        AcceptanceOutcome outcome = acceptance.accept(request);
        metrics.accepted(outcome);
        return ResponseEntity.accepted().build();
    }
}

package ru.andrew.website.leads;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
final class LeadController {
    private final LeadAcceptanceService acceptance;

    LeadController(LeadAcceptanceService acceptance) {
        this.acceptance = acceptance;
    }

    @PostMapping(path = "/api/leads", consumes = "application/json")
    ResponseEntity<Void> submit(@RequestBody LeadRequest request) {
        acceptance.accept(request);
        return ResponseEntity.accepted().build();
    }
}

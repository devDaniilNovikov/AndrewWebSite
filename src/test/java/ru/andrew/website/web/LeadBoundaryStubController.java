package ru.andrew.website.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
class LeadBoundaryStubController {
    @PostMapping(path = "/api/leads", consumes = "application/json")
    ResponseEntity<Void> accept(@RequestBody JsonNode ignored) {
        return ResponseEntity.accepted().build();
    }
}

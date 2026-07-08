package com.dietapp.diet_app.progress.controller;

import com.dietapp.diet_app.progress.dto.request.WeightLogRequest;
import com.dietapp.diet_app.progress.dto.response.WeightLogResponse;
import com.dietapp.diet_app.progress.service.ProgressService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/progress")
@AllArgsConstructor
public class ProgressController {

    @Autowired
    private final ProgressService service;


    // Log weight
    @PostMapping("/weight")
    public void logWeight(
            @RequestBody WeightLogRequest request,
            Principal principal
    ) {
        service.logWeight(
                UUID.fromString(principal.getName()),
                request
        );
    }

    // Get latest weight log
    @GetMapping("/weight/latest")
    public WeightLogResponse latest(Principal principal) {
        return service.latest(UUID.fromString(principal.getName()));
    }

    // Get weight log history
    @GetMapping("/weight/history")
    public List<WeightLogResponse> history(Principal principal) {
        return service.history(UUID.fromString(principal.getName()));
    }
}


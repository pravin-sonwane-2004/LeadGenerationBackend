package com.btechloanwala.LeadGeneration.controller;

import com.btechloanwala.LeadGeneration.dto.request.EligibilityRequest;
import com.btechloanwala.LeadGeneration.dto.response.ApiResponse;
import com.btechloanwala.LeadGeneration.service.EligibilityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class EligibilityController {

    private final EligibilityService service;

    public EligibilityController(EligibilityService service) {
        this.service = service;
    }

    @PostMapping("/eligibility")
    public ResponseEntity<ApiResponse> create(
            @Valid @RequestBody EligibilityRequest request) {

        service.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse(true, "Eligibility check submitted successfully."));
    }
}
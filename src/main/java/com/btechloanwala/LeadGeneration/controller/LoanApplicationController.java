package com.btechloanwala.LeadGeneration.controller;

import com.btechloanwala.LeadGeneration.dto.request.LoanApplicationRequest;
import com.btechloanwala.LeadGeneration.dto.response.ApiResponse;
import com.btechloanwala.LeadGeneration.service.LoanApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class LoanApplicationController {

    private final LoanApplicationService service;

    public LoanApplicationController(LoanApplicationService service) {
        this.service = service;
    }

    @PostMapping("/apply-now")
    public ResponseEntity<ApiResponse> create(
            @Valid @RequestBody LoanApplicationRequest request) {

        service.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse(true, "Application submitted successfully."));
    }
}
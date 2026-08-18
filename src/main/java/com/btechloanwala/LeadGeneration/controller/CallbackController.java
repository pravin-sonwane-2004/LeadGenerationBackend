package com.btechloanwala.LeadGeneration.controller;

import com.btechloanwala.LeadGeneration.dto.request.CallbackRequestDTO;
import com.btechloanwala.LeadGeneration.dto.response.ApiResponse;
import com.btechloanwala.LeadGeneration.service.CallbackService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CallbackController {

    private final CallbackService service;

    public CallbackController(CallbackService service) {
        this.service = service;
    }

    @PostMapping("/callback")
    public ResponseEntity<ApiResponse> create(
            @Valid @RequestBody CallbackRequestDTO request) {

        service.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse(true, "Callback requested successfully."));
    }
}
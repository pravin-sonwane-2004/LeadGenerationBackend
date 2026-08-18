package com.btechloanwala.LeadGeneration.controller;

import com.btechloanwala.LeadGeneration.dto.request.ContactRequest;
import com.btechloanwala.LeadGeneration.dto.response.ApiResponse;
import com.btechloanwala.LeadGeneration.service.ContactService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ContactController {

    private final ContactService service;

    public ContactController(ContactService service) {
        this.service = service;
    }

    @PostMapping("/contact")
    public ResponseEntity<ApiResponse> create(
            @Valid @RequestBody ContactRequest request) {

        service.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse(true, "Message received successfully."));
    }
}
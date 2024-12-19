package com.gt.person.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gt.global.common.entity.ApiResponse;
import com.gt.global.common.exception.EntityNotSaveException;
import com.gt.person.api.dto.PersonCreateRequest;
import com.gt.person.application.service.PersonService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/person")
@RequiredArgsConstructor
public class PersonController {
    
    private final PersonService personVervice;

    /**
     * 고객 생성
     * @param createRequest
     * @return
     */
    @PostMapping("/create")
    public ResponseEntity<?> createPerson(@Valid @RequestBody PersonCreateRequest createRequest) {
        
        try {
            Long personId = personVervice.createPerson(createRequest);
            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Person created successfully", personId));
        } catch (EntityNotSaveException e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false,  "Failed to create person: " + e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "An unexpected error occurred", null));
        }
        
    }
}

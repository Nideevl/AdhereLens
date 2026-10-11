package com.adherelens.identity.web;

import com.adherelens.identity.dto.CreateHospitalRequest;
import com.adherelens.identity.dto.CreateHospitalResponse;
import com.adherelens.identity.dto.HospitalResponse;
import com.adherelens.identity.service.HospitalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/hospitals")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class HospitalController {

    private final HospitalService hospitalService;

    public HospitalController(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateHospitalResponse create(@Valid @RequestBody CreateHospitalRequest request) {
        return hospitalService.create(request);
    }

    @GetMapping
    public List<HospitalResponse> list() {
        return hospitalService.list();
    }
}

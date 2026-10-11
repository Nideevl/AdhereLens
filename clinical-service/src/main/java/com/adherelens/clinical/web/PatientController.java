package com.adherelens.clinical.web;

import com.adherelens.auth.AuthUser;
import com.adherelens.clinical.dto.PatientRequest;
import com.adherelens.clinical.dto.PatientResponse;
import com.adherelens.clinical.service.PatientService;
import com.adherelens.common.model.CareSetting;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN', 'DOCTOR')")
    public PatientResponse create(@AuthenticationPrincipal AuthUser caller,
                                  @Valid @RequestBody PatientRequest request) {
        return patientService.create(caller.hospitalId(), request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN', 'DOCTOR', 'NURSE')")
    public List<PatientResponse> list(@AuthenticationPrincipal AuthUser caller,
                                      @RequestParam(required = false) CareSetting careSetting) {
        return patientService.list(caller.hospitalId(), careSetting);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN', 'DOCTOR', 'NURSE')")
    public PatientResponse get(@AuthenticationPrincipal AuthUser caller, @PathVariable UUID id) {
        return patientService.get(caller.hospitalId(), id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN', 'DOCTOR')")
    public PatientResponse update(@AuthenticationPrincipal AuthUser caller,
                                  @PathVariable UUID id,
                                  @Valid @RequestBody PatientRequest request) {
        return patientService.update(caller.hospitalId(), id, request);
    }
}

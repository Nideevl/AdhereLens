package com.adherelens.clinical.web;

import com.adherelens.auth.AuthUser;
import com.adherelens.clinical.dto.RegimenRequest;
import com.adherelens.clinical.dto.RegimenResponse;
import com.adherelens.clinical.service.RegimenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class RegimenController {

    private final RegimenService regimenService;

    public RegimenController(RegimenService regimenService) {
        this.regimenService = regimenService;
    }

    @PostMapping("/patients/{patientId}/regimens")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('DOCTOR')")
    public RegimenResponse create(@AuthenticationPrincipal AuthUser caller,
                                  @PathVariable UUID patientId,
                                  @Valid @RequestBody RegimenRequest request) {
        return regimenService.create(caller.hospitalId(), caller.staffId(), patientId, request);
    }

    @GetMapping("/patients/{patientId}/regimens")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN', 'DOCTOR', 'NURSE')")
    public List<RegimenResponse> listForPatient(@AuthenticationPrincipal AuthUser caller,
                                                @PathVariable UUID patientId) {
        return regimenService.listForPatient(caller.hospitalId(), patientId);
    }

    @GetMapping("/regimens/{id}")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN', 'DOCTOR', 'NURSE')")
    public RegimenResponse get(@AuthenticationPrincipal AuthUser caller, @PathVariable UUID id) {
        return regimenService.get(caller.hospitalId(), id);
    }

    @PostMapping("/regimens/{id}/discontinue")
    @PreAuthorize("hasRole('DOCTOR')")
    public RegimenResponse discontinue(@AuthenticationPrincipal AuthUser caller, @PathVariable UUID id) {
        return regimenService.discontinue(caller.hospitalId(), id);
    }
}

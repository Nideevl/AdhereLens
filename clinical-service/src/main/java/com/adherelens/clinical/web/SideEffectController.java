package com.adherelens.clinical.web;

import com.adherelens.auth.AuthUser;
import com.adherelens.clinical.dto.RetractRequest;
import com.adherelens.clinical.dto.SideEffectReportRequest;
import com.adherelens.clinical.dto.SideEffectResponse;
import com.adherelens.clinical.service.SideEffectService;
import com.adherelens.common.model.SideEffect;
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

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class SideEffectController {

    private final SideEffectService sideEffectService;

    public SideEffectController(SideEffectService sideEffectService) {
        this.sideEffectService = sideEffectService;
    }

    /** The fixed list a screen shows as choices. */
    @GetMapping("/side-effects/catalog")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN', 'DOCTOR', 'NURSE')")
    public List<String> catalog() {
        return Arrays.stream(SideEffect.values()).map(Enum::name).toList();
    }

    @PostMapping("/regimens/{regimenId}/side-effects")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('NURSE', 'DOCTOR')")
    public List<SideEffectResponse> report(@AuthenticationPrincipal AuthUser caller,
                                           @PathVariable UUID regimenId,
                                           @Valid @RequestBody SideEffectReportRequest request) {
        return sideEffectService.report(caller, regimenId, request);
    }

    @GetMapping("/patients/{patientId}/side-effects")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN', 'DOCTOR', 'NURSE')")
    public List<SideEffectResponse> listForPatient(@AuthenticationPrincipal AuthUser caller,
                                                   @PathVariable UUID patientId) {
        return sideEffectService.listForPatient(caller.hospitalId(), patientId);
    }

    @PostMapping("/side-effects/{id}/retract")
    @PreAuthorize("hasRole('DOCTOR')")
    public SideEffectResponse retract(@AuthenticationPrincipal AuthUser caller,
                                      @PathVariable UUID id,
                                      @Valid @RequestBody RetractRequest request) {
        return sideEffectService.retract(caller, id, request.reason());
    }
}

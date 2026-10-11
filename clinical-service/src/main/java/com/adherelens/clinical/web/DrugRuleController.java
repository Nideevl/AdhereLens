package com.adherelens.clinical.web;

import com.adherelens.auth.AuthUser;
import com.adherelens.clinical.dto.DoseSuggestionResponse;
import com.adherelens.clinical.dto.DrugRuleRequest;
import com.adherelens.clinical.dto.DrugRuleResponse;
import com.adherelens.clinical.service.DrugRuleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class DrugRuleController {

    private final DrugRuleService drugRuleService;

    public DrugRuleController(DrugRuleService drugRuleService) {
        this.drugRuleService = drugRuleService;
    }

    @PostMapping("/drug-rules")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('DOCTOR')")
    public DrugRuleResponse create(@AuthenticationPrincipal AuthUser caller,
                                   @Valid @RequestBody DrugRuleRequest request) {
        return drugRuleService.create(caller, request);
    }

    @GetMapping("/drug-rules")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN', 'DOCTOR', 'NURSE')")
    public List<DrugRuleResponse> list(@AuthenticationPrincipal AuthUser caller,
                                       @RequestParam(required = false) String drugName) {
        return drugRuleService.list(caller.hospitalId(), drugName);
    }

    @PostMapping("/drug-rules/{id}/deactivate")
    @PreAuthorize("hasRole('DOCTOR')")
    public DrugRuleResponse deactivate(@AuthenticationPrincipal AuthUser caller, @PathVariable UUID id) {
        return drugRuleService.deactivate(caller.hospitalId(), id);
    }

    @GetMapping("/patients/{patientId}/dose-suggestions")
    @PreAuthorize("hasRole('DOCTOR')")
    public DoseSuggestionResponse suggest(@AuthenticationPrincipal AuthUser caller,
                                          @PathVariable UUID patientId,
                                          @RequestParam String drugName) {
        return drugRuleService.suggest(caller.hospitalId(), patientId, drugName);
    }
}

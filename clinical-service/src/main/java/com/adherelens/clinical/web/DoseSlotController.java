package com.adherelens.clinical.web;

import com.adherelens.auth.AuthUser;
import com.adherelens.clinical.domain.SlotStatus;
import com.adherelens.clinical.dto.DoseSlotResponse;
import com.adherelens.clinical.dto.RecordDoseRequest;
import com.adherelens.clinical.service.DoseSlotService;
import com.adherelens.common.model.CareSetting;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/dose-slots")
public class DoseSlotController {

    private final DoseSlotService doseSlotService;

    public DoseSlotController(DoseSlotService doseSlotService) {
        this.doseSlotService = doseSlotService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN', 'DOCTOR', 'NURSE')")
    public List<DoseSlotResponse> list(@AuthenticationPrincipal AuthUser caller,
                                       @RequestParam(defaultValue = "PENDING") SlotStatus status,
                                       @RequestParam(required = false) CareSetting careSetting) {
        return doseSlotService.list(caller.hospitalId(), status, careSetting);
    }

    @PostMapping("/{id}/record")
    @PreAuthorize("hasAnyRole('NURSE', 'DOCTOR')")
    public DoseSlotResponse record(@AuthenticationPrincipal AuthUser caller,
                                   @PathVariable UUID id,
                                   @Valid @RequestBody RecordDoseRequest request) {
        return doseSlotService.record(caller, id, request.outcome());
    }
}

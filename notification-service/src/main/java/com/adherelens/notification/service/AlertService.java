package com.adherelens.notification.service;

import com.adherelens.notification.domain.AlertStatus;
import com.adherelens.notification.dto.AlertResponse;
import com.adherelens.notification.repository.NurseAlertRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AlertService {

    private final NurseAlertRepository alertRepository;

    public AlertService(NurseAlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    /** Always scoped by the caller's hospitalId from the token. */
    @Transactional(readOnly = true)
    public List<AlertResponse> list(UUID hospitalId, AlertStatus status) {
        return alertRepository.findTop200ByHospitalIdAndStatusOrderByDueAtAsc(hospitalId, status).stream()
                .map(a -> new AlertResponse(a.getId(), a.getDoseSlotId(), a.getPatientId(), a.getRegimenId(),
                        a.getDueAt(), a.getWindowClosesAt(), a.getStatus(), a.getResolvedStatus()))
                .toList();
    }
}

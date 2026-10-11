package com.adherelens.clinical.service;

import com.adherelens.auth.AuthUser;
import com.adherelens.clinical.domain.Regimen;
import com.adherelens.clinical.domain.SideEffectReport;
import com.adherelens.clinical.domain.SideEffectStatus;
import com.adherelens.clinical.dto.SideEffectReportRequest;
import com.adherelens.clinical.dto.SideEffectResponse;
import com.adherelens.clinical.repository.PatientRepository;
import com.adherelens.clinical.repository.RegimenRepository;
import com.adherelens.clinical.repository.SideEffectReportRepository;
import com.adherelens.common.model.SideEffect;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Only doctors and nurses report side effects (enforced in the controller and security
 * config), and every row records who. All queries are scoped by the caller's hospitalId.
 */
@Service
public class SideEffectService {

    private final SideEffectReportRepository reportRepository;
    private final RegimenRepository regimenRepository;
    private final PatientRepository patientRepository;
    private final Clock clock;

    public SideEffectService(SideEffectReportRepository reportRepository,
                             RegimenRepository regimenRepository,
                             PatientRepository patientRepository,
                             Clock clock) {
        this.reportRepository = reportRepository;
        this.regimenRepository = regimenRepository;
        this.patientRepository = patientRepository;
        this.clock = clock;
    }

    @Transactional
    public List<SideEffectResponse> report(AuthUser caller, UUID regimenId, SideEffectReportRequest request) {
        Regimen regimen = regimenRepository.findByIdAndHospitalId(regimenId, caller.hospitalId())
                .orElseThrow(() -> new NotFoundException("Regimen not found"));
        Set<SideEffect> distinct = new LinkedHashSet<>(request.sideEffects());
        if (distinct.size() != request.sideEffects().size()) {
            throw new IllegalArgumentException("Side effects must be distinct");
        }
        Instant now = Instant.now(clock);
        List<SideEffectResponse> created = new ArrayList<>();
        for (SideEffect effect : distinct) {
            SideEffectReport report = new SideEffectReport();
            report.setHospitalId(caller.hospitalId());
            report.setPatientId(regimen.getPatientId());
            report.setRegimenId(regimen.getId());
            report.setSideEffect(effect);
            report.setReportedBy(caller.staffId());
            report.setReportedByName(caller.fullName());
            report.setReportedByRole(caller.role().name());
            report.setReportedAt(now);
            created.add(toResponse(reportRepository.save(report)));
        }
        return created;
    }

    @Transactional(readOnly = true)
    public List<SideEffectResponse> listForPatient(UUID hospitalId, UUID patientId) {
        patientRepository.findByIdAndHospitalId(patientId, hospitalId)
                .orElseThrow(() -> new NotFoundException("Patient not found"));
        return reportRepository.findByPatientIdAndHospitalIdOrderByReportedAtDesc(patientId, hospitalId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SideEffectResponse retract(AuthUser caller, UUID reportId, String reason) {
        SideEffectReport report = reportRepository.findByIdAndHospitalId(reportId, caller.hospitalId())
                .orElseThrow(() -> new NotFoundException("Side effect report not found"));
        if (report.getStatus() == SideEffectStatus.RETRACTED) {
            throw new IllegalArgumentException("Already retracted");
        }
        report.setStatus(SideEffectStatus.RETRACTED);
        report.setRetractedBy(caller.staffId());
        report.setRetractedByName(caller.fullName());
        report.setRetractedAt(Instant.now(clock));
        report.setRetractReason(reason.trim());
        return toResponse(reportRepository.save(report));
    }

    private SideEffectResponse toResponse(SideEffectReport r) {
        return new SideEffectResponse(r.getId(), r.getPatientId(), r.getRegimenId(), r.getSideEffect(),
                r.getReportedBy(), r.getReportedByName(), r.getReportedByRole(), r.getReportedAt(),
                r.getStatus(), r.getRetractedAt(), r.getRetractReason());
    }
}

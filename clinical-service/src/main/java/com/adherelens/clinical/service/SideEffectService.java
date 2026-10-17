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
import com.adherelens.common.event.SideEffectEvent;
import com.adherelens.common.event.Topics;
import com.adherelens.common.model.SideEffect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

    private static final Logger log = LoggerFactory.getLogger(SideEffectService.class);

    private final SideEffectReportRepository reportRepository;
    private final RegimenRepository regimenRepository;
    private final PatientRepository patientRepository;
    private final Clock clock;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public SideEffectService(SideEffectReportRepository reportRepository,
                             RegimenRepository regimenRepository,
                             PatientRepository patientRepository,
                             Clock clock,
                             KafkaTemplate<String, Object> kafkaTemplate) {
        this.reportRepository = reportRepository;
        this.regimenRepository = regimenRepository;
        this.patientRepository = patientRepository;
        this.clock = clock;
        this.kafkaTemplate = kafkaTemplate;
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
            SideEffectReport saved = reportRepository.save(report);
            publishAfterCommit(saved);
            created.add(toResponse(saved));
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
        SideEffectReport saved = reportRepository.save(report);
        publishAfterCommit(saved);
        return toResponse(saved);
    }

    /**
     * Sends the event only once the database transaction has committed, so analytics never hears
     * about a report that was rolled back. If the app crashes between the commit and the send,
     * that one event is lost.
     */
    private void publishAfterCommit(SideEffectReport report) {
        SideEffectEvent event = new SideEffectEvent(report.getId(), report.getHospitalId(), report.getPatientId(),
                report.getRegimenId(), report.getSideEffect(), report.getStatus() == SideEffectStatus.RETRACTED,
                report.getReportedAt());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kafkaTemplate.send(Topics.SIDE_EFFECT, report.getPatientId().toString(), event)
                        .whenComplete((result, error) -> {
                            if (error != null) {
                                log.error("Publishing side effect {} failed", report.getId(), error);
                            }
                        });
            }
        });
    }

    private SideEffectResponse toResponse(SideEffectReport r) {
        return new SideEffectResponse(r.getId(), r.getPatientId(), r.getRegimenId(), r.getSideEffect(),
                r.getReportedBy(), r.getReportedByName(), r.getReportedByRole(), r.getReportedAt(),
                r.getStatus(), r.getRetractedAt(), r.getRetractReason());
    }
}

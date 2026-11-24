package com.adherelens.clinical.service;

import com.adherelens.clinical.domain.DoseSlot;
import com.adherelens.clinical.domain.Regimen;
import com.adherelens.clinical.domain.SlotStatus;
import com.adherelens.clinical.dto.PatientDoseView;
import com.adherelens.clinical.dto.RecordOutcome;
import com.adherelens.clinical.repository.DoseSlotRepository;
import com.adherelens.clinical.repository.RegimenRepository;
import com.adherelens.common.model.CareSetting;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Outpatient self-confirmation through a signed link. There is no login: the hospital and the
 * dose come from the verified token, never from the request. Only OUTPATIENT slots can be
 * confirmed this way, because inpatient doses are recorded by nurses.
 */
@Service
public class PatientDoseService {

    private final DoseLinkSigner signer;
    private final DoseSlotRepository slotRepository;
    private final RegimenRepository regimenRepository;
    private final DoseSlotService doseSlotService;
    private final Clock clock;

    public PatientDoseService(DoseLinkSigner signer,
                              DoseSlotRepository slotRepository,
                              RegimenRepository regimenRepository,
                              DoseSlotService doseSlotService,
                              Clock clock) {
        this.signer = signer;
        this.slotRepository = slotRepository;
        this.regimenRepository = regimenRepository;
        this.doseSlotService = doseSlotService;
        this.clock = clock;
    }

    /** Called by notification-service at send time. Only an open outpatient dose gets a link. */
    @Transactional(readOnly = true)
    public String issueToken(UUID hospitalId, UUID doseSlotId) {
        DoseSlot slot = slotRepository.findByIdAndHospitalId(doseSlotId, hospitalId)
                .filter(s -> s.getCareSetting() == CareSetting.OUTPATIENT)
                .filter(s -> s.getStatus() == SlotStatus.PENDING)
                .filter(s -> Instant.now(clock).isBefore(s.getWindowClosesAt()))
                .orElseThrow(() -> new NotFoundException("Dose not found"));
        return signer.sign(slot.getId(), slot.getWindowClosesAt());
    }

    @Transactional(readOnly = true)
    public PatientDoseView view(String token) {
        return toView(loadOutpatientSlot(token));
    }

    @Transactional
    public PatientDoseView record(String token, RecordOutcome outcome) {
        UUID slotId = signer.verify(token);
        doseSlotService.recordByPatient(slotId, outcome);
        return toView(loadOutpatientSlot(token));
    }

    private DoseSlot loadOutpatientSlot(String token) {
        UUID slotId = signer.verify(token);
        return slotRepository.findById(slotId)
                .filter(s -> s.getCareSetting() == CareSetting.OUTPATIENT)
                .orElseThrow(InvalidDoseLinkException::new);
    }

    private PatientDoseView toView(DoseSlot slot) {
        Regimen r = regimenRepository.findById(slot.getRegimenId()).orElseThrow(InvalidDoseLinkException::new);
        Instant now = Instant.now(clock);
        boolean canRecord = slot.getStatus() == SlotStatus.PENDING
                && !now.isBefore(slot.getDueAt()) && now.isBefore(slot.getWindowClosesAt());
        return new PatientDoseView(r.getDrugName(), r.getFormulation(), r.getDoseAmount(), r.getDoseUnit(),
                slot.getDueAt(), slot.getWindowClosesAt(), slot.getStatus(), canRecord);
    }
}

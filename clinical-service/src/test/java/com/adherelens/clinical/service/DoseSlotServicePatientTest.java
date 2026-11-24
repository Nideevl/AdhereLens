package com.adherelens.clinical.service;

import com.adherelens.clinical.domain.DoseSlot;
import com.adherelens.clinical.domain.SlotStatus;
import com.adherelens.clinical.dto.RecordOutcome;
import com.adherelens.clinical.repository.DoseSlotRepository;
import com.adherelens.clinical.repository.PatientRepository;
import com.adherelens.clinical.repository.RegimenRepository;
import com.adherelens.common.model.CareSetting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Patient self-confirmation uses the same timing rules as a nurse (due 08:00, closes 09:00, on-time 15 min). */
@ExtendWith(MockitoExtension.class)
class DoseSlotServicePatientTest {

    private static final Instant DUE = Instant.parse("2026-11-19T02:30:00Z");
    private static final Instant CLOSES = DUE.plusSeconds(3600);

    private final UUID slotId = UUID.randomUUID();

    @Mock DoseSlotRepository slotRepository;
    @Mock PatientRepository patientRepository;
    @Mock RegimenRepository regimenRepository;

    private DoseSlot slot;

    @BeforeEach
    void setUp() {
        slot = new DoseSlot();
        slot.setHospitalId(UUID.randomUUID());
        slot.setPatientId(UUID.randomUUID());
        slot.setRegimenId(UUID.randomUUID());
        slot.setCareSetting(CareSetting.OUTPATIENT);
        slot.setDueAt(DUE);
        slot.setWindowClosesAt(CLOSES);
        slot.setStatus(SlotStatus.PENDING);
    }

    private DoseSlotService serviceAt(Instant now) {
        return new DoseSlotService(slotRepository, patientRepository, regimenRepository,
                Clock.fixed(now, ZoneOffset.UTC), 15);
    }

    @Test
    void onTimeConfirmation_isHit_recordedAsPatientWithNoStaffId() {
        when(slotRepository.findById(slotId)).thenReturn(Optional.of(slot));
        when(slotRepository.save(any(DoseSlot.class))).thenAnswer(i -> i.getArgument(0));

        serviceAt(DUE.plusSeconds(600)).recordByPatient(slotId, RecordOutcome.ADMINISTERED);

        ArgumentCaptor<DoseSlot> saved = ArgumentCaptor.forClass(DoseSlot.class);
        verify(slotRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(SlotStatus.HIT);
        assertThat(saved.getValue().getRecordedBy()).isNull();
        assertThat(saved.getValue().getReportedByRole()).isEqualTo("PATIENT");
    }

    @Test
    void lateConfirmation_isDelayed() {
        when(slotRepository.findById(slotId)).thenReturn(Optional.of(slot));
        when(slotRepository.save(any(DoseSlot.class))).thenAnswer(i -> i.getArgument(0));

        serviceAt(DUE.plusSeconds(16 * 60)).recordByPatient(slotId, RecordOutcome.ADMINISTERED);

        ArgumentCaptor<DoseSlot> saved = ArgumentCaptor.forClass(DoseSlot.class);
        verify(slotRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(SlotStatus.DELAYED);
    }

    @Test
    void refusal_isRefused() {
        when(slotRepository.findById(slotId)).thenReturn(Optional.of(slot));
        when(slotRepository.save(any(DoseSlot.class))).thenAnswer(i -> i.getArgument(0));

        serviceAt(DUE.plusSeconds(1200)).recordByPatient(slotId, RecordOutcome.REFUSED);

        ArgumentCaptor<DoseSlot> saved = ArgumentCaptor.forClass(DoseSlot.class);
        verify(slotRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(SlotStatus.REFUSED);
    }

    @Test
    void secondConfirmation_isRejected_soTheLinkIsEffectivelySingleUse() {
        slot.setStatus(SlotStatus.HIT);
        when(slotRepository.findById(slotId)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> serviceAt(DUE.plusSeconds(600)).recordByPatient(slotId, RecordOutcome.ADMINISTERED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already has an outcome");
        verify(slotRepository, never()).save(any());
    }

    @Test
    void afterWindowCloses_isRejected() {
        when(slotRepository.findById(slotId)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> serviceAt(CLOSES).recordByPatient(slotId, RecordOutcome.ADMINISTERED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("window");
        verify(slotRepository, never()).save(any());
    }

    @Test
    void beforeDueTime_isRejected() {
        when(slotRepository.findById(slotId)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> serviceAt(DUE.minusSeconds(60)).recordByPatient(slotId, RecordOutcome.ADMINISTERED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not due yet");
    }

    @Test
    void inpatientDose_cannotBeConfirmedByPatient() {
        slot.setCareSetting(CareSetting.INPATIENT);
        when(slotRepository.findById(slotId)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> serviceAt(DUE.plusSeconds(600)).recordByPatient(slotId, RecordOutcome.ADMINISTERED))
                .isInstanceOf(NotFoundException.class);
        verify(slotRepository, never()).save(any());
    }
}

package com.adherelens.clinical.service;

import com.adherelens.clinical.domain.DoseSlot;
import com.adherelens.clinical.domain.SlotStatus;
import com.adherelens.clinical.repository.DoseSlotRepository;
import com.adherelens.clinical.repository.PatientRepository;
import com.adherelens.clinical.repository.RegimenRepository;
import com.adherelens.common.event.Topics;
import com.adherelens.common.model.CareSetting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Covers the "lock, re-check, act" behaviour that makes the scheduler safe with several instances. */
@ExtendWith(MockitoExtension.class)
class DoseSlotProcessorTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    private static final LocalDate DAY = LocalDate.of(2026, 11, 19);

    @Mock DoseSlotRepository slotRepository;
    @Mock RegimenRepository regimenRepository;
    @Mock PatientRepository patientRepository;
    @Mock KafkaTemplate<String, Object> kafkaTemplate;

    private final UUID slotId = UUID.randomUUID();
    private DoseSlot slot;

    @BeforeEach
    void setUp() {
        slot = new DoseSlot();
        slot.setHospitalId(UUID.randomUUID());
        slot.setPatientId(UUID.randomUUID());
        slot.setRegimenId(UUID.randomUUID());
        slot.setCareSetting(CareSetting.INPATIENT);
        slot.setDueAt(at("08:00:00"));
        slot.setWindowClosesAt(at("09:00:00"));
        slot.setStatus(SlotStatus.PENDING);
    }

    // ---- row locked by another instance ----

    @Test
    void publishDue_slotLockedByAnotherInstance_doesNothing() {
        when(slotRepository.findByIdForUpdateSkipLocked(slotId)).thenReturn(Optional.empty());

        processorAt("08:05:00").publishDue(slotId);

        verifyNoInteractions(kafkaTemplate);
        verify(slotRepository, never()).save(any());
    }

    @Test
    void markMissed_slotLockedByAnotherInstance_doesNothing() {
        when(slotRepository.findByIdForUpdateSkipLocked(slotId)).thenReturn(Optional.empty());

        processorAt("09:30:00").markMissedIfExpired(slotId);

        verify(slotRepository, never()).save(any());
    }

    @Test
    void publishOutcome_slotLockedByAnotherInstance_doesNothing() {
        when(slotRepository.findByIdForUpdateSkipLocked(slotId)).thenReturn(Optional.empty());

        processorAt("09:30:00").publishOutcome(slotId);

        verifyNoInteractions(kafkaTemplate);
        verify(slotRepository, never()).save(any());
    }

    @Test
    void createSlots_regimenLockedByAnotherInstance_doesNothing() {
        UUID regimenId = UUID.randomUUID();
        when(regimenRepository.findByIdForUpdateSkipLocked(regimenId)).thenReturn(Optional.empty());

        processorAt("08:05:00").createSlotsIfMissing(regimenId);

        verifyNoInteractions(slotRepository, patientRepository);
    }

    // ---- row already handled by the time we got the lock ----

    @Test
    void publishDue_alreadyPublished_doesNotSendAgain() {
        slot.setDueEventPublished(true);
        when(slotRepository.findByIdForUpdateSkipLocked(slotId)).thenReturn(Optional.of(slot));

        processorAt("08:05:00").publishDue(slotId);

        verifyNoInteractions(kafkaTemplate);
        verify(slotRepository, never()).save(any());
    }

    @Test
    void publishOutcome_alreadyPublished_doesNotSendAgain() {
        slot.setStatus(SlotStatus.HIT);
        slot.setOutcomePublished(true);
        when(slotRepository.findByIdForUpdateSkipLocked(slotId)).thenReturn(Optional.of(slot));

        processorAt("09:30:00").publishOutcome(slotId);

        verifyNoInteractions(kafkaTemplate);
        verify(slotRepository, never()).save(any());
    }

    @Test
    void markMissed_slotAlreadyRecordedByNurse_staysAsRecorded() {
        slot.setStatus(SlotStatus.HIT);
        when(slotRepository.findByIdForUpdateSkipLocked(slotId)).thenReturn(Optional.of(slot));

        processorAt("09:30:00").markMissedIfExpired(slotId);

        assertThat(slot.getStatus()).isEqualTo(SlotStatus.HIT);
        verify(slotRepository, never()).save(any());
    }

    // ---- normal path ----

    @Test
    void publishDue_lockAcquired_sendsEventAndMarksPublished() {
        when(slotRepository.findByIdForUpdateSkipLocked(slotId)).thenReturn(Optional.of(slot));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));

        processorAt("08:05:00").publishDue(slotId);

        verify(kafkaTemplate).send(eq(Topics.DOSE_DUE), eq(slot.getPatientId().toString()), any());
        assertThat(slot.isDueEventPublished()).isTrue();
        verify(slotRepository).save(slot);
    }

    @Test
    void markMissed_windowClosedAndStillPending_becomesMissed() {
        when(slotRepository.findByIdForUpdateSkipLocked(slotId)).thenReturn(Optional.of(slot));

        processorAt("09:00:00").markMissedIfExpired(slotId);

        assertThat(slot.getStatus()).isEqualTo(SlotStatus.MISSED);
        verify(slotRepository).save(slot);
    }

    @Test
    void markMissed_windowStillOpen_staysPending() {
        when(slotRepository.findByIdForUpdateSkipLocked(slotId)).thenReturn(Optional.of(slot));

        processorAt("08:59:59").markMissedIfExpired(slotId);

        assertThat(slot.getStatus()).isEqualTo(SlotStatus.PENDING);
        verify(slotRepository, never()).save(any());
    }

    // ---- helpers ----

    private DoseSlotProcessor processorAt(String time) {
        return new DoseSlotProcessor(slotRepository, regimenRepository, patientRepository,
                kafkaTemplate, Clock.fixed(at(time), ZONE));
    }

    private static Instant at(String time) {
        return ZonedDateTime.of(DAY, LocalTime.parse(time), ZONE).toInstant();
    }
}

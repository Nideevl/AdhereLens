package com.adherelens.clinical.service;

import com.adherelens.clinical.domain.DoseSlot;
import com.adherelens.clinical.domain.Patient;
import com.adherelens.clinical.domain.Regimen;
import com.adherelens.clinical.domain.RegimenStatus;
import com.adherelens.clinical.domain.SlotStatus;
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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Slot creation. Reference regimen: doses at 08:00 and 20:00 IST, 60-minute grace window.
 * A slot is created only while its window is still open, so an outage never turns into false MISSED doses.
 */
@ExtendWith(MockitoExtension.class)
class DoseSlotCreationTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    private static final LocalDate DAY = LocalDate.of(2026, 11, 19);

    @Mock DoseSlotRepository slotRepository;
    @Mock RegimenRepository regimenRepository;
    @Mock PatientRepository patientRepository;
    @Mock KafkaTemplate<String, Object> kafkaTemplate;

    private final UUID regimenId = UUID.randomUUID();
    private Regimen regimen;
    private Patient patient;

    @BeforeEach
    void setUp() {
        regimen = new Regimen();
        ReflectionTestUtils.setField(regimen, "id", regimenId);
        regimen.setHospitalId(UUID.randomUUID());
        regimen.setPatientId(UUID.randomUUID());
        regimen.setStatus(RegimenStatus.ACTIVE);
        regimen.setDoseTimes(new TreeSet<>(Set.of(LocalTime.of(8, 0), LocalTime.of(20, 0))));
        regimen.setStartDate(DAY.minusDays(3));
        regimen.setGraceWindowMinutes(60);
        regimen.setScheduleEffectiveFrom(at("00:00:00").minusSeconds(24 * 3600));

        patient = new Patient();
        ReflectionTestUtils.setField(patient, "id", regimen.getPatientId());
        patient.setCareSetting(CareSetting.INPATIENT);
        patient.setActive(true);
    }

    private void regimenAndPatientFound() {
        when(regimenRepository.findByIdForUpdateSkipLocked(regimenId)).thenReturn(Optional.of(regimen));
        when(patientRepository.findById(regimen.getPatientId())).thenReturn(Optional.of(patient));
    }

    private DoseSlotProcessor processorAt(String time) {
        return new DoseSlotProcessor(slotRepository, regimenRepository, patientRepository,
                kafkaTemplate, Clock.fixed(at(time), ZONE));
    }

    private static Instant at(String time) {
        return ZonedDateTime.of(DAY, LocalTime.parse(time), ZONE).toInstant();
    }

    private java.util.List<DoseSlot> savedSlots(int expected) {
        ArgumentCaptor<DoseSlot> captor = ArgumentCaptor.forClass(DoseSlot.class);
        verify(slotRepository, times(expected)).save(captor.capture());
        return captor.getAllValues();
    }

    @Test
    void createsOneSlotPerDoseTimeAtStartOfDay() {
        regimenAndPatientFound();
        when(slotRepository.existsByRegimenIdAndDueAt(eq(regimenId), any())).thenReturn(false);

        processorAt("00:30:00").createSlotsIfMissing(regimenId);

        var slots = savedSlots(2);
        DoseSlot morning = slots.get(0);
        assertThat(morning.getDueAt()).isEqualTo(at("08:00:00"));
        assertThat(morning.getWindowClosesAt()).isEqualTo(at("09:00:00"));
        assertThat(morning.getStatus()).isEqualTo(SlotStatus.PENDING);
        assertThat(morning.getCareSetting()).isEqualTo(CareSetting.INPATIENT);
        assertThat(slots.get(1).getDueAt()).isEqualTo(at("20:00:00"));
    }

    @Test
    void doesNotCreateASlotThatAlreadyExists() {
        regimenAndPatientFound();
        when(slotRepository.existsByRegimenIdAndDueAt(eq(regimenId), any())).thenReturn(true);

        processorAt("00:30:00").createSlotsIfMissing(regimenId);

        verify(slotRepository, never()).save(any());
    }

    @Test
    void lateButStillInsideTheWindowIsCreated() {
        regimenAndPatientFound();
        when(slotRepository.existsByRegimenIdAndDueAt(eq(regimenId), any())).thenReturn(false);

        processorAt("08:30:00").createSlotsIfMissing(regimenId); // scheduler came back 30 min late

        var slots = savedSlots(2);
        assertThat(slots.get(0).getDueAt()).isEqualTo(at("08:00:00")); // still recordable until 09:00
    }

    @Test
    void slotWhoseWindowAlreadyClosedIsNotCreated() {
        regimenAndPatientFound();
        when(slotRepository.existsByRegimenIdAndDueAt(eq(regimenId), any())).thenReturn(false);

        processorAt("09:00:00").createSlotsIfMissing(regimenId); // 08:00 window closes exactly now

        var slots = savedSlots(1);
        assertThat(slots.get(0).getDueAt()).isEqualTo(at("20:00:00")); // only the evening dose
    }

    @Test
    void skipsDoseTimeThatPassedBeforeTheScheduleTookEffect() {
        regimen.setScheduleEffectiveFrom(at("09:00:00"));
        regimenAndPatientFound();
        when(slotRepository.existsByRegimenIdAndDueAt(eq(regimenId), any())).thenReturn(false);

        processorAt("09:30:00").createSlotsIfMissing(regimenId);

        var slots = savedSlots(1);
        assertThat(slots.get(0).getDueAt()).isEqualTo(at("20:00:00"));
    }

    @Test
    void createsNothingForEndedRegimenOrInactivePatient() {
        regimen.setEndDate(DAY.minusDays(1));
        regimenAndPatientFound();
        processorAt("00:30:00").createSlotsIfMissing(regimenId);

        regimen.setEndDate(null);
        patient.setActive(false);
        processorAt("00:30:00").createSlotsIfMissing(regimenId);

        verify(slotRepository, never()).save(any());
    }
}

package com.adherelens.clinical.service;

import com.adherelens.auth.AuthUser;
import com.adherelens.clinical.domain.DoseSlot;
import com.adherelens.clinical.domain.Patient;
import com.adherelens.clinical.domain.Regimen;
import com.adherelens.clinical.domain.SlotStatus;
import com.adherelens.clinical.dto.DoseSlotResponse;
import com.adherelens.clinical.dto.RecordOutcome;
import com.adherelens.clinical.repository.DoseSlotRepository;
import com.adherelens.clinical.repository.PatientRepository;
import com.adherelens.clinical.repository.RegimenRepository;
import com.adherelens.common.model.CareSetting;
import com.adherelens.common.model.DoseUnit;
import com.adherelens.common.model.Formulation;
import com.adherelens.common.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Worked example used throughout: a dose due at 08:00 with a 60-minute grace window
 * (closes 09:00:00) and the configured 15-minute on-time limit.
 *   08:15:00 -> HIT        08:15:01 -> DELAYED
 *   09:00:00 -> rejected   07:55:00 -> not due yet
 */
@ExtendWith(MockitoExtension.class)
class DoseSlotServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    private static final LocalDate DAY = LocalDate.of(2026, 11, 19);
    private static final long ON_TIME_MINUTES = 15;

    private final UUID hospitalId = UUID.randomUUID();
    private final UUID slotId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final UUID regimenId = UUID.randomUUID();
    private final AuthUser nurse = new AuthUser(UUID.randomUUID(), hospitalId, Role.NURSE, "Nurse One");

    @Mock DoseSlotRepository slotRepository;
    @Mock PatientRepository patientRepository;
    @Mock RegimenRepository regimenRepository;

    private DoseSlot slot;

    @BeforeEach
    void setUp() {
        slot = new DoseSlot();
        slot.setHospitalId(hospitalId);
        slot.setPatientId(patientId);
        slot.setRegimenId(regimenId);
        slot.setCareSetting(CareSetting.INPATIENT);
        slot.setDueAt(at("08:00:00"));
        slot.setWindowClosesAt(at("09:00:00"));
        slot.setStatus(SlotStatus.PENDING);
    }

    // ---- outcome by time: the grace-window rules ----

    @Test
    void administeredExactlyAtDueTime_isHit() {
        stubFoundAndSaved();
        assertThat(recordAt("08:00:00", RecordOutcome.ADMINISTERED).status()).isEqualTo(SlotStatus.HIT);
    }

    @Test
    void administeredExactlyAtOnTimeLimit_isHit() {
        stubFoundAndSaved();
        assertThat(recordAt("08:15:00", RecordOutcome.ADMINISTERED).status()).isEqualTo(SlotStatus.HIT);
    }

    @Test
    void administeredOneSecondAfterOnTimeLimit_isDelayed() {
        stubFoundAndSaved();
        assertThat(recordAt("08:15:01", RecordOutcome.ADMINISTERED).status()).isEqualTo(SlotStatus.DELAYED);
    }

    @Test
    void administeredOneSecondBeforeWindowCloses_isDelayed() {
        stubFoundAndSaved();
        assertThat(recordAt("08:59:59", RecordOutcome.ADMINISTERED).status()).isEqualTo(SlotStatus.DELAYED);
    }

    @Test
    void recordingExactlyWhenWindowCloses_isRejected() {
        when(slotRepository.findByIdAndHospitalId(slotId, hospitalId)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> recordAt("09:00:00", RecordOutcome.ADMINISTERED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("window");
        verify(slotRepository, never()).save(any());
    }

    @Test
    void recordingBeforeDueTime_isRejected() {
        when(slotRepository.findByIdAndHospitalId(slotId, hospitalId)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> recordAt("07:55:00", RecordOutcome.ADMINISTERED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not due yet");
        verify(slotRepository, never()).save(any());
    }

    // ---- refusals ----

    @Test
    void refusedInsideWindow_isRefusedEvenWhenLate() {
        stubFoundAndSaved();
        assertThat(recordAt("08:40:00", RecordOutcome.REFUSED).status()).isEqualTo(SlotStatus.REFUSED);
    }

    @Test
    void refusalBeforeDueTime_isRejected() {
        when(slotRepository.findByIdAndHospitalId(slotId, hospitalId)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> recordAt("07:55:00", RecordOutcome.REFUSED))
                .isInstanceOf(IllegalArgumentException.class);
        verify(slotRepository, never()).save(any());
    }

    // ---- state machine: only PENDING can move ----

    @ParameterizedTest
    @EnumSource(value = SlotStatus.class, names = "PENDING", mode = EnumSource.Mode.EXCLUDE)
    void slotThatAlreadyHasAnOutcome_cannotBeRecordedAgain(SlotStatus existing) {
        slot.setStatus(existing);
        when(slotRepository.findByIdAndHospitalId(slotId, hospitalId)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> recordAt("08:10:00", RecordOutcome.ADMINISTERED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already has an outcome");
        assertThat(slot.getStatus()).isEqualTo(existing);
        verify(slotRepository, never()).save(any());
    }

    // ---- tenant isolation ----

    @Test
    void slotFromAnotherHospital_isNotFound() {
        // The lookup is scoped by the caller's hospital, so a slot of another hospital is simply absent.
        when(slotRepository.findByIdAndHospitalId(slotId, hospitalId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recordAt("08:10:00", RecordOutcome.ADMINISTERED))
                .isInstanceOf(NotFoundException.class);
        verify(slotRepository, never()).save(any());
    }

    // ---- audit fields ----

    @Test
    void successfulRecord_storesWhoWhenAndRole() {
        stubFoundAndSaved();
        recordAt("08:10:00", RecordOutcome.ADMINISTERED);

        assertThat(slot.getRespondedAt()).isEqualTo(at("08:10:00"));
        assertThat(slot.getRecordedBy()).isEqualTo(nurse.staffId());
        assertThat(slot.getReportedByRole()).isEqualTo("NURSE");
    }

    // ---- optimistic locking ----

    @Test
    void concurrentUpdate_surfacesOptimisticLockFailure() {
        when(slotRepository.findByIdAndHospitalId(slotId, hospitalId)).thenReturn(Optional.of(slot));
        when(slotRepository.save(any(DoseSlot.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(DoseSlot.class, slotId));

        // The service must not swallow this: the web layer maps it to HTTP 409.
        assertThatThrownBy(() -> recordAt("08:10:00", RecordOutcome.ADMINISTERED))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    // ---- helpers ----

    private DoseSlotResponse recordAt(String time, RecordOutcome outcome) {
        Clock clock = Clock.fixed(at(time), ZONE);
        DoseSlotService service = new DoseSlotService(
                slotRepository, patientRepository, regimenRepository, clock, ON_TIME_MINUTES);
        return service.record(nurse, slotId, outcome);
    }

    private static Instant at(String time) {
        return ZonedDateTime.of(DAY, LocalTime.parse(time), ZONE).toInstant();
    }

    /** Stubs for the happy path, including the lookups used to build the response. */
    private void stubFoundAndSaved() {
        when(slotRepository.findByIdAndHospitalId(slotId, hospitalId)).thenReturn(Optional.of(slot));
        when(slotRepository.save(any(DoseSlot.class))).thenAnswer(inv -> inv.getArgument(0));

        Patient patient = new Patient();
        ReflectionTestUtils.setField(patient, "id", patientId);
        patient.setFullName("Test Patient");
        when(patientRepository.findAllById(any())).thenReturn(List.of(patient));

        Regimen regimen = new Regimen();
        ReflectionTestUtils.setField(regimen, "id", regimenId);
        regimen.setDrugName("Paracetamol");
        regimen.setFormulation(Formulation.TABLET);
        regimen.setDoseAmount(new BigDecimal("500"));
        regimen.setDoseUnit(DoseUnit.MG);
        when(regimenRepository.findAllById(any())).thenReturn(List.of(regimen));
    }
}

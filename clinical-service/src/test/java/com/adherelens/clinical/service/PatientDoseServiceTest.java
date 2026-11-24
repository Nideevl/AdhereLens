package com.adherelens.clinical.service;

import com.adherelens.clinical.domain.DoseSlot;
import com.adherelens.clinical.domain.Regimen;
import com.adherelens.clinical.domain.SlotStatus;
import com.adherelens.clinical.dto.PatientDoseView;
import com.adherelens.clinical.dto.RecordOutcome;
import com.adherelens.clinical.repository.DoseSlotRepository;
import com.adherelens.clinical.repository.RegimenRepository;
import com.adherelens.common.model.CareSetting;
import com.adherelens.common.model.DoseUnit;
import com.adherelens.common.model.Formulation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientDoseServiceTest {

    private static final Instant DUE = Instant.parse("2026-11-19T02:30:00Z");
    private static final Instant CLOSES = DUE.plusSeconds(3600);
    private static final String SECRET = "a-test-secret-that-is-at-least-32-chars";

    private final UUID hospitalId = UUID.randomUUID();
    private final UUID slotId = UUID.randomUUID();
    private final UUID regimenId = UUID.randomUUID();

    @Mock DoseSlotRepository slotRepository;
    @Mock RegimenRepository regimenRepository;
    @Mock DoseSlotService doseSlotService;

    private DoseSlot slot;
    private Regimen regimen;

    @BeforeEach
    void setUp() {
        slot = new DoseSlot();
        ReflectionTestUtils.setField(slot, "id", slotId);
        slot.setHospitalId(hospitalId);
        slot.setRegimenId(regimenId);
        slot.setCareSetting(CareSetting.OUTPATIENT);
        slot.setDueAt(DUE);
        slot.setWindowClosesAt(CLOSES);
        slot.setStatus(SlotStatus.PENDING);

        regimen = new Regimen();
        regimen.setDrugName("DEMO_DRUG_A");
        regimen.setFormulation(Formulation.TABLET);
        regimen.setDoseAmount(new BigDecimal("500"));
        regimen.setDoseUnit(DoseUnit.MG);
    }

    private PatientDoseService serviceAt(Instant now) {
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        return new PatientDoseService(new DoseLinkSigner(SECRET, clock), slotRepository, regimenRepository,
                doseSlotService, clock);
    }

    // ---- issuing a link ----

    @Test
    void openOutpatientDose_getsATokenThatExpiresWhenTheWindowCloses() {
        when(slotRepository.findByIdAndHospitalId(slotId, hospitalId)).thenReturn(Optional.of(slot));
        PatientDoseService service = serviceAt(DUE);

        String token = service.issueToken(hospitalId, slotId);

        assertThat(token).startsWith(slotId + "." + CLOSES.getEpochSecond() + ".");
    }

    @Test
    void inpatientDose_getsNoLink() {
        slot.setCareSetting(CareSetting.INPATIENT);
        when(slotRepository.findByIdAndHospitalId(slotId, hospitalId)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> serviceAt(DUE).issueToken(hospitalId, slotId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void alreadyFinishedDose_getsNoLink() {
        slot.setStatus(SlotStatus.HIT);
        when(slotRepository.findByIdAndHospitalId(slotId, hospitalId)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> serviceAt(DUE).issueToken(hospitalId, slotId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void closedWindow_getsNoLink() {
        when(slotRepository.findByIdAndHospitalId(slotId, hospitalId)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> serviceAt(CLOSES).issueToken(hospitalId, slotId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void otherHospitalsDose_getsNoLink() {
        UUID otherHospital = UUID.randomUUID();
        when(slotRepository.findByIdAndHospitalId(slotId, otherHospital)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceAt(DUE).issueToken(otherHospital, slotId)).isInstanceOf(NotFoundException.class);
    }

    // ---- the patient page ----

    @Test
    void view_showsDrugAndWindow_andNoPatientIdentity() {
        when(slotRepository.findByIdAndHospitalId(slotId, hospitalId)).thenReturn(Optional.of(slot));
        when(slotRepository.findById(slotId)).thenReturn(Optional.of(slot));
        when(regimenRepository.findById(regimenId)).thenReturn(Optional.of(regimen));
        PatientDoseService service = serviceAt(DUE.plusSeconds(60));

        PatientDoseView view = service.view(service.issueToken(hospitalId, slotId));

        assertThat(view.drugName()).isEqualTo("DEMO_DRUG_A");
        assertThat(view.windowClosesAt()).isEqualTo(CLOSES);
        assertThat(view.canRecord()).isTrue();
    }

    @Test
    void view_beforeDueTime_cannotRecordYet() {
        when(slotRepository.findByIdAndHospitalId(slotId, hospitalId)).thenReturn(Optional.of(slot));
        when(slotRepository.findById(slotId)).thenReturn(Optional.of(slot));
        when(regimenRepository.findById(regimenId)).thenReturn(Optional.of(regimen));
        PatientDoseService service = serviceAt(DUE.minusSeconds(30));

        assertThat(service.view(service.issueToken(hospitalId, slotId)).canRecord()).isFalse();
    }

    @Test
    void view_withForgedToken_isInvalid() {
        assertThatThrownBy(() -> serviceAt(DUE).view(slotId + "." + CLOSES.getEpochSecond() + ".deadbeef"))
                .isInstanceOf(InvalidDoseLinkException.class);
        verify(slotRepository, never()).findById(slotId);
    }

    @Test
    void view_afterExpiry_isExpired() {
        when(slotRepository.findByIdAndHospitalId(slotId, hospitalId)).thenReturn(Optional.of(slot));
        String token = serviceAt(DUE).issueToken(hospitalId, slotId);

        assertThatThrownBy(() -> serviceAt(CLOSES).view(token)).isInstanceOf(ExpiredDoseLinkException.class);
    }

    // ---- confirming ----

    @Test
    void record_withGoodToken_delegatesToTheSharedRulesWithTheSlotFromTheToken() {
        when(slotRepository.findByIdAndHospitalId(slotId, hospitalId)).thenReturn(Optional.of(slot));
        when(slotRepository.findById(slotId)).thenReturn(Optional.of(slot));
        when(regimenRepository.findById(regimenId)).thenReturn(Optional.of(regimen));
        PatientDoseService service = serviceAt(DUE.plusSeconds(60));

        service.record(service.issueToken(hospitalId, slotId), RecordOutcome.ADMINISTERED);

        verify(doseSlotService).recordByPatient(slotId, RecordOutcome.ADMINISTERED);
    }

    @Test
    void record_withForgedToken_neverReachesTheDoseRules() {
        assertThatThrownBy(() -> serviceAt(DUE).record("garbage", RecordOutcome.ADMINISTERED))
                .isInstanceOf(InvalidDoseLinkException.class);
        verify(doseSlotService, never()).recordByPatient(slotId, RecordOutcome.ADMINISTERED);
    }
}

package com.adherelens.clinical.repository;

import com.adherelens.clinical.domain.Patient;
import com.adherelens.common.model.CareSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

    List<Patient> findByHospitalIdOrderByFullName(UUID hospitalId);

    List<Patient> findByHospitalIdAndCareSettingOrderByFullName(UUID hospitalId, CareSetting careSetting);

    Optional<Patient> findByIdAndHospitalId(UUID id, UUID hospitalId);
}

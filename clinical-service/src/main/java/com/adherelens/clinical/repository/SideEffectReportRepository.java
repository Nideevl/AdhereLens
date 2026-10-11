package com.adherelens.clinical.repository;

import com.adherelens.clinical.domain.SideEffectReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SideEffectReportRepository extends JpaRepository<SideEffectReport, UUID> {

    List<SideEffectReport> findByPatientIdAndHospitalIdOrderByReportedAtDesc(UUID patientId, UUID hospitalId);

    Optional<SideEffectReport> findByIdAndHospitalId(UUID id, UUID hospitalId);
}

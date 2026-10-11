package com.adherelens.clinical.repository;

import com.adherelens.clinical.domain.Regimen;
import com.adherelens.clinical.domain.RegimenStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RegimenRepository extends JpaRepository<Regimen, UUID> {

    List<Regimen> findByPatientIdAndHospitalIdOrderByStartDateDesc(UUID patientId, UUID hospitalId);

    Optional<Regimen> findByIdAndHospitalId(UUID id, UUID hospitalId);

    @Query("select r.id from Regimen r where r.status = :status")
    List<UUID> findIdsByStatus(@Param("status") RegimenStatus status);
}

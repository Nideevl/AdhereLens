package com.adherelens.clinical.repository;

import com.adherelens.clinical.domain.Regimen;
import com.adherelens.clinical.domain.RegimenStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RegimenRepository extends JpaRepository<Regimen, UUID> {

    List<Regimen> findByPatientIdAndHospitalIdOrderByStartDateDesc(UUID patientId, UUID hospitalId);

    Optional<Regimen> findByIdAndHospitalId(UUID id, UUID hospitalId);

    /** FOR UPDATE SKIP LOCKED: empty if another instance is already generating slots for this regimen. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select r from Regimen r where r.id = :id")
    Optional<Regimen> findByIdForUpdateSkipLocked(@Param("id") UUID id);

    @Query("select r.id from Regimen r where r.status = :status")
    List<UUID> findIdsByStatus(@Param("status") RegimenStatus status);
}

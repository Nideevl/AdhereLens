package com.adherelens.clinical.repository;

import com.adherelens.clinical.domain.RegimenAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RegimenAuditRepository extends JpaRepository<RegimenAudit, UUID> {

    List<RegimenAudit> findByRegimenIdAndHospitalIdOrderByChangedAtDesc(UUID regimenId, UUID hospitalId);
}

package com.adherelens.notification.repository;

import com.adherelens.notification.domain.AlertStatus;
import com.adherelens.notification.domain.NurseAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NurseAlertRepository extends JpaRepository<NurseAlert, UUID> {

    boolean existsByDoseSlotId(UUID doseSlotId);

    Optional<NurseAlert> findByDoseSlotId(UUID doseSlotId);

    List<NurseAlert> findTop200ByHospitalIdAndStatusOrderByDueAtAsc(UUID hospitalId, AlertStatus status);
}

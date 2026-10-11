package com.adherelens.clinical.repository;

import com.adherelens.clinical.domain.DoseSlot;
import com.adherelens.clinical.domain.SlotStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoseSlotRepository extends JpaRepository<DoseSlot, UUID> {

    boolean existsByRegimenIdAndDueAt(UUID regimenId, Instant dueAt);

    Optional<DoseSlot> findByIdAndHospitalId(UUID id, UUID hospitalId);

    List<DoseSlot> findByHospitalIdAndStatusAndDueAtLessThanEqualOrderByDueAt(
            UUID hospitalId, SlotStatus status, Instant now, Pageable pageable);

    @Query("select s.id from DoseSlot s where s.dueEventPublished = false "
            + "and s.status <> :cancelled and s.dueAt <= :now")
    List<UUID> findDueUnpublishedIds(@Param("cancelled") SlotStatus cancelled, @Param("now") Instant now);

    @Query("select s.id from DoseSlot s where s.status = :pending and s.windowClosesAt <= :now")
    List<UUID> findExpiredPendingIds(@Param("pending") SlotStatus pending, @Param("now") Instant now);

    @Query("select s.id from DoseSlot s where s.outcomePublished = false and s.status in :statuses")
    List<UUID> findUnpublishedOutcomeIds(@Param("statuses") Collection<SlotStatus> statuses);

    @Modifying
    @Query("update DoseSlot s set s.status = :cancelled, s.version = s.version + 1 "
            + "where s.regimenId = :regimenId and s.status = :pending and s.dueAt > :now")
    int cancelFuturePending(@Param("regimenId") UUID regimenId,
                            @Param("pending") SlotStatus pending,
                            @Param("cancelled") SlotStatus cancelled,
                            @Param("now") Instant now);
}

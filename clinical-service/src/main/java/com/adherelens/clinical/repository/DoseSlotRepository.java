package com.adherelens.clinical.repository;

import com.adherelens.clinical.domain.DoseSlot;
import com.adherelens.clinical.domain.SlotStatus;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoseSlotRepository extends JpaRepository<DoseSlot, UUID> {

    boolean existsByRegimenIdAndDueAt(UUID regimenId, Instant dueAt);

    Optional<DoseSlot> findByIdAndHospitalId(UUID id, UUID hospitalId);

    /**
     * SELECT ... FOR UPDATE SKIP LOCKED. Returns empty if another instance already holds this row,
     * so two clinical-service instances never process the same slot. Must run inside a transaction;
     * the lock is released on commit or rollback. Timeout -2 is Hibernate's SKIP_LOCKED.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select s from DoseSlot s where s.id = :id")
    Optional<DoseSlot> findByIdForUpdateSkipLocked(@Param("id") UUID id);

    List<DoseSlot> findByHospitalIdAndStatusAndDueAtLessThanEqualOrderByDueAt(
            UUID hospitalId, SlotStatus status, Instant now, Pageable pageable);

    @Query("select s.id from DoseSlot s where s.dueEventPublished = false "
            + "and s.status <> :cancelled and s.dueAt <= :now")
    List<UUID> findDueUnpublishedIds(@Param("cancelled") SlotStatus cancelled, @Param("now") Instant now);

    @Query("select s.id from DoseSlot s where s.status = :pending and s.windowClosesAt <= :now")
    List<UUID> findExpiredPendingIds(@Param("pending") SlotStatus pending, @Param("now") Instant now);

    @Query("select s.id from DoseSlot s where s.outcomePublished = false and s.status in :statuses")
    List<UUID> findUnpublishedOutcomeIds(@Param("statuses") Collection<SlotStatus> statuses);

    /** Future PENDING slots were never published or recorded, so deleting them is safe. */
    @Modifying
    @Query("delete from DoseSlot s where s.regimenId = :regimenId and s.status = :pending and s.dueAt > :now")
    int deleteFuturePending(@Param("regimenId") UUID regimenId,
                            @Param("pending") SlotStatus pending,
                            @Param("now") Instant now);

    @Modifying
    @Query("update DoseSlot s set s.status = :cancelled, s.version = s.version + 1 "
            + "where s.regimenId = :regimenId and s.status = :pending and s.dueAt > :now")
    int cancelFuturePending(@Param("regimenId") UUID regimenId,
                            @Param("pending") SlotStatus pending,
                            @Param("cancelled") SlotStatus cancelled,
                            @Param("now") Instant now);
}

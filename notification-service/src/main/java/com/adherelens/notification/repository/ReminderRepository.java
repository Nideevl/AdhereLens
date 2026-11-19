package com.adherelens.notification.repository;

import com.adherelens.notification.domain.Reminder;
import com.adherelens.notification.domain.ReminderStatus;
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

public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

    boolean existsByDoseSlotId(UUID doseSlotId);

    List<Reminder> findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
            ReminderStatus status, Instant now);

    /**
     * Claiming lock: SELECT ... FOR UPDATE SKIP LOCKED (lock timeout -2 = skip locked). If another
     * instance is working on this row the result is empty and the caller moves on.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select r from Reminder r where r.id = :id")
    Optional<Reminder> findByIdSkipLocked(@Param("id") UUID id);

    /** Normal row lock (waits). Used to record the result of a send this instance claimed. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reminder r where r.id = :id")
    Optional<Reminder> findByIdForUpdate(@Param("id") UUID id);

    /** Moves every reminder of one dose slot that is still open (not sent, failed or cancelled) to a new status. */
    @Modifying
    @Query("update Reminder r set r.status = :to where r.doseSlotId = :slotId and r.status in :open")
    int changeStatusOfOpen(@Param("slotId") UUID slotId,
                           @Param("open") Collection<ReminderStatus> open,
                           @Param("to") ReminderStatus to);

    /** Gives back reminders claimed before the cutoff: the instance that claimed them probably crashed. */
    @Modifying
    @Query("update Reminder r set r.status = :pending, r.claimedAt = null "
            + "where r.status = :sending and r.claimedAt < :cutoff")
    int releaseStaleClaims(@Param("sending") ReminderStatus sending,
                           @Param("pending") ReminderStatus pending,
                           @Param("cutoff") Instant cutoff);
}

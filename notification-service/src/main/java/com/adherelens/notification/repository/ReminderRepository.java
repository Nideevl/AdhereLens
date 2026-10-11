package com.adherelens.notification.repository;

import com.adherelens.notification.domain.Reminder;
import com.adherelens.notification.domain.ReminderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

    boolean existsByDoseSlotId(UUID doseSlotId);

    List<Reminder> findTop100ByStatusAndScheduledForLessThanEqualOrderByScheduledForAsc(
            ReminderStatus status, Instant now);

    /** Moves every reminder of one dose slot from one status to another. Returns how many moved. */
    @Modifying
    @Query("update Reminder r set r.status = :to where r.doseSlotId = :slotId and r.status = :from")
    int changeStatus(@Param("slotId") UUID slotId,
                     @Param("from") ReminderStatus from,
                     @Param("to") ReminderStatus to);
}

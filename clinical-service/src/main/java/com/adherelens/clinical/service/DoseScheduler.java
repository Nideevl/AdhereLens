package com.adherelens.clinical.service;

import com.adherelens.clinical.domain.RegimenStatus;
import com.adherelens.clinical.domain.SlotStatus;
import com.adherelens.clinical.repository.DoseSlotRepository;
import com.adherelens.clinical.repository.RegimenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Runs every 30 seconds. Assumes a single running instance of clinical-service.
 * Kafka delivery is at-least-once: if a publish succeeds but the database update fails,
 * the event is sent again on the next tick.
 */
@Component
public class DoseScheduler {

    private static final Logger log = LoggerFactory.getLogger(DoseScheduler.class);

    private final DoseSlotProcessor processor;
    private final RegimenRepository regimenRepository;
    private final DoseSlotRepository slotRepository;
    private final Clock clock;

    public DoseScheduler(DoseSlotProcessor processor,
                         RegimenRepository regimenRepository,
                         DoseSlotRepository slotRepository,
                         Clock clock) {
        this.processor = processor;
        this.regimenRepository = regimenRepository;
        this.slotRepository = slotRepository;
        this.clock = clock;
    }

    @Scheduled(initialDelay = 10_000, fixedDelay = 30_000)
    public void tick() {
        Instant now = Instant.now(clock);

        forEachSafely("create slots",
                regimenRepository.findIdsByStatus(RegimenStatus.ACTIVE), processor::createSlotsIfMissing);

        forEachSafely("publish due",
                slotRepository.findDueUnpublishedIds(SlotStatus.CANCELLED, now), processor::publishDue);

        forEachSafely("mark missed",
                slotRepository.findExpiredPendingIds(SlotStatus.PENDING, now), processor::markMissedIfExpired);

        forEachSafely("publish outcome",
                slotRepository.findUnpublishedOutcomeIds(
                        List.of(SlotStatus.HIT, SlotStatus.DELAYED, SlotStatus.MISSED, SlotStatus.REFUSED)),
                processor::publishOutcome);
    }

    private void forEachSafely(String step, List<UUID> ids, Consumer<UUID> action) {
        for (UUID id : ids) {
            try {
                action.accept(id);
            } catch (Exception e) {
                log.warn("Scheduler step '{}' failed for {}: {}", step, id, e.toString());
            }
        }
    }
}

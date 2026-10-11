package com.adherelens.notification.service;

import com.adherelens.common.event.DoseDueEvent;
import com.adherelens.common.event.DoseOutcomeEvent;
import com.adherelens.common.model.CareSetting;
import com.adherelens.notification.domain.NurseAlert;
import com.adherelens.notification.domain.Reminder;
import com.adherelens.notification.domain.ReminderStatus;
import com.adherelens.notification.domain.SlotOutcome;
import com.adherelens.notification.repository.NurseAlertRepository;
import com.adherelens.notification.repository.ReminderRepository;
import com.adherelens.notification.repository.SlotOutcomeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * Handles the two Kafka events. Delivery is at-least-once, so every method is safe to run
 * twice for the same dose slot. Inpatients get a nurse alert. Outpatients get a small, fixed
 * number of reminder attempts that stop as soon as the dose reaches a final status.
 */
@Service
public class NotificationService {

    private final NurseAlertRepository alertRepository;
    private final ReminderRepository reminderRepository;
    private final SlotOutcomeRepository outcomeRepository;
    private final Clock clock;
    private final List<Integer> offsetsMinutes;

    public NotificationService(NurseAlertRepository alertRepository,
                               ReminderRepository reminderRepository,
                               SlotOutcomeRepository outcomeRepository,
                               Clock clock,
                               @Value("${adherelens.notification.outpatient-offsets-minutes}") String offsets) {
        this.alertRepository = alertRepository;
        this.reminderRepository = reminderRepository;
        this.outcomeRepository = outcomeRepository;
        this.clock = clock;
        this.offsetsMinutes = Arrays.stream(offsets.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .sorted()
                .toList();
    }

    @Transactional
    public void onDoseDue(DoseDueEvent event) {
        if (outcomeRepository.existsById(event.doseSlotId())) {
            return; // the dose already finished, nothing left to remind about
        }
        if (event.careSetting() == CareSetting.INPATIENT) {
            if (!alertRepository.existsByDoseSlotId(event.doseSlotId())) {
                alertRepository.save(new NurseAlert(event.doseSlotId(), event.hospitalId(), event.patientId(),
                        event.regimenId(), event.dueAt(), event.windowClosesAt()));
            }
            return;
        }
        if (reminderRepository.existsByDoseSlotId(event.doseSlotId())) {
            return; // duplicate event
        }
        for (int i = 0; i < offsetsMinutes.size(); i++) {
            Instant when = event.dueAt().plus(Duration.ofMinutes(offsetsMinutes.get(i)));
            if (!when.isBefore(event.windowClosesAt())) {
                continue; // never remind outside the window
            }
            reminderRepository.save(new Reminder(event.doseSlotId(), event.hospitalId(), event.patientId(),
                    i + 1, when, event.windowClosesAt()));
        }
    }

    @Transactional
    public void onDoseOutcome(DoseOutcomeEvent event) {
        if (!outcomeRepository.existsById(event.doseSlotId())) {
            outcomeRepository.save(new SlotOutcome(event.doseSlotId(), event.status().name(), Instant.now(clock)));
        }
        reminderRepository.changeStatus(event.doseSlotId(), ReminderStatus.PENDING, ReminderStatus.CANCELLED);
        alertRepository.findByDoseSlotId(event.doseSlotId()).ifPresent(alert -> {
            if (alert.getResolvedAt() == null) {
                alert.resolve(event.status().name(), Instant.now(clock));
            }
        });
    }
}

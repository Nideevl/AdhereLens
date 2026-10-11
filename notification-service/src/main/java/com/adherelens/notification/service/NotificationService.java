package com.adherelens.notification.service;

import com.adherelens.common.event.DoseDueEvent;
import com.adherelens.common.event.DoseOutcomeEvent;
import com.adherelens.common.model.CareSetting;
import com.adherelens.notification.domain.NurseAlert;
import com.adherelens.notification.domain.Reminder;
import com.adherelens.notification.domain.ReminderChannel;
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
import java.util.Comparator;
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
    private final List<Step> steps;

    public NotificationService(NurseAlertRepository alertRepository,
                               ReminderRepository reminderRepository,
                               SlotOutcomeRepository outcomeRepository,
                               Clock clock,
                               @Value("${adherelens.notification.outpatient-schedule}") String schedule) {
        this.alertRepository = alertRepository;
        this.reminderRepository = reminderRepository;
        this.outcomeRepository = outcomeRepository;
        this.clock = clock;
        // Example: "0:SMS,15:SMS,30:CALL" = a text at the due time, another 15 minutes later, a call at 30.
        this.steps = Arrays.stream(schedule.split(","))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .map(part -> {
                    String[] pair = part.split(":");
                    return new Step(Integer.parseInt(pair[0].trim()),
                            ReminderChannel.valueOf(pair[1].trim().toUpperCase()));
                })
                .sorted(Comparator.comparingInt(Step::offsetMinutes))
                .toList();
    }

    /** One planned reminder: minutes after the due time, and how to reach the patient. */
    private record Step(int offsetMinutes, ReminderChannel channel) {
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
        for (int i = 0; i < steps.size(); i++) {
            Step step = steps.get(i);
            Instant when = event.dueAt().plus(Duration.ofMinutes(step.offsetMinutes()));
            if (!when.isBefore(event.windowClosesAt())) {
                continue; // never remind outside the window
            }
            reminderRepository.save(new Reminder(event.doseSlotId(), event.hospitalId(), event.patientId(),
                    i + 1, step.channel(), when, event.windowClosesAt()));
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

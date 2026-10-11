package com.adherelens.notification.kafka;

import com.adherelens.common.event.DoseDueEvent;
import com.adherelens.common.event.DoseOutcomeEvent;
import com.adherelens.common.event.Topics;
import com.adherelens.notification.service.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Receives each message as plain text and parses it here. The two topics carry different
 * event types, so one shared JSON deserializer setting would not fit both.
 */
@Component
public class DoseEventListener {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public DoseEventListener(NotificationService notificationService, ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = Topics.DOSE_DUE)
    public void onDoseDue(String payload) throws JsonProcessingException {
        notificationService.onDoseDue(objectMapper.readValue(payload, DoseDueEvent.class));
    }

    @KafkaListener(topics = Topics.DOSE_OUTCOME)
    public void onDoseOutcome(String payload) throws JsonProcessingException {
        notificationService.onDoseOutcome(objectMapper.readValue(payload, DoseOutcomeEvent.class));
    }
}

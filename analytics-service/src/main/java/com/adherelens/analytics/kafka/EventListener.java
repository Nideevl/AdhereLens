package com.adherelens.analytics.kafka;

import com.adherelens.analytics.service.IngestService;
import com.adherelens.common.event.DoseOutcomeEvent;
import com.adherelens.common.event.SideEffectEvent;
import com.adherelens.common.event.Topics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Reads each message as text and parses it here, because the topics carry different event types. */
@Component
public class EventListener {

    private final IngestService ingestService;
    private final ObjectMapper objectMapper;

    public EventListener(IngestService ingestService, ObjectMapper objectMapper) {
        this.ingestService = ingestService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = Topics.DOSE_OUTCOME)
    public void onDoseOutcome(String payload) throws JsonProcessingException {
        ingestService.onDoseOutcome(objectMapper.readValue(payload, DoseOutcomeEvent.class));
    }

    @KafkaListener(topics = Topics.SIDE_EFFECT)
    public void onSideEffect(String payload) throws JsonProcessingException {
        ingestService.onSideEffect(objectMapper.readValue(payload, SideEffectEvent.class));
    }
}

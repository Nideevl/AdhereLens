package com.adherelens.notification.service;

import com.adherelens.notification.dto.ContactResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

/** Asks clinical-service for a patient's phone at send time, so no phone is stored here. */
@Component
public class ClinicalClient {

    private final RestClient client;
    private final String internalKey;

    public ClinicalClient(@Value("${adherelens.clinical.base-url}") String baseUrl,
                          @Value("${adherelens.internal.api-key}") String internalKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(5_000);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.internalKey = internalKey;
    }

    /** Throws if the patient is not found, is inactive, or has no phone, so the attempt is marked FAILED. */
    public String contactPhone(UUID hospitalId, UUID patientId) {
        ContactResponse contact = client.get()
                .uri("/internal/patients/{patientId}/contact?hospitalId={hospitalId}", patientId, hospitalId)
                .header("X-Internal-Key", internalKey)
                .retrieve()
                .body(ContactResponse.class);
        if (contact == null || contact.phone() == null || contact.phone().isBlank()) {
            throw new IllegalStateException("No phone number on file");
        }
        return contact.phone();
    }
}

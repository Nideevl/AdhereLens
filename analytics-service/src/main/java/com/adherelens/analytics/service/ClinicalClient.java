package com.adherelens.analytics.service;

import com.adherelens.analytics.dto.ClinicalContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

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

    public ClinicalContext context(UUID hospitalId, UUID regimenId) {
        ClinicalContext context = client.get()
                .uri("/internal/regimens/{regimenId}/analytics-context?hospitalId={hospitalId}", regimenId, hospitalId)
                .header("X-Internal-Key", internalKey)
                .retrieve()
                .body(ClinicalContext.class);
        if (context == null) {
            throw new IllegalStateException("Empty context for regimen " + regimenId);
        }
        return context;
    }
}

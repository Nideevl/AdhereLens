package com.adherelens.clinical.web;

import com.adherelens.clinical.domain.DoseSlot;
import com.adherelens.clinical.service.ExpiredDoseLinkException;
import com.adherelens.clinical.service.InvalidDoseLinkException;
import com.adherelens.clinical.service.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void optimisticLockFailure_mapsTo409() {
        var response = handler.conflict(new ObjectOptimisticLockingFailureException(DoseSlot.class, UUID.randomUUID()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsKey("error");
    }

    @Test
    void notFound_mapsTo404() {
        var response = handler.notFound(new NotFoundException("Dose not found"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("error", "Dose not found");
    }

    @Test
    void businessRuleViolation_mapsTo400() {
        var response = handler.badRequest(new IllegalArgumentException("This dose is not due yet"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "This dose is not due yet");
    }

    @Test
    void invalidDoseLink_mapsTo404() {
        var response = handler.invalidLink(new InvalidDoseLinkException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void expiredDoseLink_mapsTo410() {
        var response = handler.expiredLink(new ExpiredDoseLinkException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GONE);
    }
}

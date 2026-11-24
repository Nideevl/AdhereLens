package com.adherelens.notification.dto;

/** What clinical-service returns for a dose link request. The token is put into the SMS and never stored. */
public record DoseLinkResponse(String token) {
}

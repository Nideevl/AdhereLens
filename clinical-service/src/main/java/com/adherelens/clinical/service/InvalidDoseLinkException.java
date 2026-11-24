package com.adherelens.clinical.service;

/** The link is malformed, forged, or points at nothing a patient may act on. Mapped to 404. */
public class InvalidDoseLinkException extends RuntimeException {
    public InvalidDoseLinkException() {
        super("This link is not valid");
    }
}

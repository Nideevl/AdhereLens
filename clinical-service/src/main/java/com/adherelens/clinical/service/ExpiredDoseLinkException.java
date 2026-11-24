package com.adherelens.clinical.service;

/** The link was genuine but its dose window has closed. Mapped to 410. */
public class ExpiredDoseLinkException extends RuntimeException {
    public ExpiredDoseLinkException() {
        super("This link has expired");
    }
}

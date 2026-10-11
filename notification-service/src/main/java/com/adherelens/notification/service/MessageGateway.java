package com.adherelens.notification.service;

/** The outside world: something that can text or call a phone number. */
public interface MessageGateway {

    void sendSms(String toPhone, String text);

    void placeCall(String toPhone, String text);
}

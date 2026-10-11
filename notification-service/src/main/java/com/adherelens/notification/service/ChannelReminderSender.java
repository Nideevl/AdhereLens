package com.adherelens.notification.service;

import com.adherelens.notification.domain.Reminder;
import org.springframework.stereotype.Component;

/** Looks up the phone, then sends the reminder by text or call as the attempt's channel says. */
@Component
public class ChannelReminderSender implements ReminderSender {

    private static final String SMS_TEXT =
            "AdhereLens reminder: it is time for your medicine dose. Please take it as prescribed.";
    private static final String CALL_TEXT =
            "This is a reminder from your hospital. It is time for your medicine dose. Please take it as prescribed.";

    private final ClinicalClient clinicalClient;
    private final MessageGateway gateway;

    public ChannelReminderSender(ClinicalClient clinicalClient, MessageGateway gateway) {
        this.clinicalClient = clinicalClient;
        this.gateway = gateway;
    }

    @Override
    public void send(Reminder reminder) {
        String phone = clinicalClient.contactPhone(reminder.getHospitalId(), reminder.getPatientId());
        switch (reminder.getChannel()) {
            case SMS -> gateway.sendSms(phone, SMS_TEXT);
            case CALL -> gateway.placeCall(phone, CALL_TEXT);
        }
    }
}

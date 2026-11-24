package com.adherelens.notification.service;

import com.adherelens.notification.domain.Reminder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Looks up the phone, then sends the reminder by text or call as the attempt's channel says.
 * A text carries a signed one-time link so the patient can confirm that exact dose. A call cannot
 * carry a link. If the link cannot be fetched the plain reminder still goes out.
 */
@Component
public class ChannelReminderSender implements ReminderSender {

    private static final Logger log = LoggerFactory.getLogger(ChannelReminderSender.class);

    private static final String SMS_TEXT =
            "AdhereLens reminder: it is time for your medicine dose. Please take it as prescribed.";
    private static final String SMS_TEXT_WITH_LINK = SMS_TEXT + " Confirm here: ";
    private static final String CALL_TEXT =
            "This is a reminder from your hospital. It is time for your medicine dose. Please take it as prescribed.";

    private final ClinicalClient clinicalClient;
    private final MessageGateway gateway;
    private final String publicBaseUrl;

    public ChannelReminderSender(ClinicalClient clinicalClient,
                                 MessageGateway gateway,
                                 @Value("${adherelens.notification.public-base-url}") String publicBaseUrl) {
        this.clinicalClient = clinicalClient;
        this.gateway = gateway;
        this.publicBaseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
    }

    @Override
    public void send(Reminder reminder) {
        String phone = clinicalClient.contactPhone(reminder.getHospitalId(), reminder.getPatientId());
        switch (reminder.getChannel()) {
            case SMS -> gateway.sendSms(phone, smsText(reminder));
            case CALL -> gateway.placeCall(phone, CALL_TEXT);
        }
    }

    private String smsText(Reminder reminder) {
        try {
            String token = clinicalClient.doseLinkToken(reminder.getHospitalId(), reminder.getDoseSlotId());
            return SMS_TEXT_WITH_LINK + publicBaseUrl + "/d/" + token;
        } catch (RuntimeException e) {
            // Class name only: messages can contain ids. The reminder matters more than the link.
            log.warn("Sending reminder without a link: {}", e.getClass().getSimpleName());
            return SMS_TEXT;
        }
    }
}

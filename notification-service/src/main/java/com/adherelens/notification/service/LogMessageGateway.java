package com.adherelens.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Used while adherelens.twilio.enabled is false (the default). Logs the action with a masked number. */
@Component
@ConditionalOnProperty(name = "adherelens.twilio.enabled", havingValue = "false", matchIfMissing = true)
public class LogMessageGateway implements MessageGateway {

    private static final Logger log = LoggerFactory.getLogger(LogMessageGateway.class);

    @Override
    public void sendSms(String toPhone, String text) {
        log.info("[TWILIO-STUB] SMS to {}: {}", mask(toPhone), text);
    }

    @Override
    public void placeCall(String toPhone, String text) {
        log.info("[TWILIO-STUB] CALL to {}: {}", mask(toPhone), text);
    }

    private static String mask(String phone) {
        if (phone == null || phone.length() <= 4) {
            return "****";
        }
        return "******" + phone.substring(phone.length() - 4);
    }
}

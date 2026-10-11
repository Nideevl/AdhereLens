package com.adherelens.notification.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Sends texts and calls through Twilio's REST API (no SDK). Active only when
 * adherelens.twilio.enabled=true. Any Twilio error (4xx/5xx) throws, and the reminder
 * attempt is then marked FAILED. Not yet run against a real Twilio account.
 */
@Component
@ConditionalOnProperty(name = "adherelens.twilio.enabled", havingValue = "true")
public class TwilioMessageGateway implements MessageGateway {

    private final RestClient client;
    private final String accountSid;
    private final String authToken;
    private final String fromNumber;
    private final String defaultCountryCode;

    public TwilioMessageGateway(@Value("${adherelens.twilio.account-sid}") String accountSid,
                                @Value("${adherelens.twilio.auth-token}") String authToken,
                                @Value("${adherelens.twilio.from-number}") String fromNumber,
                                @Value("${adherelens.twilio.default-country-code}") String defaultCountryCode,
                                @Value("${adherelens.twilio.base-url}") String baseUrl) {
        if (accountSid.isBlank() || authToken.isBlank() || fromNumber.isBlank()) {
            throw new IllegalStateException(
                    "Twilio is enabled but TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN or TWILIO_FROM_NUMBER is missing");
        }
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
        this.defaultCountryCode = defaultCountryCode;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(10_000);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Override
    public void sendSms(String toPhone, String text) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", toE164(toPhone));
        form.add("From", fromNumber);
        form.add("Body", text);
        post("Messages.json", form);
    }

    @Override
    public void placeCall(String toPhone, String text) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", toE164(toPhone));
        form.add("From", fromNumber);
        form.add("Twiml", "<Response><Say>" + escapeXml(text) + "</Say></Response>");
        post("Calls.json", form);
    }

    private void post(String resource, MultiValueMap<String, String> form) {
        client.post()
                .uri("/2010-04-01/Accounts/" + accountSid + "/" + resource)
                .headers(headers -> headers.setBasicAuth(accountSid, authToken))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity();
    }

    /** Twilio needs +<country><number>. A number without a leading + gets the default country code. */
    private String toE164(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("+")) {
            return "+" + trimmed.substring(1).replaceAll("\\D", "");
        }
        return defaultCountryCode + trimmed.replaceAll("\\D", "").replaceFirst("^0+", "");
    }

    private static String escapeXml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

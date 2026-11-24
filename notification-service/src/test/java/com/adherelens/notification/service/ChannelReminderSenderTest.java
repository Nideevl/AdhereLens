package com.adherelens.notification.service;

import com.adherelens.notification.domain.Reminder;
import com.adherelens.notification.domain.ReminderChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChannelReminderSenderTest {

    private final UUID hospitalId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final UUID slotId = UUID.randomUUID();

    @Mock ClinicalClient clinicalClient;
    @Mock MessageGateway gateway;

    private ChannelReminderSender sender;
    private Reminder reminder;

    @BeforeEach
    void setUp() {
        sender = new ChannelReminderSender(clinicalClient, gateway, "https://app.example.test/");
    }

    private void channel(ReminderChannel channel) {
        reminder = new Reminder(slotId, hospitalId, patientId, 1, channel,
                Instant.parse("2026-11-19T03:00:00Z"), Instant.parse("2026-11-19T04:00:00Z"));
    }

    @Test
    void sms_carriesTheSignedLink_withoutADoubleSlash() {
        channel(ReminderChannel.SMS);
        when(clinicalClient.contactPhone(hospitalId, patientId)).thenReturn("+911234567890");
        when(clinicalClient.doseLinkToken(hospitalId, slotId)).thenReturn("tok.123.sig");

        sender.send(reminder);

        verify(gateway).sendSms(eq("+911234567890"), contains("https://app.example.test/d/tok.123.sig"));
    }

    @Test
    void sms_stillGoesOutWithoutALink_whenTheTokenCannotBeFetched() {
        channel(ReminderChannel.SMS);
        when(clinicalClient.contactPhone(hospitalId, patientId)).thenReturn("+911234567890");
        when(clinicalClient.doseLinkToken(hospitalId, slotId)).thenThrow(new IllegalStateException("clinical down"));

        sender.send(reminder);

        verify(gateway).sendSms(eq("+911234567890"), org.mockito.ArgumentMatchers.argThat(t -> !t.contains("/d/")));
    }

    @Test
    void call_hasNoLink_andNeverAsksForOne() {
        channel(ReminderChannel.CALL);
        when(clinicalClient.contactPhone(hospitalId, patientId)).thenReturn("+911234567890");

        sender.send(reminder);

        verify(gateway).placeCall(eq("+911234567890"), anyString());
        verify(clinicalClient, never()).doseLinkToken(hospitalId, slotId);
    }

    @Test
    void phoneLookupFailure_propagates_soTheAttemptIsMarkedFailed() {
        channel(ReminderChannel.SMS);
        when(clinicalClient.contactPhone(hospitalId, patientId)).thenThrow(new IllegalStateException("No phone number on file"));

        assertThatThrownBy(() -> sender.send(reminder)).isInstanceOf(IllegalStateException.class);
        verify(gateway, never()).sendSms(anyString(), anyString());
    }
}

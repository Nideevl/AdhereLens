package com.adherelens.notification.service;

import com.adherelens.notification.domain.Reminder;
import com.adherelens.notification.domain.ReminderChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderDispatcherTest {

    @Mock ReminderStateService state;
    @Mock ReminderSender sender;

    private final UUID id = UUID.randomUUID();
    private final Reminder claimed = new Reminder(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1,
            ReminderChannel.SMS, Instant.parse("2026-11-19T03:00:00Z"), Instant.parse("2026-11-19T04:00:00Z"));

    private ReminderDispatcher dispatcher() {
        return new ReminderDispatcher(state, sender);
    }

    @Test
    void successfulSendIsClaimedThenSentThenRecorded() {
        when(state.claim(id)).thenReturn(claimed);

        dispatcher().dispatch(id);

        InOrder order = inOrder(state, sender);
        order.verify(state).claim(id);
        order.verify(sender).send(claimed);
        order.verify(state).recordSent(id);
        verify(state, never()).recordFailure(any(), any());
    }

    @Test
    void failedSendRecordsOnlyTheExceptionClassName() {
        when(state.claim(id)).thenReturn(claimed);
        doThrow(new IllegalStateException("Twilio rejected +91XXXXXXXXXX")).when(sender).send(claimed);

        dispatcher().dispatch(id);

        verify(state).recordFailure(id, "IllegalStateException"); // never the message: it can hold a phone number
        verify(state, never()).recordSent(any());
    }

    @Test
    void nothingIsSentWhenTheClaimIsNotGranted() {
        when(state.claim(id)).thenReturn(null); // locked by another instance, not due, cancelled ...

        dispatcher().dispatch(id);

        verifyNoInteractions(sender);
        verify(state, never()).recordSent(any());
        verify(state, never()).recordFailure(any(), any());
    }
}

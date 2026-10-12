package com.example.quanlydetai.notification;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

class EmailDeliveryWorkerTest {
    @Test
    void disabledDeliveryDoesNotClaimOrSend() {
        var queue = mock(ResultEmailService.class);
        var transport = mock(EmailTransport.class);
        new EmailDeliveryWorker(queue, transport, false).deliverBatch();
        verifyNoInteractions(queue, transport);
    }

    @Test
    void failureIsQueuedForRetryAndDoesNotBlockNextRecipient() {
        var queue = mock(ResultEmailService.class);
        var transport = mock(EmailTransport.class);
        when(queue.claim())
                .thenReturn(
                        new ResultEmailService.Delivery(
                                "first", "a@example.test", "Kết quả", "Điểm", 1),
                        new ResultEmailService.Delivery(
                                "second", "b@example.test", "Kết quả", "Điểm", 1),
                        null);
        when(transport.send("first", "a@example.test", "Kết quả", "Điểm"))
                .thenThrow(new IllegalStateException("offline"));
        when(transport.send("second", "b@example.test", "Kết quả", "Điểm")).thenReturn("receipt");
        new EmailDeliveryWorker(queue, transport, true).deliverBatch();
        verify(queue).complete("first", 1, null);
        verify(queue).complete("second", 1, "receipt");
    }
}

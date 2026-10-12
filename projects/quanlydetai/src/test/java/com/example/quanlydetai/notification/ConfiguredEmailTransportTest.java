package com.example.quanlydetai.notification;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ConfiguredEmailTransportTest {
    @Test
    void smtpSendsAnIndividualMessageAndPropagatesFailure() {
        var smtp = mock(JavaMailSender.class);
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(smtp);
        var transport = new ConfiguredEmailTransport("smtp", "fit@example.test", "", provider);
        assertThat(transport.send("id", "sv@example.test", "Kết quả", "Điểm: 6.75"))
                .isEqualTo("smtp-accepted");
        var message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(smtp).send(message.capture());
        assertThat(message.getValue().getTo()).containsExactly("sv@example.test");
        assertThat(message.getValue().getText()).isEqualTo("Điểm: 6.75");
        doThrow(new org.springframework.mail.MailSendException("offline"))
                .when(smtp)
                .send(any(SimpleMailMessage.class));
        assertThatThrownBy(() -> transport.send("id", "sv@example.test", "Kết quả", "Điểm"))
                .isInstanceOf(org.springframework.mail.MailSendException.class);
    }

    @Test
    void brevoUsesHttpsApiAndRequiresAnAcceptanceReceipt() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var transport =
                new ConfiguredEmailTransport(
                        "brevo",
                        "fit@example.test",
                        "test-key",
                        mock(ObjectProvider.class),
                        builder);
        server.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "test-key"))
                .andExpect(
                        content()
                                .json(
                                        """
                                        {"sender":{"email":"fit@example.test"},"to":[{"email":"sv@example.test"}],
                                         "subject":"Kết quả", "textContent":"Điểm: 6.75", "headers":{"X-Notification-ID":"id"}}
                                        """))
                .andRespond(
                        withSuccess("{\"messageId\":\"accepted-id\"}", MediaType.APPLICATION_JSON));
        assertThat(transport.send("id", "sv@example.test", "Kết quả", "Điểm: 6.75"))
                .isEqualTo("accepted-id");
        server.verify();
        server.reset();
        server.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> transport.send("id", "sv@example.test", "Kết quả", "Điểm"))
                .isInstanceOf(IllegalStateException.class);
        server.verify();
        server.reset();
        server.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        assertThatThrownBy(() -> transport.send("id", "sv@example.test", "Kết quả", "Điểm"))
                .isInstanceOf(org.springframework.web.client.HttpClientErrorException.class);
        server.verify();
    }
}

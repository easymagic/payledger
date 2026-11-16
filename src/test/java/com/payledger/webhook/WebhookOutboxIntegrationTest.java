package com.payledger.webhook;

import com.payledger.domain.WebhookDelivery;
import com.payledger.domain.WebhookDeliveryStatus;
import com.payledger.domain.WebhookEndpoint;
import com.payledger.repository.WebhookDeliveryRepository;
import com.payledger.repository.WebhookEndpointRepository;
import com.payledger.service.TransferCommand;
import com.payledger.service.TransferService;
import com.payledger.service.WalletService;
import com.payledger.support.AbstractIntegrationTest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookOutboxIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    WalletService walletService;
    @Autowired
    TransferService transferService;
    @Autowired
    WebhookEndpointRepository endpointRepository;
    @Autowired
    WebhookDeliveryRepository deliveryRepository;
    @Autowired
    WebhookDispatchWorker dispatchWorker;
    @Autowired
    WebhookSignatureService signatureService;

    private MockWebServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void stopServer() throws IOException {
        server.shutdown();
    }

    @Test
    void transfer_enqueuesOutboxAndDeliversWithSignature() throws Exception {
        String suffix = UUID.randomUUID().toString();
        var source = walletService.provisionWallet(
                "Hook Merch", "hook-" + suffix + "@example.com",
                "s-" + suffix, "S", "s-" + suffix + "@example.com",
                "NGN", new BigDecimal("50.0000"));
        var dest = walletService.provisionWallet(
                "Hook Merch", "hook-" + suffix + "@example.com",
                "d-" + suffix, "D", "d-" + suffix + "@example.com",
                "NGN", new BigDecimal("0.0000"));

        String secret = "whsec-" + suffix;
        WebhookEndpoint endpoint = endpointRepository.save(new WebhookEndpoint(
                source.merchant(),
                server.url("/hooks").toString(),
                secret,
                "TRANSFER_COMPLETED"));

        server.enqueue(new MockResponse().setResponseCode(500).setBody("down"));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));

        transferService.transfer(new TransferCommand(
                source.wallet().getId(), dest.wallet().getId(),
                new BigDecimal("10.0000"), "NGN",
                "hook-xfer-" + suffix, "webhook"));

        List<WebhookDelivery> pending = deliveryRepository.findDue(
                WebhookDeliveryStatus.PENDING, Instant.now().plusSeconds(5), PageRequest.of(0, 10));
        assertThat(pending).isNotEmpty();
        UUID deliveryId = pending.stream()
                .filter(d -> d.getEndpoint().getId().equals(endpoint.getId()))
                .findFirst()
                .orElseThrow()
                .getId();

        dispatchWorker.dispatchOne(deliveryId);
        WebhookDelivery afterFail = deliveryRepository.findWithEndpointById(deliveryId).orElseThrow();
        assertThat(afterFail.getAttemptCount()).isEqualTo(1);
        assertThat(afterFail.getStatus()).isEqualTo(WebhookDeliveryStatus.PENDING);

        // Make it due again
        afterFail.resetForManualRetry();
        deliveryRepository.save(afterFail);

        dispatchWorker.dispatchOne(deliveryId);
        WebhookDelivery delivered = deliveryRepository.findWithEndpointById(deliveryId).orElseThrow();
        assertThat(delivered.getStatus()).isEqualTo(WebhookDeliveryStatus.DELIVERED);

        RecordedRequest failed = server.takeRequest(2, TimeUnit.SECONDS);
        RecordedRequest success = server.takeRequest(2, TimeUnit.SECONDS);
        assertThat(failed).isNotNull();
        assertThat(success).isNotNull();
        assertThat(success.getHeader("X-PayLedger-Event")).isEqualTo("TRANSFER_COMPLETED");
        String signature = success.getHeader("X-PayLedger-Signature");
        assertThat(signature).isEqualTo(signatureService.sign(secret, success.getBody().readUtf8()));
    }
}

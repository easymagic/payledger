package com.payledger.webhook;

import com.payledger.domain.WebhookDelivery;
import com.payledger.domain.WebhookDeliveryStatus;
import com.payledger.repository.WebhookDeliveryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class WebhookDispatcher {

    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookDispatchWorker worker;
    private final int batchSize;

    public WebhookDispatcher(
            WebhookDeliveryRepository deliveryRepository,
            WebhookDispatchWorker worker,
            @Value("${payledger.webhooks.batch-size:20}") int batchSize) {
        this.deliveryRepository = deliveryRepository;
        this.worker = worker;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${payledger.webhooks.poll-interval-ms:2000}")
    public void pollAndDispatch() {
        List<WebhookDelivery> due = deliveryRepository.findDue(
                WebhookDeliveryStatus.PENDING,
                Instant.now(),
                PageRequest.of(0, batchSize));
        for (WebhookDelivery delivery : due) {
            worker.dispatchOne(delivery.getId());
        }
    }
}

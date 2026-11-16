package com.payledger.service;

import com.payledger.domain.Merchant;
import com.payledger.domain.WebhookDelivery;
import com.payledger.domain.WebhookDeliveryStatus;
import com.payledger.domain.WebhookEndpoint;
import com.payledger.exception.ResourceNotFoundException;
import com.payledger.repository.MerchantRepository;
import com.payledger.repository.WebhookDeliveryRepository;
import com.payledger.repository.WebhookEndpointRepository;
import com.payledger.security.MerchantAccessService;
import com.payledger.webhook.WebhookDispatchWorker;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class WebhookAdminService {

    private final WebhookEndpointRepository endpointRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final MerchantRepository merchantRepository;
    private final MerchantAccessService merchantAccessService;
    private final WebhookDispatchWorker dispatchWorker;

    public WebhookAdminService(
            WebhookEndpointRepository endpointRepository,
            WebhookDeliveryRepository deliveryRepository,
            MerchantRepository merchantRepository,
            MerchantAccessService merchantAccessService,
            WebhookDispatchWorker dispatchWorker) {
        this.endpointRepository = endpointRepository;
        this.deliveryRepository = deliveryRepository;
        this.merchantRepository = merchantRepository;
        this.merchantAccessService = merchantAccessService;
        this.dispatchWorker = dispatchWorker;
    }

    @Transactional
    public WebhookEndpoint registerEndpoint(UUID merchantId, String url, String secret, String eventTypes) {
        merchantAccessService.requireMerchantAccess(merchantId);
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId));
        return endpointRepository.save(new WebhookEndpoint(merchant, url, secret, eventTypes));
    }

    @Transactional(readOnly = true)
    public Page<WebhookDelivery> listDeadLetters(Pageable pageable) {
        return deliveryRepository.findByStatus(WebhookDeliveryStatus.DEAD_LETTER, pageable);
    }

    @Transactional(readOnly = true)
    public WebhookDelivery getDelivery(UUID deliveryId) {
        return deliveryRepository.findWithEndpointById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("WebhookDelivery", deliveryId));
    }

    @Transactional
    public WebhookDelivery retryDelivery(UUID deliveryId) {
        WebhookDelivery delivery = deliveryRepository.findWithEndpointById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("WebhookDelivery", deliveryId));
        delivery.resetForManualRetry();
        deliveryRepository.save(delivery);
        return delivery;
    }

    public void dispatchNow(UUID deliveryId) {
        dispatchWorker.dispatchOne(deliveryId);
    }
}

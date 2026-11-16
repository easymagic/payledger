package com.payledger.web;

import com.payledger.service.WebhookAdminService;
import com.payledger.web.dto.WebhookDeliveryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/webhooks")
@Tag(name = "Admin Webhooks")
public class AdminWebhookController {

    private final WebhookAdminService webhookAdminService;

    public AdminWebhookController(WebhookAdminService webhookAdminService) {
        this.webhookAdminService = webhookAdminService;
    }

    @GetMapping("/dead-letters")
    @Operation(summary = "List dead-letter webhook deliveries")
    public Page<WebhookDeliveryResponse> deadLetters(@PageableDefault(size = 20) Pageable pageable) {
        return webhookAdminService.listDeadLetters(pageable).map(WebhookDeliveryResponse::from);
    }

    @PostMapping("/deliveries/{deliveryId}/retry")
    @Operation(summary = "Reset a delivery for retry and dispatch immediately")
    public WebhookDeliveryResponse retry(@PathVariable UUID deliveryId) {
        webhookAdminService.retryDelivery(deliveryId);
        webhookAdminService.dispatchNow(deliveryId);
        return WebhookDeliveryResponse.from(webhookAdminService.getDelivery(deliveryId));
    }
}

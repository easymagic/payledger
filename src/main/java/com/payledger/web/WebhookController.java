package com.payledger.web;

import com.payledger.service.WebhookAdminService;
import com.payledger.web.dto.WebhookEndpointRequest;
import com.payledger.web.dto.WebhookEndpointResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks")
@Tag(name = "Webhooks")
public class WebhookController {

    private final WebhookAdminService webhookAdminService;

    public WebhookController(WebhookAdminService webhookAdminService) {
        this.webhookAdminService = webhookAdminService;
    }

    @PostMapping("/endpoints")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a webhook endpoint for a merchant")
    public WebhookEndpointResponse register(@Valid @RequestBody WebhookEndpointRequest request) {
        return WebhookEndpointResponse.from(webhookAdminService.registerEndpoint(
                request.merchantId(),
                request.url(),
                request.secret(),
                request.eventTypes()));
    }
}

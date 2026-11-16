package com.payledger.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.payledger.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class WalletApiIntegrationTest extends AbstractIntegrationTest {

    @Test
    void provisionTransferAndSearch_endToEnd() throws Exception {
        String token = loginAdmin();
        String suffix = UUID.randomUUID().toString();

        MvcResult sourceResult = mockMvc.perform(post("/api/v1/wallets")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "merchantName": "API Merchant",
                                  "merchantEmail": "api-%s@example.com",
                                  "customerExternalRef": "c-src-%s",
                                  "customerName": "Source",
                                  "customerEmail": "src-%s@example.com",
                                  "currency": "NGN",
                                  "openingBalance": 200.0000
                                }
                                """.formatted(suffix, suffix, suffix)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        MvcResult destResult = mockMvc.perform(post("/api/v1/wallets")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "merchantName": "API Merchant Dest",
                                  "merchantEmail": "api-dest-%s@example.com",
                                  "customerExternalRef": "c-dst-%s",
                                  "customerName": "Dest",
                                  "customerEmail": "dst-%s@example.com",
                                  "currency": "NGN",
                                  "openingBalance": 0.0000
                                }
                                """.formatted(suffix, suffix, suffix)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode source = objectMapper.readTree(sourceResult.getResponse().getContentAsString());
        JsonNode dest = objectMapper.readTree(destResult.getResponse().getContentAsString());
        String sourceId = source.get("id").asText();
        String destId = dest.get("id").asText();
        String key = "api-idem-" + suffix;

        mockMvc.perform(post("/api/v1/transfers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceWalletId": "%s",
                                  "destinationWalletId": "%s",
                                  "amount": 75.0000,
                                  "currency": "NGN",
                                  "idempotencyKey": "%s",
                                  "description": "api transfer"
                                }
                                """.formatted(sourceId, destId, key)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.replayed").value(false))
                .andExpect(jsonPath("$.type").value("TRANSFER"));

        mockMvc.perform(post("/api/v1/transfers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceWalletId": "%s",
                                  "destinationWalletId": "%s",
                                  "amount": 75.0000,
                                  "currency": "NGN",
                                  "idempotencyKey": "%s",
                                  "description": "api transfer"
                                }
                                """.formatted(sourceId, destId, key)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replayed").value(true));

        mockMvc.perform(get("/api/v1/wallets/" + sourceId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance.amount").value(125.0));

        mockMvc.perform(get("/api/v1/transactions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("walletId", sourceId)
                        .param("type", "TRANSFER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("TRANSFER"));

        mockMvc.perform(get("/api/v1/transactions/wallets/" + sourceId + "/summaries")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idempotencyKey").value(key));

        mockMvc.perform(get("/api/v1/wallets/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource not found"));
    }

    @Test
    void validationFailure_returnsProblemDetails() throws Exception {
        String token = loginAdmin();
        MvcResult result = mockMvc.perform(post("/api/v1/transfers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceWalletId": null,
                                  "destinationWalletId": null,
                                  "amount": -1,
                                  "currency": "NG",
                                  "idempotencyKey": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("title").asText()).isEqualTo("Validation failed");
        assertThat(body.get("errors")).isNotNull();
    }

    @Test
    void unauthenticatedWrite_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}

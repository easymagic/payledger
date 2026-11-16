package com.payledger.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.payledger.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class SecurityIntegrationTest extends AbstractIntegrationTest {

    @Test
    void merchantCannotAccessAnotherMerchantsWallet() throws Exception {
        String adminToken = loginAdmin();
        String suffix = UUID.randomUUID().toString();

        MvcResult foreignWallet = mockMvc.perform(post("/api/v1/wallets")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "merchantName": "Foreign",
                                  "merchantEmail": "foreign-%s@example.com",
                                  "customerExternalRef": "f-%s",
                                  "customerName": "Foreign",
                                  "customerEmail": "f-%s@example.com",
                                  "currency": "NGN",
                                  "openingBalance": 10.0000
                                }
                                """.formatted(suffix, suffix, suffix)))
                .andExpect(status().isCreated())
                .andReturn();
        String walletId = objectMapper.readTree(foreignWallet.getResponse().getContentAsString())
                .get("id").asText();

        String merchantToken = loginMerchant();
        mockMvc.perform(get("/api/v1/wallets/" + walletId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(merchantToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Forbidden"));
    }

    @Test
    void adminCanAccessAnyWallet() throws Exception {
        String adminToken = loginAdmin();
        String suffix = UUID.randomUUID().toString();
        MvcResult wallet = mockMvc.perform(post("/api/v1/wallets")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "merchantName": "Any",
                                  "merchantEmail": "any-%s@example.com",
                                  "customerExternalRef": "a-%s",
                                  "customerName": "Any",
                                  "customerEmail": "a-%s@example.com",
                                  "currency": "NGN",
                                  "openingBalance": 5.0000
                                }
                                """.formatted(suffix, suffix, suffix)))
                .andExpect(status().isCreated())
                .andReturn();
        String walletId = objectMapper.readTree(wallet.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/v1/wallets/" + walletId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(walletId));
    }

    @Test
    void loginReturnsJwtWithRole() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"admin-change-me"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        org.assertj.core.api.Assertions.assertThat(body.get("accessToken").asText()).isNotBlank();
    }
}

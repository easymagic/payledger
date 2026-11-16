package com.payledger.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProvisionWalletRequest(
        @NotBlank @Size(max = 200) String merchantName,
        @NotBlank @Email String merchantEmail,
        @NotBlank @Size(max = 100) String customerExternalRef,
        @NotBlank @Size(max = 200) String customerName,
        @NotBlank @Email String customerEmail,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotNull @DecimalMin("0.0000") BigDecimal openingBalance
) {
}

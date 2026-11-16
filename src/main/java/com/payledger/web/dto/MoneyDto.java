package com.payledger.web.dto;

import com.payledger.domain.Money;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MoneyDto(
        @NotNull @DecimalMin(value = "0.0001", inclusive = true) BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) String currency
) {
    public static MoneyDto from(Money money) {
        return new MoneyDto(money.getAmount(), money.getCurrency());
    }
}

package com.payledger.web;

import com.payledger.service.TransferCommand;
import com.payledger.service.TransferService;
import com.payledger.service.WalletService;
import com.payledger.web.dto.MerchantBalanceResponse;
import com.payledger.web.dto.ProvisionWalletRequest;
import com.payledger.web.dto.TransferRequest;
import com.payledger.web.dto.TransferResponse;
import com.payledger.web.dto.WalletResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Wallets & Transfers")
public class WalletController {

    private final WalletService walletService;
    private final TransferService transferService;

    public WalletController(WalletService walletService, TransferService transferService) {
        this.walletService = walletService;
        this.transferService = transferService;
    }

    @PostMapping("/wallets")
    @Operation(summary = "Provision a merchant/customer wallet (demo bootstrap)")
    public ResponseEntity<WalletResponse> provision(@Valid @RequestBody ProvisionWalletRequest request) {
        var result = walletService.provisionWallet(
                request.merchantName(),
                request.merchantEmail(),
                request.customerExternalRef(),
                request.customerName(),
                request.customerEmail(),
                request.currency(),
                request.openingBalance()
        );
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(WalletResponse.from(result.wallet()));
    }

    @GetMapping("/wallets/{walletId}")
    @Operation(summary = "Get wallet by id")
    public WalletResponse getWallet(@PathVariable UUID walletId) {
        return WalletResponse.from(walletService.getWallet(walletId));
    }

    @GetMapping("/merchants/{merchantId}/balance")
    @Operation(summary = "Sum wallet balances for a merchant (JPQL aggregate)")
    public MerchantBalanceResponse merchantBalance(
            @PathVariable UUID merchantId,
            @RequestParam(defaultValue = "NGN") String currency) {
        return new MerchantBalanceResponse(
                merchantId,
                currency.toUpperCase(),
                walletService.sumMerchantBalance(merchantId, currency));
    }

    @PostMapping("/transfers")
    @Operation(summary = "Transfer funds between wallets (idempotent)")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
        var result = transferService.transfer(new TransferCommand(
                request.sourceWalletId(),
                request.destinationWalletId(),
                request.amount(),
                request.currency(),
                request.idempotencyKey(),
                request.description()
        ));
        HttpStatus status = result.replayed() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(TransferResponse.from(result));
    }
}

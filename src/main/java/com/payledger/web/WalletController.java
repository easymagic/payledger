package com.payledger.web;

import com.payledger.domain.LedgerTransaction;
import com.payledger.domain.TransactionType;
import com.payledger.exception.ResourceNotFoundException;
import com.payledger.repository.MerchantRepository;
import com.payledger.repository.TransactionRepository;
import com.payledger.security.MerchantAccessService;
import com.payledger.security.PayLedgerUserPrincipal;
import com.payledger.service.RefundCommand;
import com.payledger.service.RefundService;
import com.payledger.service.TransferCommand;
import com.payledger.service.TransferService;
import com.payledger.service.WalletService;
import com.payledger.web.dto.MerchantBalanceResponse;
import com.payledger.web.dto.ProvisionWalletRequest;
import com.payledger.web.dto.RefundRequest;
import com.payledger.web.dto.RefundResponse;
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
@Tag(name = "Wallets, Transfers & Refunds")
public class WalletController {

    private final WalletService walletService;
    private final TransferService transferService;
    private final RefundService refundService;
    private final MerchantAccessService merchantAccessService;
    private final MerchantRepository merchantRepository;
    private final TransactionRepository transactionRepository;

    public WalletController(
            WalletService walletService,
            TransferService transferService,
            RefundService refundService,
            MerchantAccessService merchantAccessService,
            MerchantRepository merchantRepository,
            TransactionRepository transactionRepository) {
        this.walletService = walletService;
        this.transferService = transferService;
        this.refundService = refundService;
        this.merchantAccessService = merchantAccessService;
        this.merchantRepository = merchantRepository;
        this.transactionRepository = transactionRepository;
    }

    @PostMapping("/wallets")
    @Operation(summary = "Provision a merchant/customer wallet (demo bootstrap)")
    public ResponseEntity<WalletResponse> provision(@Valid @RequestBody ProvisionWalletRequest request) {
        PayLedgerUserPrincipal principal = merchantAccessService.requirePrincipal();
        if (!principal.isAdmin()) {
            var merchant = merchantRepository.findById(principal.getMerchantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Merchant", principal.getMerchantId()));
            if (!merchant.getEmail().equalsIgnoreCase(request.merchantEmail())) {
                throw new com.payledger.exception.AccessDeniedBusinessException(
                        "Merchants may only provision wallets under their own merchant email");
            }
        }

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
        return WalletResponse.from(merchantAccessService.requireWalletAccess(walletId));
    }

    @GetMapping("/merchants/{merchantId}/balance")
    @Operation(summary = "Sum wallet balances for a merchant (cached aggregate)")
    public MerchantBalanceResponse merchantBalance(
            @PathVariable UUID merchantId,
            @RequestParam(defaultValue = "NGN") String currency) {
        merchantAccessService.requireMerchantAccess(merchantId);
        return new MerchantBalanceResponse(
                merchantId,
                currency.toUpperCase(),
                walletService.sumMerchantBalance(merchantId, currency));
    }

    @PostMapping("/transfers")
    @Operation(summary = "Transfer funds between wallets (idempotent)")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
        merchantAccessService.requireWalletAccess(request.sourceWalletId());
        merchantAccessService.requireWalletAccess(request.destinationWalletId());
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

    @PostMapping("/refunds")
    @Operation(summary = "Refund a completed transfer (idempotent, partial allowed)")
    public ResponseEntity<RefundResponse> refund(@Valid @RequestBody RefundRequest request) {
        LedgerTransaction original = transactionRepository.findDetailedById(request.originalTransactionId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", request.originalTransactionId()));
        if (original.getType() != TransactionType.TRANSFER) {
            throw new IllegalArgumentException("Only TRANSFER transactions can be refunded");
        }
        merchantAccessService.requireWalletAccess(original.getWallet().getId());
        if (original.getCounterpartyWallet() != null) {
            merchantAccessService.requireWalletAccess(original.getCounterpartyWallet().getId());
        }

        var result = refundService.refund(new RefundCommand(
                request.originalTransactionId(),
                request.amount(),
                request.currency(),
                request.idempotencyKey(),
                request.description()
        ));
        HttpStatus status = result.replayed() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(RefundResponse.from(result));
    }
}

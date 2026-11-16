package com.payledger.web;

import com.payledger.security.MerchantAccessService;
import com.payledger.service.ReconciliationService;
import com.payledger.web.dto.ReconciliationReportResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports")
public class ReportController {

    private final ReconciliationService reconciliationService;
    private final MerchantAccessService merchantAccessService;

    public ReportController(
            ReconciliationService reconciliationService,
            MerchantAccessService merchantAccessService) {
        this.reconciliationService = reconciliationService;
        this.merchantAccessService = merchantAccessService;
    }

    @GetMapping("/reconciliation")
    @Operation(summary = "Reconciliation report per wallet for a merchant and period")
    public ReconciliationReportResponse reconciliation(
            @RequestParam UUID merchantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        merchantAccessService.requireMerchantAccess(merchantId);
        return reconciliationService.report(merchantId, from, to);
    }
}

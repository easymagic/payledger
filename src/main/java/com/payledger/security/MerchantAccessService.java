package com.payledger.security;

import com.payledger.domain.Wallet;
import com.payledger.exception.AccessDeniedBusinessException;
import com.payledger.exception.ResourceNotFoundException;
import com.payledger.repository.WalletRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class MerchantAccessService {

    private final WalletRepository walletRepository;

    public MerchantAccessService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    public PayLedgerUserPrincipal requirePrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof PayLedgerUserPrincipal principal)) {
            throw new AccessDeniedBusinessException("Authentication required");
        }
        return principal;
    }

    public void requireMerchantAccess(UUID merchantId) {
        PayLedgerUserPrincipal principal = requirePrincipal();
        if (principal.isAdmin()) {
            return;
        }
        if (principal.getMerchantId() == null || !principal.getMerchantId().equals(merchantId)) {
            throw new AccessDeniedBusinessException("Merchant scope does not allow access to merchant " + merchantId);
        }
    }

    @Transactional(readOnly = true)
    public Wallet requireWalletAccess(UUID walletId) {
        Wallet wallet = walletRepository.findWithCustomerById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", walletId));
        requireMerchantAccess(wallet.getCustomer().getMerchant().getId());
        return wallet;
    }
}

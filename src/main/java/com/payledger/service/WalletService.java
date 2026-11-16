package com.payledger.service;

import com.payledger.domain.Customer;
import com.payledger.domain.Merchant;
import com.payledger.domain.Money;
import com.payledger.domain.Wallet;
import com.payledger.exception.ResourceNotFoundException;
import com.payledger.repository.CustomerRepository;
import com.payledger.repository.MerchantRepository;
import com.payledger.repository.WalletRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final CustomerRepository customerRepository;
    private final MerchantRepository merchantRepository;

    public WalletService(
            WalletRepository walletRepository,
            CustomerRepository customerRepository,
            MerchantRepository merchantRepository) {
        this.walletRepository = walletRepository;
        this.customerRepository = customerRepository;
        this.merchantRepository = merchantRepository;
    }

    @Transactional(readOnly = true)
    public Wallet getWallet(UUID walletId) {
        return walletRepository.findWithCustomerById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", walletId));
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "merchantBalances", key = "#merchantId + ':' + #currency")
    public BigDecimal sumMerchantBalance(UUID merchantId, String currency) {
        if (!merchantRepository.existsById(merchantId)) {
            throw new ResourceNotFoundException("Merchant", merchantId);
        }
        return walletRepository.sumBalanceByMerchantAndCurrency(merchantId, currency.toUpperCase());
    }

    /**
     * Demo bootstrap: merchant → customer → wallet. Prefer admin for cross-merchant provisioning.
     */
    @Transactional
    public WalletProvisionResult provisionWallet(
            String merchantName,
            String merchantEmail,
            String customerExternalRef,
            String customerName,
            String customerEmail,
            String currency,
            BigDecimal openingBalance) {

        Merchant merchant = merchantRepository.findByEmail(merchantEmail)
                .orElseGet(() -> merchantRepository.save(new Merchant(merchantName, merchantEmail)));

        Customer customer = customerRepository
                .findByMerchantIdAndExternalRef(merchant.getId(), customerExternalRef)
                .orElseGet(() -> customerRepository.save(
                        new Customer(merchant, customerExternalRef, customerName, customerEmail)));

        return walletRepository.findByCustomerId(customer.getId())
                .map(existing -> new WalletProvisionResult(merchant, customer, existing, false))
                .orElseGet(() -> {
                    Wallet wallet = walletRepository.save(
                            new Wallet(customer, Money.of(openingBalance, currency)));
                    return new WalletProvisionResult(merchant, customer, wallet, true);
                });
    }

    public record WalletProvisionResult(Merchant merchant, Customer customer, Wallet wallet, boolean created) {
    }
}

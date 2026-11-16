package com.payledger.security;

import com.payledger.domain.AppUser;
import com.payledger.domain.Merchant;
import com.payledger.domain.UserRole;
import com.payledger.repository.AppUserRepository;
import com.payledger.repository.MerchantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds demo users for local study. Passwords come from env (defaults are for demos only).
 */
@Component
public class DemoUserBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoUserBootstrap.class);

    private final AppUserRepository appUserRepository;
    private final MerchantRepository merchantRepository;
    private final PasswordEncoder passwordEncoder;
    private final DemoUserProperties properties;

    public DemoUserBootstrap(
            AppUserRepository appUserRepository,
            MerchantRepository merchantRepository,
            PasswordEncoder passwordEncoder,
            DemoUserProperties properties) {
        this.appUserRepository = appUserRepository;
        this.merchantRepository = merchantRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }

        if (!appUserRepository.existsByUsername(properties.getAdminUsername())) {
            appUserRepository.save(new AppUser(
                    properties.getAdminUsername(),
                    passwordEncoder.encode(properties.getAdminPassword()),
                    UserRole.ADMIN,
                    null));
            log.info("Seeded demo ADMIN user '{}'", properties.getAdminUsername());
        }

        if (!appUserRepository.existsByUsername(properties.getMerchantUsername())) {
            Merchant merchant = merchantRepository.findByEmail(properties.getMerchantEmail())
                    .orElseGet(() -> merchantRepository.save(
                            new Merchant(properties.getMerchantName(), properties.getMerchantEmail())));
            appUserRepository.save(new AppUser(
                    properties.getMerchantUsername(),
                    passwordEncoder.encode(properties.getMerchantPassword()),
                    UserRole.MERCHANT,
                    merchant));
            log.info("Seeded demo MERCHANT user '{}' for merchant {}",
                    properties.getMerchantUsername(), merchant.getId());
        }
    }
}

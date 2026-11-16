package com.payledger.security;

import com.payledger.repository.AppUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayLedgerUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    public PayLedgerUserDetailsService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return appUserRepository.findByUsername(username)
                .map(user -> {
                    // Touch merchant id while session is open
                    if (user.getMerchant() != null) {
                        user.getMerchant().getId();
                    }
                    return new PayLedgerUserPrincipal(user);
                })
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}

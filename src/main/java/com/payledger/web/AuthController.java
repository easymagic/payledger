package com.payledger.web;

import com.payledger.security.JwtProperties;
import com.payledger.security.JwtService;
import com.payledger.security.PayLedgerUserPrincipal;
import com.payledger.web.dto.LoginRequest;
import com.payledger.web.dto.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            JwtProperties jwtProperties) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    @PostMapping("/login")
    @Operation(summary = "Exchange username/password for a JWT")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        PayLedgerUserPrincipal principal = (PayLedgerUserPrincipal) authentication.getPrincipal();
        String token = jwtService.createToken(principal);
        return new LoginResponse(
                token,
                "Bearer",
                principal.getRole().name(),
                principal.getMerchantId(),
                jwtProperties.getExpiryMinutes());
    }
}

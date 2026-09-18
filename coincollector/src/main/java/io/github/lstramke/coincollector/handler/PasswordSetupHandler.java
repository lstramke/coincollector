package io.github.lstramke.coincollector.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.github.lstramke.coincollector.model.DTOs.Requests.PasswordSetupRequest;
import io.github.lstramke.coincollector.services.UserStorageService;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/v1/password")
public class PasswordSetupHandler {
    
    private final static Logger logger = LoggerFactory.getLogger(PasswordSetupHandler.class);
    private final UserStorageService userStorageService;
    private final PasswordEncoder passwordEncoder;

    public PasswordSetupHandler(UserStorageService userStorageService, PasswordEncoder passwordEncoder) {
        this.userStorageService = userStorageService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/setup")
    public ResponseEntity<?> passwordSetup(@Valid @RequestBody PasswordSetupRequest request) {
        logger.info("Password setup attempt for user {}", request.username());

        var user = this.userStorageService.getByUsername(request.username());

        if (user.getPasswordHash() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Password is already set");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userStorageService.update(user);

        return ResponseEntity.noContent().build();
    }
}

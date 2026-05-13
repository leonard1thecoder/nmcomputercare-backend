package com.backend.nmcomputercare.subscribe.controller;

import com.backend.nmcomputercare.subscribe.dtos.VerificationRequest;
import com.backend.nmcomputercare.subscribe.dtos.VerificationResponse;
import com.backend.nmcomputercare.subscribe.service.VerificationService;
import com.backend.nmcomputercare.utils.ExceptionAdvice;
import com.backend.nmcomputercare.utils.ExecService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutorService;

@RestController
@RequestMapping("/api/subscriptions")
public class VerificationController extends ExecService {

    @Value("${app.frontend.base-url:https://nmcomputercare.co.za}")
    private String frontendBaseUrl;

    private final VerificationService verificationService;

    public VerificationController(VerificationService verificationService,
                                  ExecutorService controllerExecutorService,
                                  ExceptionAdvice advice) {
        super(controllerExecutorService, advice);
        this.verificationService = verificationService;
    }

    @GetMapping("/verify")
    public ResponseEntity<Void> verify(@RequestParam("token") String token) {
        @SuppressWarnings("unchecked")
        List<VerificationResponse> response =
                (List<VerificationResponse>) exec(
                        verificationService,
                        "verifySubscription",
                        VerificationRequest.builder().token(token).build());

        VerificationResponse confirmed = response.isEmpty() ? null : response.get(0);
        String redirectUrl = normalizedFrontendBaseUrl() + "/subscription/confirmed"
                + "?name=" + encode(confirmed != null ? confirmed.getName() : null);

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build();
    }

    private String encode(String value) {
        if (value == null) {
            return "";
        }
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    private String normalizedFrontendBaseUrl() {
        if (frontendBaseUrl == null || frontendBaseUrl.isBlank()) {
            return "https://nmcomputercare.co.za";
        }
        return frontendBaseUrl.endsWith("/")
                ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1)
                : frontendBaseUrl;
    }
}

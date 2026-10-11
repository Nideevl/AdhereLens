package com.adherelens.notification.web;

import com.adherelens.auth.AuthUser;
import com.adherelens.notification.domain.AlertStatus;
import com.adherelens.notification.dto.AlertResponse;
import com.adherelens.notification.service.AlertService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('NURSE', 'DOCTOR')")
    public List<AlertResponse> list(@AuthenticationPrincipal AuthUser caller,
                                    @RequestParam(defaultValue = "OPEN") AlertStatus status) {
        return alertService.list(caller.hospitalId(), status);
    }
}

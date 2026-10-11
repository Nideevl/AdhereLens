package com.adherelens.identity.web;

import com.adherelens.auth.AuthUser;
import com.adherelens.identity.dto.CreateStaffRequest;
import com.adherelens.identity.dto.StaffResponse;
import com.adherelens.identity.service.StaffService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/staff")
@PreAuthorize("hasRole('HOSPITAL_ADMIN')")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StaffResponse create(@AuthenticationPrincipal AuthUser caller,
                                @Valid @RequestBody CreateStaffRequest request) {
        return staffService.create(caller.hospitalId(), request);
    }

    @GetMapping
    public List<StaffResponse> list(@AuthenticationPrincipal AuthUser caller) {
        return staffService.listForHospital(caller.hospitalId());
    }
}

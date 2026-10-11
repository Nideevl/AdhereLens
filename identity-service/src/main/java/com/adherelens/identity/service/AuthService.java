package com.adherelens.identity.service;

import com.adherelens.identity.domain.Hospital;
import com.adherelens.identity.domain.HospitalStatus;
import com.adherelens.identity.domain.Staff;
import com.adherelens.identity.dto.LoginRequest;
import com.adherelens.identity.dto.LoginResponse;
import com.adherelens.identity.repository.HospitalRepository;
import com.adherelens.identity.repository.StaffRepository;
import com.adherelens.identity.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final String BAD_CREDENTIALS = "Invalid email or password";

    private final StaffRepository staffRepository;
    private final HospitalRepository hospitalRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(StaffRepository staffRepository,
                       HospitalRepository hospitalRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.staffRepository = staffRepository;
        this.hospitalRepository = hospitalRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        Staff staff = staffRepository.findByEmail(request.email().trim().toLowerCase())
                .filter(Staff::isActive)
                .filter(s -> passwordEncoder.matches(request.password(), s.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException(BAD_CREDENTIALS));

        if (staff.getHospitalId() != null) {
            Hospital hospital = hospitalRepository.findById(staff.getHospitalId())
                    .orElseThrow(() -> new UnauthorizedException(BAD_CREDENTIALS));
            if (hospital.getStatus() != HospitalStatus.ACTIVE) {
                throw new UnauthorizedException(BAD_CREDENTIALS);
            }
        }

        return new LoginResponse(jwtService.issue(staff), staff.getRole(), staff.getHospitalId(), staff.getFullName());
    }
}

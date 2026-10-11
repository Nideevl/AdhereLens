package com.adherelens.identity.service;

import com.adherelens.common.model.Role;
import com.adherelens.identity.domain.Staff;
import com.adherelens.identity.dto.CreateStaffRequest;
import com.adherelens.identity.dto.StaffResponse;
import com.adherelens.identity.repository.StaffRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class StaffService {

    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;

    public StaffService(StaffRepository staffRepository, PasswordEncoder passwordEncoder) {
        this.staffRepository = staffRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** hospitalId always comes from the caller's verified token, never from the request body. */
    public StaffResponse create(UUID hospitalId, CreateStaffRequest request) {
        if (request.role() != Role.DOCTOR && request.role() != Role.NURSE) {
            throw new IllegalArgumentException("Role must be DOCTOR or NURSE");
        }
        String email = request.email().trim().toLowerCase();
        if (staffRepository.existsByEmail(email)) {
            throw new ConflictException("Email already in use");
        }

        Staff staff = new Staff();
        staff.setHospitalId(hospitalId);
        staff.setEmail(email);
        staff.setPasswordHash(passwordEncoder.encode(request.password()));
        staff.setFullName(request.fullName().trim());
        staff.setRole(request.role());
        staff.setSpecialty(request.specialty());
        return toResponse(staffRepository.save(staff));
    }

    public List<StaffResponse> listForHospital(UUID hospitalId) {
        return staffRepository.findByHospitalIdOrderByFullName(hospitalId).stream()
                .map(this::toResponse)
                .toList();
    }

    private StaffResponse toResponse(Staff s) {
        return new StaffResponse(s.getId(), s.getEmail(), s.getFullName(), s.getRole(), s.getSpecialty(), s.isActive());
    }
}

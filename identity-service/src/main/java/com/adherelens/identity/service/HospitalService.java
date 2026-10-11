package com.adherelens.identity.service;

import com.adherelens.common.model.Role;
import com.adherelens.identity.domain.Hospital;
import com.adherelens.identity.domain.HospitalStatus;
import com.adherelens.identity.domain.Staff;
import com.adherelens.identity.dto.CreateHospitalRequest;
import com.adherelens.identity.dto.CreateHospitalResponse;
import com.adherelens.identity.dto.HospitalResponse;
import com.adherelens.identity.repository.HospitalRepository;
import com.adherelens.identity.repository.StaffRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@Service
public class HospitalService {

    private final HospitalRepository hospitalRepository;
    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public HospitalService(HospitalRepository hospitalRepository,
                           StaffRepository staffRepository,
                           PasswordEncoder passwordEncoder) {
        this.hospitalRepository = hospitalRepository;
        this.staffRepository = staffRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CreateHospitalResponse create(CreateHospitalRequest request) {
        String adminEmail = request.adminEmail().trim().toLowerCase();
        if (hospitalRepository.existsByCode(request.code())) {
            throw new ConflictException("Hospital code already exists");
        }
        if (staffRepository.existsByEmail(adminEmail)) {
            throw new ConflictException("Email already in use");
        }

        String apiKey = generateApiKey();

        Hospital hospital = new Hospital();
        hospital.setCode(request.code());
        hospital.setName(request.name().trim());
        hospital.setStatus(HospitalStatus.ACTIVE);
        hospital.setApiKeyHash(passwordEncoder.encode(apiKey));
        hospital = hospitalRepository.save(hospital);

        Staff admin = new Staff();
        admin.setHospitalId(hospital.getId());
        admin.setEmail(adminEmail);
        admin.setPasswordHash(passwordEncoder.encode(request.adminPassword()));
        admin.setFullName(request.adminFullName().trim());
        admin.setRole(Role.HOSPITAL_ADMIN);
        admin = staffRepository.save(admin);

        return new CreateHospitalResponse(hospital.getId(), hospital.getCode(), hospital.getName(), apiKey, admin.getId());
    }

    public List<HospitalResponse> list() {
        return hospitalRepository.findAll().stream()
                .map(h -> new HospitalResponse(h.getId(), h.getCode(), h.getName(), h.getStatus()))
                .toList();
    }

    private String generateApiKey() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return "alk_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

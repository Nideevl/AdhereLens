package com.adherelens.identity.repository;

import com.adherelens.common.model.Role;
import com.adherelens.identity.domain.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StaffRepository extends JpaRepository<Staff, UUID> {

    Optional<Staff> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);

    List<Staff> findByHospitalIdOrderByFullName(UUID hospitalId);
}

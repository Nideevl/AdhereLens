package com.adherelens.identity.repository;

import com.adherelens.identity.domain.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HospitalRepository extends JpaRepository<Hospital, UUID> {

    boolean existsByCode(String code);
}

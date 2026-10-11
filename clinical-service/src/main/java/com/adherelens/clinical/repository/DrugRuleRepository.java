package com.adherelens.clinical.repository;

import com.adherelens.clinical.domain.DrugRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DrugRuleRepository extends JpaRepository<DrugRule, UUID> {

    List<DrugRule> findByHospitalIdAndActiveTrueOrderByDrugNameAscMinAgeYearsAsc(UUID hospitalId);

    List<DrugRule> findByHospitalIdAndDrugNameAndActiveTrueOrderByMinAgeYearsAsc(UUID hospitalId, String drugName);

    Optional<DrugRule> findByIdAndHospitalId(UUID id, UUID hospitalId);
}

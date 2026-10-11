package com.adherelens.notification.repository;

import com.adherelens.notification.domain.SlotOutcome;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SlotOutcomeRepository extends JpaRepository<SlotOutcome, UUID> {
}

package com.iquenobot.plan.domain.repository;

import com.iquenobot.plan.domain.entity.Plan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanRepository extends JpaRepository<Plan, UUID> {

    Page<Plan> findByDeletedFalse(Pageable pageable);

    Optional<Plan> findByIdAndDeletedFalse(UUID id);

    Optional<Plan> findByCodeAndDeletedFalse(String code);

    Optional<Plan> findByCode(String code);

    boolean existsByCode(String code);
}

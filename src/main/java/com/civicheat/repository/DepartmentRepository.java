package com.civicheat.repository;

import com.civicheat.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing municipal Department entity persistence.
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    Optional<Department> findByName(String name);

    Optional<Department> findByCode(String code);

    boolean existsByName(String name);

    boolean existsByCode(String code);
}

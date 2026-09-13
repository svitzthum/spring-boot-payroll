package dev.svitzthum.payroll.workinghours.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface EmployeeJpaRepository extends JpaRepository<EmployeeEntity, UUID> {

	Optional<EmployeeEntity> findByExternalEmployeeRef(String externalEmployeeRef);

}

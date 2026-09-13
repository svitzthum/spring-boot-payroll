package dev.svitzthum.payroll.workinghours.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface TimeTrackingImportJpaRepository extends JpaRepository<TimeTrackingImportEntity, UUID> {

	boolean existsByExternalEventId(String externalEventId);

	Optional<TimeTrackingImportEntity> findByExternalEventId(String externalEventId);

}


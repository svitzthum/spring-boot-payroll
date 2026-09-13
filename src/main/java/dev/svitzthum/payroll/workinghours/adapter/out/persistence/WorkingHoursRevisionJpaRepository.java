package dev.svitzthum.payroll.workinghours.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface WorkingHoursRevisionJpaRepository extends JpaRepository<WorkingHoursRevisionEntity, UUID> {

	/** Returns the history of the monthly value, oldest first. */
	List<WorkingHoursRevisionEntity> findByWorkingHoursIdOrderByChangedAt(UUID workingHoursId);

}


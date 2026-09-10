package dev.svitzthum.payroll.workinghours.adapter.out.persistence;

import dev.svitzthum.payroll.workinghours.domain.MonthlyWorkingHours;
import dev.svitzthum.payroll.workinghours.domain.WorkDuration;

/**
 * Translates between the entity and the domain aggregate. Keeping this in one place is
 * what allows the domain to stay free of JPA annotations (ADR 0005).
 */
final class WorkingHoursMapper {

	private WorkingHoursMapper() {
	}

	static MonthlyWorkingHours toDomain(MonthlyWorkingHoursEntity entity) {
		return MonthlyWorkingHours.restore(entity.getEmployeeId(), entity.getPeriod(),
				WorkDuration.ofMinutes(entity.getMinutesWorked()), entity.getLastSource(), entity.getVersion());
	}

	static void applyTo(MonthlyWorkingHours workingHours, MonthlyWorkingHoursEntity entity) {
		entity.setMinutesWorked(workingHours.workedTime().minutes());
		entity.setLastSource(workingHours.source());
	}

}


package dev.svitzthum.payroll.workinghours.application.port.out;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.svitzthum.payroll.workinghours.domain.MonthlyWorkingHours;

public interface WorkingHoursRepository {

	Optional<MonthlyWorkingHours> find(UUID employeeId, YearMonth period);

	/**
	 * Returns all months of the given year, ordered by period ascending.
	 */
	List<MonthlyWorkingHours> findByYear(UUID employeeId, int year);

	/**
	 * Stores the aggregate and returns it with the version it now has.
	 * @throws dev.svitzthum.payroll.workinghours.application.WorkingHoursConflictException
	 * if another writer changed the same employee and period in the meantime
	 */
	MonthlyWorkingHours save(MonthlyWorkingHours workingHours);

}


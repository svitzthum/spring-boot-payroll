package dev.svitzthum.payroll.workinghours.application.port.in;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.svitzthum.payroll.workinghours.domain.MonthlyWorkingHours;

/**
 * Reads recorded working time. Both methods fail for an unknown employee so that a
 * caller can tell "employee does not exist" from "nothing recorded yet".
 */
public interface GetWorkingHoursQuery {

	Optional<MonthlyWorkingHours> findWorkingHours(UUID employeeId, YearMonth period);

	List<MonthlyWorkingHours> findWorkingHoursOfYear(UUID employeeId, int year);

}


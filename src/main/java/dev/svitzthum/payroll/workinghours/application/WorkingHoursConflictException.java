package dev.svitzthum.payroll.workinghours.application;

import java.time.YearMonth;
import java.util.UUID;

/**
 * Another writer changed the same employee and period concurrently. Raised by the
 * repository port, either from optimistic locking or from the unique constraint on
 * (employee, period). See ADR 0004.
 */
public class WorkingHoursConflictException extends RuntimeException {

	public WorkingHoursConflictException(UUID employeeId, YearMonth period, Throwable cause) {
		super("working hours of employee %s for %s were changed concurrently".formatted(employeeId, period), cause);
	}

}


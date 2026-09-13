package dev.svitzthum.payroll.workinghours.application;

import java.time.YearMonth;
import java.util.UUID;

/**
 * The month was entered manually, so the time tracking import may not replace it
 * (ADR 0006).
 */
public class ManualEntryTakesPrecedenceException extends RuntimeException {

	private final UUID employeeId;

	private final YearMonth period;

	public ManualEntryTakesPrecedenceException(UUID employeeId, YearMonth period) {
		super("working hours of employee %s for %s were entered manually and are not replaced by the import"
			.formatted(employeeId, period));
		this.employeeId = employeeId;
		this.period = period;
	}

	public UUID employeeId() {
		return this.employeeId;
	}

	public YearMonth period() {
		return this.period;
	}

}


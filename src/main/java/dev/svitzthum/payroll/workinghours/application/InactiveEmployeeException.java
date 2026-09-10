package dev.svitzthum.payroll.workinghours.application;

import java.util.UUID;

/** The employee exists but has left the company, so no time may be recorded any more. */
public class InactiveEmployeeException extends RuntimeException {

	private final UUID employeeId;

	public InactiveEmployeeException(UUID employeeId) {
		super("employee %s is not active".formatted(employeeId));
		this.employeeId = employeeId;
	}

	public UUID employeeId() {
		return this.employeeId;
	}

}


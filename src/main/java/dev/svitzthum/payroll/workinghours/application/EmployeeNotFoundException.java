package dev.svitzthum.payroll.workinghours.application;

import java.util.UUID;

/** The employee does not exist. */
public class EmployeeNotFoundException extends RuntimeException {

	private final UUID employeeId;

	public EmployeeNotFoundException(UUID employeeId) {
		super("employee %s does not exist".formatted(employeeId));
		this.employeeId = employeeId;
	}

	public UUID employeeId() {
		return this.employeeId;
	}

}


package dev.svitzthum.payroll.workinghours.application.port.out;

import java.util.Optional;
import java.util.UUID;

/**
 * The master data this feature needs about an employee: does the employee exist, and may
 * working time still be recorded for them.
 */
public interface EmployeeDirectory {

	Optional<Employee> find(UUID employeeId);

	record Employee(UUID id, boolean active) {
	}

}


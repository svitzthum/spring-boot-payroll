package dev.svitzthum.payroll.workinghours.application.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import dev.svitzthum.payroll.workinghours.application.port.out.EmployeeDirectory;

class InMemoryEmployeeDirectory implements EmployeeDirectory {

	private final Map<UUID, Employee> employees = new HashMap<>();

	@Override
	public Optional<Employee> find(UUID employeeId) {
		return Optional.ofNullable(this.employees.get(employeeId));
	}

	UUID addActiveEmployee() {
		return add(true);
	}

	UUID addInactiveEmployee() {
		return add(false);
	}

	private UUID add(boolean active) {
		UUID id = UUID.randomUUID();
		this.employees.put(id, new Employee(id, active));
		return id;
	}

}


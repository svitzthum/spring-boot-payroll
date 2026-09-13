package dev.svitzthum.payroll.workinghours.application.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import dev.svitzthum.payroll.workinghours.application.port.out.EmployeeDirectory;

class InMemoryEmployeeDirectory implements EmployeeDirectory {

	private final Map<UUID, Employee> employees = new HashMap<>();

	private final Map<String, UUID> externalReferences = new HashMap<>();

	@Override
	public Optional<Employee> find(UUID employeeId) {
		return Optional.ofNullable(this.employees.get(employeeId));
	}

	@Override
	public Optional<Employee> findByExternalReference(String externalEmployeeRef) {
		return Optional.ofNullable(this.externalReferences.get(externalEmployeeRef)).flatMap(this::find);
	}

	UUID addActiveEmployee() {
		return add(true);
	}

	UUID addInactiveEmployee() {
		return add(false);
	}

	UUID addActiveEmployee(String externalEmployeeRef) {
		UUID id = add(true);
		this.externalReferences.put(externalEmployeeRef, id);
		return id;
	}

	private UUID add(boolean active) {
		UUID id = UUID.randomUUID();
		this.employees.put(id, new Employee(id, active));
		return id;
	}

}

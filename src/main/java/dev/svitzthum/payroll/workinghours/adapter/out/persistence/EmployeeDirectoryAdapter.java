package dev.svitzthum.payroll.workinghours.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import dev.svitzthum.payroll.workinghours.application.port.out.EmployeeDirectory;

import org.springframework.stereotype.Component;

@Component
class EmployeeDirectoryAdapter implements EmployeeDirectory {

	private final EmployeeJpaRepository employees;

	EmployeeDirectoryAdapter(EmployeeJpaRepository employees) {
		this.employees = employees;
	}

	@Override
	public Optional<Employee> find(UUID employeeId) {
		return this.employees.findById(employeeId).map(EmployeeDirectoryAdapter::toEmployee);
	}

	@Override
	public Optional<Employee> findByExternalReference(String externalEmployeeRef) {
		return this.employees.findByExternalEmployeeRef(externalEmployeeRef).map(EmployeeDirectoryAdapter::toEmployee);
	}

	private static Employee toEmployee(EmployeeEntity entity) {
		return new Employee(entity.getId(), entity.isActive());
	}

}

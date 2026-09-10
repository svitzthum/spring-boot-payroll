package dev.svitzthum.payroll.workinghours.adapter.out.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Employee master data. Iteration 1 only reads it, so there is no write side and no
 * association to the working hours entity — the relation is kept by id.
 */
@Entity
@Table(name = "employee")
class EmployeeEntity {

	@Id
	private UUID id;

	@Column(name = "employer_id", nullable = false)
	private UUID employerId;

	@Column(name = "personnel_number", nullable = false)
	private String personnelNumber;

	@Column(name = "first_name", nullable = false)
	private String firstName;

	@Column(name = "last_name", nullable = false)
	private String lastName;

	@Column(name = "external_employee_ref")
	private String externalEmployeeRef;

	@Column(name = "active", nullable = false)
	private boolean active;

	protected EmployeeEntity() {
	}

	UUID getId() {
		return this.id;
	}

	boolean isActive() {
		return this.active;
	}

}


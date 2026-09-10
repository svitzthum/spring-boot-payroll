package dev.svitzthum.payroll.workinghours.adapter.out.persistence;

import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

import dev.svitzthum.payroll.workinghours.domain.WorkingHoursSource;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Persistent form of {@code MonthlyWorkingHours}. It carries the technical columns the
 * domain does not know about: id, optimistic locking version and audit timestamps.
 */
@Entity
@Table(name = "monthly_working_hours")
@EntityListeners(AuditingEntityListener.class)
class MonthlyWorkingHoursEntity {

	@Id
	private UUID id;

	@Column(name = "employee_id", nullable = false, updatable = false)
	private UUID employeeId;

	@Convert(converter = YearMonthConverter.class)
	@Column(name = "period", nullable = false, updatable = false)
	private YearMonth period;

	@Column(name = "minutes_worked", nullable = false)
	private int minutesWorked;

	@Enumerated(EnumType.STRING)
	@Column(name = "last_source", nullable = false)
	private WorkingHoursSource lastSource;

	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected MonthlyWorkingHoursEntity() {
	}

	MonthlyWorkingHoursEntity(UUID employeeId, YearMonth period) {
		this.id = UUID.randomUUID();
		this.employeeId = employeeId;
		this.period = period;
	}

	UUID getEmployeeId() {
		return this.employeeId;
	}

	YearMonth getPeriod() {
		return this.period;
	}

	int getMinutesWorked() {
		return this.minutesWorked;
	}

	void setMinutesWorked(int minutesWorked) {
		this.minutesWorked = minutesWorked;
	}

	WorkingHoursSource getLastSource() {
		return this.lastSource;
	}

	void setLastSource(WorkingHoursSource lastSource) {
		this.lastSource = lastSource;
	}

	Long getVersion() {
		return this.version;
	}

}


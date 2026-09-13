package dev.svitzthum.payroll.workinghours.adapter.out.persistence;

import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

import dev.svitzthum.payroll.workinghours.application.ImportStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Persistent form of a journal entry. Rows are written once and never changed, so there
 * is neither a version nor an audit timestamp: {@code importedAt} is part of the entry
 * itself.
 */
@Entity
@Table(name = "time_tracking_import")
class TimeTrackingImportEntity {

	@Id
	private UUID id;

	@Column(name = "external_event_id", nullable = false, updatable = false)
	private String externalEventId;

	@Column(name = "external_employee_ref", nullable = false, updatable = false)
	private String externalEmployeeRef;

	@Convert(converter = YearMonthConverter.class)
	@Column(name = "period", nullable = false, updatable = false)
	private YearMonth period;

	@Column(name = "minutes_worked", nullable = false, updatable = false)
	private int minutesWorked;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, updatable = false)
	private ImportStatus status;

	@Column(name = "imported_at", nullable = false, updatable = false)
	private Instant importedAt;

	@Column(name = "detail", updatable = false)
	private String detail;

	protected TimeTrackingImportEntity() {
	}

	TimeTrackingImportEntity(String externalEventId, String externalEmployeeRef, YearMonth period, int minutesWorked,
			ImportStatus status, String detail, Instant importedAt) {
		this.id = UUID.randomUUID();
		this.externalEventId = externalEventId;
		this.externalEmployeeRef = externalEmployeeRef;
		this.period = period;
		this.minutesWorked = minutesWorked;
		this.status = status;
		this.detail = detail;
		this.importedAt = importedAt;
	}

	String getExternalEmployeeRef() {
		return this.externalEmployeeRef;
	}

	YearMonth getPeriod() {
		return this.period;
	}

	int getMinutesWorked() {
		return this.minutesWorked;
	}

	ImportStatus getStatus() {
		return this.status;
	}

	String getDetail() {
		return this.detail;
	}

	Instant getImportedAt() {
		return this.importedAt;
	}

}



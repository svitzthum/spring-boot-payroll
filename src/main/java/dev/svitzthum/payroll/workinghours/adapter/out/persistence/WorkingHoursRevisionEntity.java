package dev.svitzthum.payroll.workinghours.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import dev.svitzthum.payroll.workinghours.domain.WorkingHoursSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * One entry of the append-only history: which source set which value when. Rows are never
 * changed or deleted, so there is no version and no updated timestamp.
 */
@Entity
@Table(name = "working_hours_revision")
@EntityListeners(AuditingEntityListener.class)
class WorkingHoursRevisionEntity {

	@Id
	private UUID id;

	@Column(name = "working_hours_id", nullable = false, updatable = false)
	private UUID workingHoursId;

	@Column(name = "minutes_worked", nullable = false, updatable = false)
	private int minutesWorked;

	@Enumerated(EnumType.STRING)
	@Column(name = "source", nullable = false, updatable = false)
	private WorkingHoursSource source;

	@CreatedDate
	@Column(name = "changed_at", nullable = false, updatable = false)
	private Instant changedAt;

	protected WorkingHoursRevisionEntity() {
	}

	WorkingHoursRevisionEntity(UUID workingHoursId, int minutesWorked, WorkingHoursSource source) {
		this.id = UUID.randomUUID();
		this.workingHoursId = workingHoursId;
		this.minutesWorked = minutesWorked;
		this.source = source;
	}

	int getMinutesWorked() {
		return this.minutesWorked;
	}

	WorkingHoursSource getSource() {
		return this.source;
	}

	Instant getChangedAt() {
		return this.changedAt;
	}

}


package dev.svitzthum.payroll.workinghours.adapter.out.persistence;

import dev.svitzthum.payroll.workinghours.application.EventAlreadyImportedException;
import dev.svitzthum.payroll.workinghours.application.port.out.TimeTrackingImportJournal;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * Implements the journal on top of JPA. The unique constraint on the external event id is
 * what actually guarantees that an event is applied at most once, no matter how many
 * instances run the import (ADR 0007); {@link #contains} only spares the common case the
 * work of trying.
 */
@Component
class TimeTrackingImportJournalAdapter implements TimeTrackingImportJournal {

	private final TimeTrackingImportJpaRepository entries;

	TimeTrackingImportJournalAdapter(TimeTrackingImportJpaRepository entries) {
		this.entries = entries;
	}

	@Override
	public boolean contains(String externalEventId) {
		return this.entries.existsByExternalEventId(externalEventId);
	}

	@Override
	public void record(ImportedEvent event) {
		TimeTrackingImportEntity entity = new TimeTrackingImportEntity(event.externalEventId(),
				event.externalEmployeeRef(), event.period(), event.minutesWorked(), event.status(), event.detail(),
				event.importedAt());
		try {
			// flush here so a second instance that recorded the same event in the
			// meantime is detected by this call rather than by the surrounding commit
			this.entries.saveAndFlush(entity);
		}
		catch (DataIntegrityViolationException ex) {
			throw new EventAlreadyImportedException(event.externalEventId(), ex);
		}
	}

}


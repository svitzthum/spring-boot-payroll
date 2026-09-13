package dev.svitzthum.payroll.workinghours.application.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.svitzthum.payroll.workinghours.application.EventAlreadyImportedException;
import dev.svitzthum.payroll.workinghours.application.port.out.TimeTrackingImportJournal;

class InMemoryTimeTrackingImportJournal implements TimeTrackingImportJournal {

	private final Map<String, ImportedEvent> entries = new LinkedHashMap<>();

	@Override
	public boolean contains(String externalEventId) {
		return this.entries.containsKey(externalEventId);
	}

	@Override
	public void record(ImportedEvent event) {
		if (this.entries.putIfAbsent(event.externalEventId(), event) != null) {
			throw new EventAlreadyImportedException(event.externalEventId(), null);
		}
	}

	List<ImportedEvent> entries() {
		return new ArrayList<>(this.entries.values());
	}

	ImportedEvent entry(String externalEventId) {
		return this.entries.get(externalEventId);
	}

}

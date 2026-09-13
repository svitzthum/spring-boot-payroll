package dev.svitzthum.payroll.workinghours.application.service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.svitzthum.payroll.workinghours.application.EventAlreadyImportedException;
import dev.svitzthum.payroll.workinghours.application.ImportStatus;
import dev.svitzthum.payroll.workinghours.application.WorkingHoursConflictException;
import dev.svitzthum.payroll.workinghours.application.port.in.ImportTimeTrackingUseCase;
import dev.svitzthum.payroll.workinghours.application.port.out.TimeTrackingImportJournal;
import dev.svitzthum.payroll.workinghours.application.port.out.TimeTrackingSystem;
import dev.svitzthum.payroll.workinghours.application.port.out.TimeTrackingSystem.TimeTrackingEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

/**
 * Fetches the reported times and hands each event to the processor. The run itself is
 * not transactional: every event is committed on its own, so a single failure neither
 * discards the events before it nor stops the ones after it.
 */
@Service
class TimeTrackingImportService implements ImportTimeTrackingUseCase {

	private static final Log logger = LogFactory.getLog(TimeTrackingImportService.class);

	private final TimeTrackingSystem timeTracking;

	private final TimeTrackingImportJournal journal;

	private final TimeTrackingEventProcessor processor;

	TimeTrackingImportService(TimeTrackingSystem timeTracking, TimeTrackingImportJournal journal,
			TimeTrackingEventProcessor processor) {
		this.timeTracking = timeTracking;
		this.journal = journal;
		this.processor = processor;
	}

	@Override
	public ImportSummary importTimeTracking() {
		List<TimeTrackingEvent> events = this.timeTracking.fetchEvents();
		Map<ImportStatus, Integer> counts = new EnumMap<>(ImportStatus.class);
		for (TimeTrackingEvent event : events) {
			process(event).ifPresent(status -> counts.merge(status, 1, Integer::sum));
		}
		ImportSummary summary = new ImportSummary(counts.getOrDefault(ImportStatus.APPLIED, 0),
				counts.getOrDefault(ImportStatus.SKIPPED, 0), counts.getOrDefault(ImportStatus.FAILED, 0));
		logger.debug("imported time tracking events: " + summary);
		return summary;
	}

	private Optional<ImportStatus> process(TimeTrackingEvent event) {
		if (this.journal.contains(event.externalEventId())) {
			return Optional.empty();
		}
		try {
			return Optional.of(this.processor.process(event));
		}
		catch (EventAlreadyImportedException ex) {
			// another instance processed the same event in the meantime
			return Optional.empty();
		}
		catch (WorkingHoursConflictException ex) {
			// the retries did not settle it; nothing was written, so the next run picks
			// the event up again
			logger.info("deferring event " + event.externalEventId() + " after a concurrent modification");
			return Optional.empty();
		}
	}

}

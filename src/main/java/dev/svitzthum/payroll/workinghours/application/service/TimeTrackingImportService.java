package dev.svitzthum.payroll.workinghours.application.service;

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
		int applied = 0;
		int skipped = 0;
		int failed = 0;
		for (TimeTrackingEvent event : this.timeTracking.fetchEvents()) {
			switch (process(event)) {
				case APPLIED -> applied++;
				case SKIPPED -> skipped++;
				case FAILED -> failed++;
				case null -> {
					// already journalled or deferred, counts towards nothing
				}
			}
		}
		ImportSummary summary = new ImportSummary(applied, skipped, failed);
		logger.debug("imported time tracking events: " + summary);
		return summary;
	}

	/** @return the status the event ended in, or {@code null} if it was not processed */
	private ImportStatus process(TimeTrackingEvent event) {
		if (this.journal.contains(event.externalEventId())) {
			return null;
		}
		try {
			return this.processor.process(event);
		}
		catch (EventAlreadyImportedException ex) {
			// another instance processed the same event in the meantime
			return null;
		}
		catch (WorkingHoursConflictException ex) {
			// the retries did not settle it; nothing was written, so the next run picks
			// the event up again
			logger.info("deferring event " + event.externalEventId() + " after a concurrent modification");
			return null;
		}
	}

}

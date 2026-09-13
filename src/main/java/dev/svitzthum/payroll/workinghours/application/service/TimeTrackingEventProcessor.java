package dev.svitzthum.payroll.workinghours.application.service;

import java.time.Clock;
import java.time.Instant;

import dev.svitzthum.payroll.workinghours.application.FuturePeriodException;
import dev.svitzthum.payroll.workinghours.application.ImportStatus;
import dev.svitzthum.payroll.workinghours.application.InactiveEmployeeException;
import dev.svitzthum.payroll.workinghours.application.ManualEntryTakesPrecedenceException;
import dev.svitzthum.payroll.workinghours.application.WorkingHoursConflictException;
import dev.svitzthum.payroll.workinghours.application.port.in.RecordWorkingHoursCommand;
import dev.svitzthum.payroll.workinghours.application.port.in.RecordWorkingHoursUseCase;
import dev.svitzthum.payroll.workinghours.application.port.out.EmployeeDirectory;
import dev.svitzthum.payroll.workinghours.application.port.out.EmployeeDirectory.Employee;
import dev.svitzthum.payroll.workinghours.application.port.out.TimeTrackingImportJournal;
import dev.svitzthum.payroll.workinghours.application.port.out.TimeTrackingImportJournal.ImportedEvent;
import dev.svitzthum.payroll.workinghours.application.port.out.TimeTrackingSystem.TimeTrackingEvent;
import dev.svitzthum.payroll.workinghours.domain.WorkDuration;
import dev.svitzthum.payroll.workinghours.domain.WorkingHoursSource;

import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Processes a single event in its own transaction, so one unusable event does not
 * discard the whole run. Writing the monthly value and writing the journal entry happen
 * together: either both are committed or neither is.
 *
 * <p>
 * The outcome is decided by the use case, not here: a month that was entered manually is
 * refused and recorded as {@link ImportStatus#SKIPPED}, permanent problems — an unknown
 * employee, a value the domain rejects — as {@link ImportStatus#FAILED}. Neither is
 * retried. A concurrent modification is retried with fresh state (ADR 0004); if it
 * persists, nothing is journalled and the next run processes the event again.
 */
@Component
class TimeTrackingEventProcessor {

	private final RecordWorkingHoursUseCase recordWorkingHours;

	private final EmployeeDirectory employees;

	private final TimeTrackingImportJournal journal;

	private final Clock clock;

	TimeTrackingEventProcessor(RecordWorkingHoursUseCase recordWorkingHours, EmployeeDirectory employees,
			TimeTrackingImportJournal journal, Clock clock) {
		this.recordWorkingHours = recordWorkingHours;
		this.employees = employees;
		this.journal = journal;
		this.clock = clock;
	}

	@Retryable(includes = WorkingHoursConflictException.class, maxRetries = 2)
	@Transactional
	ImportStatus process(TimeTrackingEvent event) {
		Employee employee = this.employees.findByExternalReference(event.externalEmployeeRef()).orElse(null);
		if (employee == null) {
			return journal(event, ImportStatus.FAILED, "unknown employee reference " + event.externalEmployeeRef());
		}
		try {
			this.recordWorkingHours.recordWorkingHours(new RecordWorkingHoursCommand(employee.id(), event.period(),
					WorkDuration.ofMinutes(event.minutesWorked()), WorkingHoursSource.TIME_TRACKING));
		}
		catch (ManualEntryTakesPrecedenceException ex) {
			return journal(event, ImportStatus.SKIPPED, "a manual entry takes precedence");
		}
		catch (InactiveEmployeeException | FuturePeriodException | IllegalArgumentException ex) {
			return journal(event, ImportStatus.FAILED, ex.getMessage());
		}
		return journal(event, ImportStatus.APPLIED, null);
	}


	private ImportStatus journal(TimeTrackingEvent event, ImportStatus status, String detail) {
		this.journal.record(new ImportedEvent(event.externalEventId(), event.externalEmployeeRef(), event.period(),
				event.minutesWorked(), status, detail, Instant.now(this.clock)));
		return status;
	}

}

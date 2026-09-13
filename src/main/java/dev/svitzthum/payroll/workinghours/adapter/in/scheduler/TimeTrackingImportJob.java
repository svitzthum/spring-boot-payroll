package dev.svitzthum.payroll.workinghours.adapter.in.scheduler;

import dev.svitzthum.payroll.workinghours.application.port.in.ImportTimeTrackingUseCase;
import dev.svitzthum.payroll.workinghours.application.port.in.ImportTimeTrackingUseCase.ImportSummary;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Driving adapter that triggers the import on a schedule. It holds no business logic of
 * its own: it decides when, the use case decides what.
 *
 * <p>
 * A failing run is logged and swallowed, because an exception leaving this method would
 * only cancel the schedule. There is nothing to compensate — every event is committed on
 * its own, and whatever did not make it is picked up by the next run.
 */
@Component
@ConditionalOnProperty(name = "payroll.import.enabled", havingValue = "true", matchIfMissing = true)
class TimeTrackingImportJob {

	private static final Log logger = LogFactory.getLog(TimeTrackingImportJob.class);

	private final ImportTimeTrackingUseCase importTimeTracking;

	TimeTrackingImportJob(ImportTimeTrackingUseCase importTimeTracking) {
		this.importTimeTracking = importTimeTracking;
	}

	@Scheduled(initialDelayString = "${payroll.import.initial-delay:10s}",
			fixedDelayString = "${payroll.import.interval:5m}")
	void runImport() {
		try {
			ImportSummary summary = this.importTimeTracking.importTimeTracking();
			logger.info("time tracking import finished: " + summary.total() + " events, " + summary.applied()
					+ " applied, " + summary.skipped() + " skipped, " + summary.failed() + " failed");
		}
		catch (RuntimeException ex) {
			logger.error("time tracking import failed, retrying on the next run", ex);
		}
	}

}


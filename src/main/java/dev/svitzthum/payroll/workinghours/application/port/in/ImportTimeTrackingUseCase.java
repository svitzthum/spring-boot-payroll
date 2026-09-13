package dev.svitzthum.payroll.workinghours.application.port.in;

/**
 * Transfers the working times reported by the external time tracking system into the
 * payroll data. Running it again is safe: an event that has already been processed is
 * recognised by the import journal.
 */
public interface ImportTimeTrackingUseCase {

	ImportSummary importTimeTracking();

	/** How many events of one run ended in which state. */
	record ImportSummary(int applied, int skipped, int failed) {

		public int total() {
			return this.applied + this.skipped + this.failed;
		}

	}

}

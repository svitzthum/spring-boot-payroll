package dev.svitzthum.payroll.workinghours.application.service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import dev.svitzthum.payroll.workinghours.application.port.out.TimeTrackingSystem;

class InMemoryTimeTrackingSystem implements TimeTrackingSystem {

	private final List<TimeTrackingEvent> events = new ArrayList<>();

	@Override
	public List<TimeTrackingEvent> fetchEvents() {
		return List.copyOf(this.events);
	}

	void reports(String eventId, String externalEmployeeRef, YearMonth period, int minutes) {
		this.events.add(new TimeTrackingEvent(eventId, externalEmployeeRef, period, minutes));
	}

}

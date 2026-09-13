package dev.svitzthum.payroll.workinghours.adapter.in.scheduler;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables the scheduler this adapter needs, kept next to the only job that uses it. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
class SchedulingConfiguration {

}


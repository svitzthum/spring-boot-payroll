package dev.svitzthum.payroll.workinghours.adapter.out.persistence;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables the auditing listener that fills {@code created_at} and {@code updated_at}.
 */
@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing
class PersistenceConfiguration {

}


package dev.svitzthum.payroll;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Guards the dependency rule of the hexagon so a violation fails the build instead of
 * being caught in review. See docs/02-architecture.md.
 */
@AnalyzeClasses(packages = "dev.svitzthum.payroll", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

	@ArchTest
	static final ArchRule theDomainUsesNoFrameworks = noClasses().that()
		.resideInAPackage("..workinghours.domain..")
		.should()
		.dependOnClassesThat()
		.resideInAnyPackage("org.springframework..", "jakarta..", "tools.jackson..", "com.fasterxml.jackson..",
				"org.hibernate..")
		.because("the domain model must stay free of framework types");

	@ArchTest
	static final ArchRule theDomainDoesNotKnowTheApplication = noClasses().that()
		.resideInAPackage("..workinghours.domain..")
		.should()
		.dependOnClassesThat()
		.resideInAnyPackage("..application..", "..adapter..");

	@ArchTest
	static final ArchRule theApplicationDoesNotKnowItsAdapters = noClasses().that()
		.resideInAPackage("..application..")
		.should()
		.dependOnClassesThat()
		.resideInAPackage("..adapter..")
		.because("adapters are chosen at runtime, the application only knows its ports");

	@ArchTest
	static final ArchRule adaptersDoNotKnowEachOther = noClasses().that()
		.resideInAPackage("..adapter.in..")
		.should()
		.dependOnClassesThat()
		.resideInAPackage("..adapter.out..");

	@ArchTest
	static final ArchRule jpaEntitiesStayInThePersistenceAdapter = noClasses().that()
		.resideOutsideOfPackage("..adapter.out.persistence..")
		.should()
		.dependOnClassesThat()
		.areAnnotatedWith("jakarta.persistence.Entity")
		.because("nothing outside the persistence adapter may see a JPA entity");

}


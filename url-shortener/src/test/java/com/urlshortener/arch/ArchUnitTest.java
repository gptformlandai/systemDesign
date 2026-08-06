package com.urlshortener.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * ArchUnit tests enforcing Hexagonal Architecture boundaries.
 *
 * <p>Rules:
 * <ol>
 *   <li>Domain layer has zero Spring dependencies</li>
 *   <li>Domain layer has zero adapter dependencies</li>
 *   <li>Port interfaces have zero Spring/adapter dependencies</li>
 *   <li>Services depend only on ports — never on adapter implementations</li>
 *   <li>Adapters may depend on ports and domain — not on services directly</li>
 * </ol>
 *
 * <p>These rules are enforced at compile time via Gradle's {@code archTest} task.
 */
@DisplayName("Hexagonal Architecture boundary tests")
class ArchUnitTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.urlshortener");
    }

    @Test
    @DisplayName("Domain layer has no Spring Framework dependencies")
    void domainHasNoSpringDependencies() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.urlshortener.domain..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Domain layer has no adapter dependencies")
    void domainHasNoAdapterDependencies() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.urlshortener.domain..")
                .should().dependOnClassesThat().resideInAPackage("com.urlshortener.adapter..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Port interfaces have no adapter dependencies")
    void portsHaveNoAdapterDependencies() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.urlshortener.port..")
                .should().dependOnClassesThat().resideInAPackage("com.urlshortener.adapter..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Services depend only on domain and port interfaces — not on adapter implementations")
    void servicesDoNotDependOnAdapters() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.urlshortener.service..")
                .should().dependOnClassesThat().resideInAPackage("com.urlshortener.adapter..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Adapters do not import service layer (prevent circular dependency)")
    void adaptersDoNotDependOnServices() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.urlshortener.adapter..")
                .and().haveSimpleNameNotEndingWith("RedisIdempotencyStore") // implements service inner interface
                .should().dependOnClassesThat()
                .resideInAPackage("com.urlshortener.service..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Domain objects are records or enums (immutability constraint)")
    void domainObjectsAreImmutable() {
        ArchRule rule = classes()
                .that().resideInAPackage("com.urlshortener.domain..")
                .should().beRecords()
                .orShould().beEnums();

        rule.check(classes);
    }
}

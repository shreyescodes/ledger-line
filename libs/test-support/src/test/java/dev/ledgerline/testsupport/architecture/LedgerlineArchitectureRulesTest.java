package dev.ledgerline.testsupport.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import dev.ledgerline.common.fixture.CommonUsingSpring;
import dev.ledgerline.common.fixture.SharedType;
import dev.ledgerline.fixture.adapter.FixtureAdapter;
import dev.ledgerline.fixture.application.CallsAnotherService;
import dev.ledgerline.fixture.application.ReadsTheSystemClock;
import dev.ledgerline.fixture.application.UsesAnInjectedClock;
import dev.ledgerline.fixture.application.UsesTheCommonLib;
import dev.ledgerline.fixture.config.FixtureClockConfiguration;
import dev.ledgerline.fixture.domain.DomainCallingAnAdapter;
import dev.ledgerline.fixture.domain.FloatingPointAmount;
import dev.ledgerline.fixture.domain.MinorUnitsAmount;
import dev.ledgerline.fixture.domain.SpringAwareDomainObject;
import org.junit.jupiter.api.Test;

/** Each rule must reject a class that breaks it and accept one that follows it. */
class LedgerlineArchitectureRulesTest {

    @Test
    void servicesCanNotUseEachOthersCode() {
        assertViolated(
                LedgerlineArchitectureRules.SERVICES_ONLY_DEPEND_ON_THEMSELVES_AND_SHARED_LIBS,
                CallsAnotherService.class);
        assertHolds(
                LedgerlineArchitectureRules.SERVICES_ONLY_DEPEND_ON_THEMSELVES_AND_SHARED_LIBS,
                UsesTheCommonLib.class,
                SharedType.class);
    }

    @Test
    void domainCanNotUseSpringOrOuterLayers() {
        assertViolated(LedgerlineArchitectureRules.DOMAIN_IS_FRAMEWORK_FREE, SpringAwareDomainObject.class);
        assertViolated(LedgerlineArchitectureRules.DOMAIN_IS_FRAMEWORK_FREE, DomainCallingAnAdapter.class);
        assertHolds(LedgerlineArchitectureRules.DOMAIN_IS_FRAMEWORK_FREE, MinorUnitsAmount.class);
    }

    @Test
    void layersOnlyPointInwards() {
        assertViolated(
                LedgerlineArchitectureRules.LAYERS_POINT_INWARDS, DomainCallingAnAdapter.class, FixtureAdapter.class);
        assertHolds(
                LedgerlineArchitectureRules.LAYERS_POINT_INWARDS, UsesTheCommonLib.class, MinorUnitsAmount.class);
    }

    @Test
    void domainHasNoFloatingPoint() {
        assertViolated(LedgerlineArchitectureRules.NO_FLOATING_POINT_IN_THE_DOMAIN, FloatingPointAmount.class);
        assertHolds(LedgerlineArchitectureRules.NO_FLOATING_POINT_IN_THE_DOMAIN, MinorUnitsAmount.class);
    }

    @Test
    void onlyConfigurationReadsTheSystemClock() {
        assertViolated(LedgerlineArchitectureRules.TIME_COMES_FROM_AN_INJECTED_CLOCK, ReadsTheSystemClock.class);
        assertHolds(
                LedgerlineArchitectureRules.TIME_COMES_FROM_AN_INJECTED_CLOCK,
                UsesAnInjectedClock.class,
                FixtureClockConfiguration.class);
    }

    @Test
    void commonLibIsFrameworkFree() {
        assertViolated(LedgerlineArchitectureRules.COMMON_IS_FRAMEWORK_FREE, CommonUsingSpring.class);
        assertHolds(LedgerlineArchitectureRules.COMMON_IS_FRAMEWORK_FREE, SharedType.class);
    }

    private static void assertViolated(ArchRule rule, Class<?>... classes) {
        EvaluationResult result = rule.evaluate(new ClassFileImporter().importClasses(classes));
        assertThat(result.hasViolation())
                .as("expected a violation of: %s", rule.getDescription())
                .isTrue();
    }

    private static void assertHolds(ArchRule rule, Class<?>... classes) {
        EvaluationResult result = rule.evaluate(new ClassFileImporter().importClasses(classes));
        assertThat(result.getFailureReport().getDetails())
                .as("expected no violation of: %s", rule.getDescription())
                .isEmpty();
    }
}

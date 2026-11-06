package dev.ledgerline.testsupport.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Set;

/**
 * Architecture rules every service must follow (ADR-0008). A service applies all of them with:
 *
 * <pre>{@code
 * @AnalyzeClasses(packages = "dev.ledgerline.ledger", importOptions = ImportOption.DoNotIncludeTests.class)
 * class ArchitectureTest {
 *     @ArchTest
 *     static final ArchTests RULES = ArchTests.in(LedgerlineArchitectureRules.class);
 * }
 * }</pre>
 *
 * <p>Rules allow an empty match because the skeleton has no domain code yet.
 */
public final class LedgerlineArchitectureRules {

    private static final String BASE_PACKAGE = "dev.ledgerline";
    private static final Set<String> SHARED_MODULES = Set.of("common", "testsupport");

    /** Services never call each other's code; they talk over HTTP or Kafka. */
    @ArchTest
    public static final ArchRule SERVICES_ONLY_DEPEND_ON_THEMSELVES_AND_SHARED_LIBS = classes()
            .that()
            .resideInAPackage(BASE_PACKAGE + "..")
            .should(onlyDependOnTheirOwnModuleOrSharedLibs())
            .allowEmptyShould(true);

    /** Domain code is plain Java, so it can be tested without a framework. */
    @ArchTest
    public static final ArchRule DOMAIN_IS_FRAMEWORK_FREE = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "org.springframework..",
                    "jakarta..",
                    "javax..",
                    "com.fasterxml..",
                    "tools.jackson..",
                    "org.hibernate..",
                    "org.jooq..",
                    "..application..",
                    "..adapter..",
                    "..config..")
            .because("the domain model must not depend on frameworks or outer layers")
            .allowEmptyShould(true);

    /** Ports and adapters: adapters call the application, the application calls the domain. */
    @ArchTest
    public static final ArchRule LAYERS_POINT_INWARDS = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .withOptionalLayers(true)
            .layer("Domain")
            .definedBy("..domain..")
            .layer("Application")
            .definedBy("..application..")
            .layer("Adapters")
            .definedBy("..adapter..")
            .layer("Config")
            .definedBy("..config..")
            .whereLayer("Config")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("Adapters")
            .mayOnlyBeAccessedByLayers("Config")
            .whereLayer("Application")
            .mayOnlyBeAccessedByLayers("Adapters", "Config")
            .allowEmptyShould(true);

    /** Invariant I1: money is integer minor units, never floating point. */
    @ArchTest
    public static final ArchRule NO_FLOATING_POINT_IN_THE_DOMAIN = noFields()
            .that()
            .areDeclaredInClassesThat()
            .resideInAPackage("..domain..")
            .should()
            .haveRawType(double.class)
            .orShould()
            .haveRawType(float.class)
            .orShould()
            .haveRawType(Double.class)
            .orShould()
            .haveRawType(Float.class)
            .because("money is a long count of minor units (invariant I1, ADR-0002)")
            .allowEmptyShould(true);

    /** Invariant I10: time comes from an injected Clock; only configuration reads the system clock. */
    @ArchTest
    public static final ArchRule TIME_COMES_FROM_AN_INJECTED_CLOCK = noClasses()
            .that()
            .resideOutsideOfPackage("..config..")
            .should()
            .callMethod(Instant.class, "now")
            .orShould()
            .callMethod(LocalDateTime.class, "now")
            .orShould()
            .callMethod(LocalDate.class, "now")
            .orShould()
            .callMethod(OffsetDateTime.class, "now")
            .orShould()
            .callMethod(ZonedDateTime.class, "now")
            .orShould()
            .callMethod(System.class, "currentTimeMillis")
            .orShould()
            .callMethod(Clock.class, "systemUTC")
            .orShould()
            .callMethod(Clock.class, "systemDefaultZone")
            .because("tests must control time (invariant I10)")
            .allowEmptyShould(true);

    /** libs/common is shared by domain code, so it stays framework-free too. */
    @ArchTest
    public static final ArchRule COMMON_IS_FRAMEWORK_FREE = noClasses()
            .that()
            .resideInAPackage(BASE_PACKAGE + ".common..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta..", "com.fasterxml..", "tools.jackson..")
            .allowEmptyShould(true);

    private LedgerlineArchitectureRules() {}

    private static ArchCondition<JavaClass> onlyDependOnTheirOwnModuleOrSharedLibs() {
        return new ArchCondition<>("only depend on their own service or the shared libs") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                String ownModule = moduleOf(origin.getPackageName());
                for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                    String targetPackage =
                            dependency.getTargetClass().getBaseComponentType().getPackageName();
                    if (!targetPackage.startsWith(BASE_PACKAGE + ".")) {
                        continue;
                    }
                    String targetModule = moduleOf(targetPackage);
                    if (!targetModule.equals(ownModule) && !SHARED_MODULES.contains(targetModule)) {
                        events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                    }
                }
            }
        };
    }

    /** "dev.ledgerline.ledger.domain" belongs to module "ledger". */
    private static String moduleOf(String packageName) {
        if (!packageName.startsWith(BASE_PACKAGE + ".")) {
            return "";
        }
        String rest = packageName.substring(BASE_PACKAGE.length() + 1);
        int dot = rest.indexOf('.');
        return dot < 0 ? rest : rest.substring(0, dot);
    }
}

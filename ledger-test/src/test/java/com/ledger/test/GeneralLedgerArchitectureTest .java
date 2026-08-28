package com.ledger.test;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.ledger")
class GeneralLedgerArchitectureTest {

    /**
     * Domain must be completely independent from outer layers.
     *
     * Reference:
     * ADR-0011 — Domain Layer Architecture
     */
    @ArchTest
    static final ArchRule domain_should_not_depend_on_outer_layers =
            noClasses()
                    .that()
                    .resideInAnyPackage("..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "..application..",
                            "..presentation..",
                            "..infrastructure.."
                    );

    /**
     * Domain must be framework independent.
     *
     * Reference:
     * ADR-0011 — Domain Layer Architecture
     */
    @ArchTest
    static final ArchRule domain_should_not_depend_on_frameworks =
            noClasses()
                    .that()
                    .resideInAnyPackage("..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.persistence..",
                            "javax.persistence.."
                    );

    /**
     * Application may depend on Domain,
     * but must not depend on Presentation or Infrastructure.
     *
     * Reference:
     * ADR-0011 — Domain Layer Architecture
     */
    @ArchTest
    static final ArchRule application_should_only_depend_inward =
            noClasses()
                    .that()
                    .resideInAnyPackage("..application..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "..presentation..",
                            "..infrastructure.."
                    );

    /**
     * Presentation depends on Application,
     * but must not depend directly on Infrastructure.
     *
     * Reference:
     * ADR-0011 — Domain Layer Architecture
     */
    @ArchTest
    static final ArchRule presentation_should_not_depend_on_infrastructure =
            noClasses()
                    .that()
                    .resideInAnyPackage("..presentation..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..infrastructure..");

    /**
     * Infrastructure is an outer layer and must not depend on
     * Presentation.
     *
     * Reference:
     * ADR-0011 — Domain Layer Architecture
     */
    @ArchTest
    static final ArchRule infrastructure_should_not_depend_on_presentation =
            noClasses()
                    .that()
                    .resideInAnyPackage("..infrastructure..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..presentation..");
}
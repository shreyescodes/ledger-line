package dev.ledgerline.ledger;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import dev.ledgerline.testsupport.architecture.LedgerlineArchitectureRules;

@AnalyzeClasses(packages = "dev.ledgerline.ledger", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchTests LEDGERLINE_RULES = ArchTests.in(LedgerlineArchitectureRules.class);
}

package fr.nextbanking.credits;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.onionArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Hexagone : sens des dépendances, et aucun framework dans le domaine et l'application.
 */
@AnalyzeClasses(packages = "fr.nextbanking.credits", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule sens_des_dependances = onionArchitecture()
            .domainModels("..domain..")
            .applicationServices("..application..")
            .adapter("in", "..adapter.in..")
            .adapter("out", "..adapter.out..")
            .adapter("infrastructure", "..infrastructure..")
            // Aucun paquet « domain service » distinct : le domaine tient dans ..domain.. (domainModels).
            .withOptionalLayers(true)
            .ensureAllClassesAreContainedInArchitecture();

    @ArchTest
    static final ArchRule aucun_framework_dans_le_domaine_et_l_application = noClasses()
            .that().resideInAnyPackage("..domain..", "..application..")
            .should().dependOnClassesThat().resideOutsideOfPackages(
                    "java..", "fr.nextbanking.credits.domain..", "fr.nextbanking.credits.application..");
}

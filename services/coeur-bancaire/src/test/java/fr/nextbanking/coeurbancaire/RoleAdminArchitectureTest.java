package fr.nextbanking.coeurbancaire;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import fr.nextbanking.coeurbancaire.domain.Role;

/**
 * Le contrôle du rôle ADMIN a un point d'appel unique : {@code DecisionAdministrateur.exigerAdmin}. Aucune autre
 * classe ne lit {@code Role.ADMIN} : un cas d'usage futur appelle ce point au lieu de recopier le contrôle.
 */
@AnalyzeClasses(packages = "fr.nextbanking.coeurbancaire", importOptions = ImportOption.DoNotIncludeTests.class)
class RoleAdminArchitectureTest {

    private static final String POINT_UNIQUE = "fr.nextbanking.coeurbancaire.application.DecisionAdministrateur";

    @ArchTest
    static final ArchRule seul_le_point_unique_lit_le_role_admin = noClasses()
            .that().doNotHaveFullyQualifiedName(POINT_UNIQUE)
            .and().doNotHaveFullyQualifiedName(Role.class.getName())
            .should().accessField(Role.class, "ADMIN");
}

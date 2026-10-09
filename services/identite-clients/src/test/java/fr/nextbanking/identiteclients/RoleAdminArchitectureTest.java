package fr.nextbanking.identiteclients;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import fr.nextbanking.identiteclients.domain.Role;

/**
 * Le contrôle du rôle ADMIN a un point d'appel unique : {@code DroitsRoles} (package-private), appelé par la
 * désactivation, la réactivation et le portefeuille du conseiller. Aucune autre classe ne lit {@code Role.ADMIN}.
 */
@AnalyzeClasses(packages = "fr.nextbanking.identiteclients", importOptions = ImportOption.DoNotIncludeTests.class)
class RoleAdminArchitectureTest {

    private static final String POINT_UNIQUE = "fr.nextbanking.identiteclients.application.DroitsRoles";

    @ArchTest
    static final ArchRule seul_le_point_unique_lit_le_role_admin = noClasses()
            .that().doNotHaveFullyQualifiedName(POINT_UNIQUE)
            .and().doNotHaveFullyQualifiedName(Role.class.getName())
            .should().accessField(Role.class, "ADMIN");
}

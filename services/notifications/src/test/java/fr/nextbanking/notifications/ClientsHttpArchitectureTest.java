package fr.nextbanking.notifications;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.util.Set;

/**
 * Les clients HTTP ne vivent que dans les adaptateurs des fournisseurs externes :
 * les services ne s'appellent jamais en HTTP.
 */
@AnalyzeClasses(packages = "fr.nextbanking.notifications", importOptions = ImportOption.DoNotIncludeTests.class)
class ClientsHttpArchitectureTest {

    // Désignés par nom qualifié : ces classes ne sont pas toutes sur le classpath.
    private static final Set<String> CLIENTS_HTTP = Set.of(
            "org.springframework.web.client.RestClient",
            "org.springframework.web.reactive.function.client.WebClient",
            "org.springframework.web.client.RestTemplate",
            "java.net.http.HttpClient");

    private static final DescribedPredicate<JavaClass> CLIENT_HTTP =
            DescribedPredicate.describe("un client HTTP " + CLIENTS_HTTP,
                    classe -> CLIENTS_HTTP.contains(classe.getName())
                            || CLIENTS_HTTP.stream().anyMatch(nom -> classe.getName().startsWith(nom + "$")));

    @ArchTest
    static final ArchRule clients_http_dans_les_fournisseurs_seulement = noClasses()
            .that().resideOutsideOfPackage("..adapter.out.fournisseur..")
            .should().dependOnClassesThat(CLIENT_HTTP);
}

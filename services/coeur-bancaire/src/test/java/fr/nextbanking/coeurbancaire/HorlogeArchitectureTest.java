package fr.nextbanking.coeurbancaire;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Le temps ne se lit que par le port Horloge : seuls les adaptateurs horloge et jeton lisent l'heure système.
 */
@AnalyzeClasses(packages = "fr.nextbanking.coeurbancaire", importOptions = ImportOption.DoNotIncludeTests.class)
class HorlogeArchitectureTest {

    private static final String[] ADAPTATEURS_DU_TEMPS = {"..adapter.out.horloge..", "..adapter.out.jeton.."};

    // Accès = appels et références de méthode (Instant::now).
    private static final DescribedPredicate<JavaAccess<?>> ACCES_A_NOW_DE_JAVA_TIME =
            DescribedPredicate.describe("un appel ou une référence à now(..) d'un type de java.time",
                    acces -> acces.getName().equals("now")
                            && acces.getTargetOwner().getPackageName().startsWith("java.time"));

    private static final DescribedPredicate<JavaAccess<?>> ACCES_A_CURRENT_TIME_MILLIS =
            DescribedPredicate.describe("un appel ou une référence à System.currentTimeMillis()",
                    acces -> acces.getName().equals("currentTimeMillis")
                            && acces.getTargetOwner().getName().equals("java.lang.System"));

    @ArchTest
    static final ArchRule aucun_now_de_java_time = noClasses()
            .that().resideOutsideOfPackages(ADAPTATEURS_DU_TEMPS)
            .should().accessTargetWhere(ACCES_A_NOW_DE_JAVA_TIME);

    @ArchTest
    static final ArchRule aucune_clock = noClasses()
            .that().resideOutsideOfPackages(ADAPTATEURS_DU_TEMPS)
            .should().dependOnClassesThat().haveFullyQualifiedName("java.time.Clock");

    @ArchTest
    static final ArchRule aucun_current_time_millis = noClasses()
            .that().resideOutsideOfPackages(ADAPTATEURS_DU_TEMPS)
            .should().accessTargetWhere(ACCES_A_CURRENT_TIME_MILLIS);
}

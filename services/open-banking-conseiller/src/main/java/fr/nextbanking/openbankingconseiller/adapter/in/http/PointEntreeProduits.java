package fr.nextbanking.openbankingconseiller.adapter.in.http;

import fr.nextbanking.openbankingconseiller.application.port.in.ConsulterCatalogue;
import fr.nextbanking.openbankingconseiller.domain.Catalogue;
import fr.nextbanking.openbankingconseiller.domain.ProduitCredit;
import fr.nextbanking.openbankingconseiller.domain.ProduitEpargne;
import fr.nextbanking.openbankingconseiller.domain.ProduitInvestissement;
import fr.nextbanking.openbankingconseiller.domain.TypeProduit;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptateur entrant HTTP du catalogue, ouvert à tout utilisateur authentifié quel que soit son rôle (FR-010-01).
 * Taux, rendements et LTV sortent en chaîne décimale exacte à 4 décimales, les montants à 2 : la mise en forme
 * (« 3,00 % ») et le libellé du risque restent au front. Un type inconnu est refusé par {@link GestionErreurs}.
 */
@RestController
public class PointEntreeProduits {

    static final String CHEMIN = "/api/open-banking-conseiller/produits";

    private static final int DECIMALES_TAUX = 4;
    private static final int DECIMALES_MONTANT = 2;

    private final ConsulterCatalogue consulterCatalogue;

    public PointEntreeProduits(ConsulterCatalogue consulterCatalogue) {
        this.consulterCatalogue = consulterCatalogue;
    }

    @GetMapping(CHEMIN)
    public ReponseCatalogue consulter(@RequestParam(name = "type", required = false) TypeProduit type) {
        return ReponseCatalogue.depuis(consulterCatalogue.consulter(type));
    }

    private static String decimal(BigDecimal valeur, int decimales) {
        return valeur == null ? null : valeur.setScale(decimales, RoundingMode.HALF_UP).toPlainString();
    }

    /** Catalogue rendu en JSON : trois groupes, toujours présents. */
    public record ReponseCatalogue(List<Epargne> epargne, List<Credit> credit, List<Investissement> investissement) {

        static ReponseCatalogue depuis(Catalogue catalogue) {
            return new ReponseCatalogue(
                    catalogue.epargne().stream().map(Epargne::depuis).toList(),
                    catalogue.credit().stream().map(Credit::depuis).toList(),
                    catalogue.investissement().stream().map(Investissement::depuis).toList());
        }
    }

    /** Produit d'épargne rendu en JSON. */
    public record Epargne(long id, String nom, String description, String taux, String depotMin,
            boolean capitalGaranti, Integer dureeMaxMois) {

        static Epargne depuis(ProduitEpargne produit) {
            return new Epargne(produit.id(), produit.nom(), produit.description(),
                    decimal(produit.taux(), DECIMALES_TAUX), decimal(produit.depotMin(), DECIMALES_MONTANT),
                    produit.capitalGaranti(), produit.dureeMaxMois());
        }
    }

    /** Produit de crédit rendu en JSON. */
    public record Credit(long id, String nom, String description, String taux, Integer dureeMaxMois,
            String ltvMax, boolean assuranceRequise) {

        static Credit depuis(ProduitCredit produit) {
            return new Credit(produit.id(), produit.nom(), produit.description(),
                    decimal(produit.taux(), DECIMALES_TAUX), produit.dureeMaxMois(),
                    decimal(produit.ltvMax(), DECIMALES_TAUX), produit.assuranceRequise());
        }
    }

    /** Produit d'investissement rendu en JSON. */
    public record Investissement(long id, String nom, String description, String secteur, Integer niveauRisque,
            String rendementAttendu) {

        static Investissement depuis(ProduitInvestissement produit) {
            return new Investissement(produit.id(), produit.nom(), produit.description(), produit.secteur(),
                    produit.niveauRisque(), decimal(produit.rendementAttendu(), DECIMALES_TAUX));
        }
    }
}

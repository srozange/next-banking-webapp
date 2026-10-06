package fr.nextbanking.credits.referencedata;

import fr.nextbanking.credits.adapter.out.horloge.HorlogeSysteme;
import fr.nextbanking.credits.application.port.out.Horloge;
import fr.nextbanking.credits.domain.Annuite;
import fr.nextbanking.credits.domain.DemandeSimulation;
import fr.nextbanking.credits.domain.LignePret;
import fr.nextbanking.credits.domain.Simulation;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Générateur de {@code reference-data/reference-data.sql} (portée test, lancé par
 * {@code reference-data/generer-reference-data.sh}) : 2 prêts EN_COURS de {@code user1} (identifiant client 1) et
 * leurs 18 échéances. Les dates sont relatives à la date du jour lue par l'{@link Horloge} : le début de chaque prêt
 * est le 1er d'un mois passé, et les échéances échues sont payées. Valeurs reconstituées faute de {@code import.sql}
 * dans le dépôt (voir la story 5.1) : à confirmer contre le legacy.
 */
public final class GenerateurReferenceData {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final long USER1 = 1L;

    /** Prêt du legacy : mois écoulés depuis le début, type, montant, taux (%), durée (mois), objet. */
    private record PretReference(long id, int moisEcoules, String type, String montant, String taux, int duree,
            String objet) {
    }

    private static final List<PretReference> PRETS = List.of(
            new PretReference(1, 4, "TRAVAUX", "5000", "4.9", 12, "Travaux de renovation"),
            new PretReference(2, 2, "AUTO", "9000", "3.9", 6, "Achat vehicule"));

    private GenerateurReferenceData() {
    }

    /** Écrit le fichier désigné par le premier argument (par défaut {@code reference-data/reference-data.sql}). */
    public static void main(String[] arguments) throws IOException {
        Path fichier = Path.of(arguments.length > 0 ? arguments[0] : "reference-data/reference-data.sql");
        Horloge horloge = new HorlogeSysteme("Europe/Paris");
        Files.writeString(fichier, generer(horloge.aujourdhui()), StandardCharsets.UTF_8);
        System.out.println("reference-data.sql généré : " + PRETS.size() + " prêts, relatifs au " + horloge.aujourdhui());
    }

    /** Rend le contenu de {@code reference-data.sql} pour la date du jour donnée. */
    public static String generer(LocalDate aujourdhui) {
        StringBuilder sql = new StringBuilder("""
                -- Données de référence du service credits : 2 prêts EN_COURS de user1 (identifiant client 1) et leurs
                -- 18 échéances, identifiants numériques. Hors migrations : chargé dans une base déjà migrée par Flyway.
                -- Valeurs reconstituées (import.sql du legacy absent du dépôt), à confirmer contre le legacy.
                -- Généré par reference-data/generer-reference-data.sh : ne pas modifier à la main.
                -- Dates relatives à la date de génération (Horloge) : début = 1er d'un mois passé.
                """);
        StringBuilder echeances = new StringBuilder();
        long idEcheance = 1;
        for (PretReference pret : PRETS) {
            LocalDate debut = aujourdhui.withDayOfMonth(1).minusMonths(pret.moisEcoules());
            Simulation simulation = Annuite.simuler(DemandeSimulation.lire(pret.montant(), pret.taux(),
                    Integer.toString(pret.duree())));
            sql.append("insert into loans (id, customer_id, type, montant, taux_annuel, duree_mois, mensualite,"
                    + " cout_total, total_interets, motif, status, date_debut, date_fin)\nvalues (")
                    .append(pret.id()).append(", ").append(USER1).append(", ").append(texte(pret.type())).append(", ")
                    .append(montant(new BigDecimal(pret.montant()))).append(", ")
                    .append(new BigDecimal(pret.taux()).setScale(4)).append(", ").append(pret.duree()).append(", ")
                    .append(montant(simulation.mensualite())).append(", ").append(montant(simulation.coutTotal()))
                    .append(", ").append(montant(simulation.totalInterets())).append(", ").append(texte(pret.objet()))
                    .append(", 'EN_COURS', ").append(date(debut)).append(", ")
                    .append(date(debut.plusMonths(pret.duree()))).append(");\n");
            for (LignePret ligne : simulation.echeancier()) {
                LocalDate echeance = debut.plusMonths(ligne.numero());
                boolean payee = !echeance.isAfter(aujourdhui);
                echeances.append("insert into loan_repayments (id, loan_id, numero_echeance, date_echeance,"
                        + " montant_capital, montant_interets, montant_total, capital_restant, paye, date_paiement)\n"
                        + "values (")
                        .append(idEcheance++).append(", ").append(pret.id()).append(", ").append(ligne.numero())
                        .append(", ").append(date(echeance)).append(", ").append(montant(ligne.capital())).append(", ")
                        .append(montant(ligne.interets())).append(", ").append(montant(ligne.total())).append(", ")
                        .append(montant(ligne.capitalRestant())).append(", ").append(payee).append(", ")
                        .append(payee ? instant(echeance) : "null").append(");\n");
            }
        }
        sql.append(echeances);
        sql.append("select setval(pg_get_serial_sequence('loans', 'id'), (select max(id) from loans));\n");
        sql.append("select setval(pg_get_serial_sequence('loan_repayments', 'id'),"
                + " (select max(id) from loan_repayments));\n");
        return sql.toString();
    }

    private static String montant(BigDecimal valeur) {
        return valeur.toPlainString();
    }

    private static String date(LocalDate jour) {
        return "date " + texte(jour.toString());
    }

    private static String instant(LocalDate jour) {
        return "timestamptz " + texte(jour.atTime(10, 0).atZone(PARIS).toInstant().toString());
    }

    private static String texte(String valeur) {
        return "'" + valeur.replace("'", "''") + "'";
    }
}

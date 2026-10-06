package fr.nextbanking.coeurbancaire.domain;

/**
 * Le contrôle de fraude n'a pas pu décider (accès à l'historique en erreur ou trop long) : le virement est refusé pour
 * erreur de contrôle, sans aucun débit.
 */
public class ControleEnErreur extends RuntimeException {

    public ControleEnErreur(Throwable origine) {
        super("Contrôle de fraude en erreur", origine);
    }
}

package fr.nextbanking.documents.application.port.out;

import fr.nextbanking.documents.domain.DocumentPdf;
import fr.nextbanking.documents.domain.DonneesDocument;
import fr.nextbanking.documents.domain.TypeDocument;

/**
 * Port sortant : seul producteur de PDF du système. Une panne ou un type dont la génération n'est pas encore livrée
 * lève une exception non contrôlée, jamais un document partiel.
 */
public interface GenerateurPdf {

    /** Génère le document du type demandé pour ces données. */
    DocumentPdf generer(TypeDocument type, DonneesDocument donnees);
}

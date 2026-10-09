package fr.nextbanking.documents.application.port.out;

import java.util.function.Supplier;

/**
 * Port sortant : exécute un travail dans une transaction, sans annotation dans l'application.
 */
public interface Transaction {

    /** Exécute le travail dans la transaction courante, ou dans une nouvelle s'il n'y en a pas. */
    <T> T executer(Supplier<T> travail);
}

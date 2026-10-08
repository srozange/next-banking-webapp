import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router';
import { ROUTE_CONNEXION } from '../../../domain/connexion';
import {
  estCredit,
  formaterMontantTransaction,
  formaterSolde,
  libelleTypeCompte,
  type EtatComptesTiers,
} from '../../../domain/comptesTiers';
import { formaterDateEcheance, formaterMontantListe } from '../../../domain/pret';
import {
  chargerComptesTiers,
  connecterBanque,
  deconnecterPowens,
  rafraichirComptes,
  selectionnerCompte,
  type ClientComptesTiers,
  type ResultatTiers,
} from '../../../application/comptesTiers';
import type { SessionLocale } from '../../../application/session';
import { TEXTES } from '../../textes';
import { AlerteErreur } from '../../ui/AlerteErreur';
import { BoutonPrimaire } from '../../ui/BoutonPrimaire';
import { BoutonSecondaire } from '../../ui/BoutonSecondaire';
import { Carte } from '../../ui/Carte';
import './EcranComptesTiers.css';

const T = TEXTES.comptesTiers;

/** Au plus 20 transactions affichées (FR-012-13). */
const TRANSACTIONS_MAX = 20;

interface Proprietes {
  client: ClientComptesTiers;
  session: SessionLocale;
  /** Envoie le navigateur à l'adresse de Powens ; par défaut `window.location.assign`, substituable en test. */
  allerVers?: (adresse: string) => void;
}

const ETAT_VIDE: EtatComptesTiers = { connecte: false, comptes: [], compteSelectionne: null, transactions: [], message: '', patrimoineTotal: '0.00' };

const aller = (adresse: string) => window.location.assign(adresse);

/**
 * Écran /comptes-tiers (FEAT-012), identique pour CLIENT, CONSEILLER et ADMIN : connexion d'une banque par la page web
 * de Powens, patrimoine total, comptes externes, transactions du compte sélectionné, rafraîchissement et déconnexion.
 * L'état vient du service ; rien de cet état n'est écrit dans le navigateur.
 */
export function EcranComptesTiers({ client, session, allerVers = aller }: Proprietes) {
  const naviguer = useNavigate();
  const [etat, setEtat] = useState<EtatComptesTiers | null>(null);
  /** Message d'une action qui a échoué (prioritaire sur le message de l'état). */
  const [erreur, setErreur] = useState<string | null>(null);
  /** Compte cliqué dont les transactions sont indisponibles (panne de Powens, BUG-043). */
  const [indisponiblePour, setIndisponiblePour] = useState<number | null>(null);
  const [occupe, setOccupe] = useState(false);
  const envoiEnCours = useRef(false);

  const finDeSession = () => void naviguer(ROUTE_CONNEXION, { replace: true });

  useEffect(() => {
    let actif = true;
    void chargerComptesTiers(client, session).then((resultat) => {
      if (!actif) {
        return;
      }
      if (resultat.type === 'deconnecte') {
        finDeSession();
      } else if (resultat.type === 'ok') {
        setEtat(resultat.valeur);
      } else {
        setEtat(ETAT_VIDE);
        setErreur(TEXTES.erreurTechnique);
      }
    });
    return () => {
      actif = false;
    };
    // Chargement unique à l'ouverture de l'écran.
  }, []);

  /** Une action à la fois : applique le résultat, ou le message qui convient à l'échec. */
  async function agir<T>(
    lancer: () => Promise<ResultatTiers<T>>,
    reussite: (valeur: T) => void,
    messageIndisponible: string,
    echec?: () => void,
    messageTechnique: string = TEXTES.erreurTechnique,
  ) {
    if (envoiEnCours.current) {
      return;
    }
    envoiEnCours.current = true;
    setOccupe(true);
    const resultat = await lancer();
    envoiEnCours.current = false;
    setOccupe(false);
    if (resultat.type === 'deconnecte') {
      finDeSession();
    } else if (resultat.type === 'ok') {
      setErreur(null);
      reussite(resultat.valeur);
    } else {
      echec?.();
      setErreur(resultat.type === 'indisponible' ? messageIndisponible : messageTechnique);
    }
  }

  const connecter = () =>
    agir(
      () => connecterBanque(client, session),
      (adresse) => allerVers(adresse),
      T.connexionImpossible,
      undefined,
      T.connexionImpossible,
    );

  const choisir = (compteId: number) => {
    if (compteId === etat?.compteSelectionne && indisponiblePour === null) {
      return;
    }
    return agir(
      () => selectionnerCompte(client, session, compteId),
      (nouvelEtat) => {
        setIndisponiblePour(null);
        setEtat(nouvelEtat);
      },
      T.powensIndisponible,
      () => setIndisponiblePour(compteId),
    );
  };

  const rafraichir = () =>
    agir(
      () => rafraichirComptes(client, session),
      (nouvelEtat) => {
        setIndisponiblePour(null);
        setEtat(nouvelEtat);
      },
      T.powensIndisponible,
    );

  const deconnecter = () =>
    agir(
      () => deconnecterPowens(client, session),
      () => {
        setIndisponiblePour(null);
        setEtat(ETAT_VIDE);
      },
      T.powensIndisponible,
    );

  if (etat === null) {
    return (
      <div className="ecran-tiers" aria-busy="true" data-testid="tiers-contenu">
        <title>{TEXTES.titresOnglet.comptesTiers}</title>
      </div>
    );
  }

  const message = erreur ?? etat.message;
  const consulte = indisponiblePour ?? etat.compteSelectionne;
  const compteConsulte = etat.comptes.find((compte) => compte.id === consulte) ?? null;

  return (
    <div className="ecran-tiers" aria-busy={occupe ? 'true' : undefined} data-testid="tiers-contenu">
      <title>{TEXTES.titresOnglet.comptesTiers}</title>
      {message !== '' && (
        <AlerteErreur testId="tiers-message-erreur">
          <p>{message}</p>
        </AlerteErreur>
      )}
      {etat.connecte ? (
        <>
          <div className="ecran-tiers__barre">
            <span className="ecran-tiers__statut">{T.statut}</span>
            <div className="ecran-tiers__actions">
              <BoutonSecondaire data-testid="tiers-deconnecter" onClick={() => void deconnecter()}>
                {T.deconnecter}
              </BoutonSecondaire>
              <BoutonSecondaire data-testid="tiers-rafraichir" onClick={() => void rafraichir()}>
                {T.rafraichir}
              </BoutonSecondaire>
            </div>
          </div>
          <div className="ecran-tiers__patrimoine" data-testid="tiers-patrimoine">
            <span className="ecran-tiers__libelle">{T.patrimoine.libelle}</span>
            <span className="ecran-tiers__total">{formaterMontantListe(etat.patrimoineTotal)}</span>
            <span className="ecran-tiers__fournisseur">{T.patrimoine.fournisseur}</span>
          </div>
          <section aria-labelledby="tiers-titre-comptes">
            <h2 id="tiers-titre-comptes" className="ecran-tiers__titre">
              {T.comptes.titre(etat.comptes.length)}
            </h2>
            <ul className="ecran-tiers__comptes">
              {etat.comptes.map((compte) => (
                <li key={compte.id}>
                  <button
                    type="button"
                    className={compte.id === consulte ? 'ecran-tiers__compte ecran-tiers__compte--selectionne' : 'ecran-tiers__compte'}
                    aria-pressed={compte.id === consulte}
                    data-testid={`tiers-compte-${compte.id}`}
                    onClick={() => void choisir(compte.id)}
                  >
                    <span className="ecran-tiers__banque">{T.comptes.banque}</span>
                    <span className="ecran-tiers__nom">{compte.nom}</span>
                    <span className="ecran-tiers__type">{libelleTypeCompte(compte.type)}</span>
                    <span className="ecran-tiers__solde">{formaterSolde(compte)}</span>
                    {compte.iban !== null && compte.iban !== '' && <span className="ecran-tiers__iban">{compte.iban}</span>}
                  </button>
                </li>
              ))}
            </ul>
          </section>
          {compteConsulte !== null && (
            <section aria-labelledby="tiers-titre-transactions" data-testid="tiers-transactions">
              <h2 id="tiers-titre-transactions" className="ecran-tiers__titre">
                {T.transactions.titre(compteConsulte.nom)}
              </h2>
              {indisponiblePour !== null ? (
                <p className="ecran-tiers__vide">{T.powensIndisponible}</p>
              ) : etat.transactions.length === 0 ? (
                <p className="ecran-tiers__vide">{T.transactions.aucune}</p>
              ) : (
                <Carte>
                  <table className="ecran-tiers__tableau" aria-labelledby="tiers-titre-transactions">
                    <thead>
                      <tr>
                        <th scope="col">{T.transactions.date}</th>
                        <th scope="col">{T.transactions.libelle}</th>
                        <th scope="col" className="ecran-tiers__droite">
                          {T.transactions.montant}
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      {etat.transactions.slice(0, TRANSACTIONS_MAX).map((transaction, indice) => (
                        <tr key={indice}>
                          <td>{formaterDateEcheance(transaction.date)}</td>
                          <td>{transaction.libelle}</td>
                          <td
                            className={`ecran-tiers__droite ${estCredit(transaction.montant) ? 'ecran-tiers--credit' : 'ecran-tiers--debit'}`}
                          >
                            {formaterMontantTransaction(transaction.montant)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </Carte>
              )}
            </section>
          )}
        </>
      ) : (
        <Carte>
          <section className="ecran-tiers__connexion" aria-labelledby="tiers-titre-connexion">
            <h2 id="tiers-titre-connexion" className="ecran-tiers__titre">
              {T.connexion.titre}
            </h2>
            <p>{T.connexion.explication}</p>
            <ul className="ecran-tiers__banques">
              {T.connexion.banques.map((banque) => (
                <li key={banque}>{banque}</li>
              ))}
            </ul>
            <BoutonPrimaire testId="tiers-connecter-banque" type="button" occupe={occupe} onClick={() => void connecter()}>
              {T.connexion.connecter}
            </BoutonPrimaire>
          </section>
        </Carte>
      )}
    </div>
  );
}

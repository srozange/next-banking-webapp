import { useEffect, useRef, useState, type RefObject } from 'react';
import { useNavigate, useOutletContext } from 'react-router';
import { ROUTE_CONNEXION } from '../../../domain/connexion';
import { centimesDepuisChaine } from '../../../domain/montant';
import {
  carteDuClient,
  coordonnees,
  formaterEurosArrondi,
  initiale,
  statutAvecBadge,
  type CarteClient,
  type Portefeuille,
} from '../../../domain/portefeuille';
import { formaterMontantListe } from '../../../domain/pret';
import { formaterMontantTableau } from '../../../domain/tableauDeBord';
import type { ResultatProfil } from '../../../application/chargerProfil';
import { chargerPortefeuille, type ClientPortefeuille, type ResultatPortefeuille } from '../../../application/consulterPortefeuille';
import type { SessionLocale } from '../../../application/session';
import { useMemoireSession } from '../../gabarit/memoireSession';
import { TEXTES } from '../../textes';
import { AlerteErreur } from '../../ui/AlerteErreur';
import { BoutonSecondaire } from '../../ui/BoutonSecondaire';
import { Carte } from '../../ui/Carte';
import { Etiquette } from '../../ui/Etiquette';
import { Tableau } from '../../ui/Tableau';
import './EcranEspaceConseiller.css';

const T = TEXTES.espaceConseiller;
const COLONNES_COMPTES = [T.colonnesComptes.numero, T.colonnesComptes.type, T.colonnesComptes.solde];
const COLONNES_PRETS = [T.colonnesPrets.type, T.colonnesPrets.montant, T.colonnesPrets.mensualite, T.colonnesPrets.statut];

/** Clé du portefeuille dans la mémoire de session. */
export const CLE_PORTEFEUILLE = 'espace-conseiller.portefeuille';

interface Proprietes {
  client: ClientPortefeuille;
  session: SessionLocale;
}

interface EtatAffiche {
  portefeuille: Portefeuille | null;
  messages: readonly string[];
}

/**
 * Écran /espace-conseiller (FEAT-015), pour CONSEILLER et ADMIN (un CLIENT est redirigé par la garde) : bandeau
 * d'accueil, trois indicateurs, une carte par client et un panneau de détail inline (FR-015-02 à FR-015-09). Les trois
 * lectures partent ensemble, une fois par session : la promesse est gardée par la mémoire du gabarit authentifié, et
 * oubliée si le résultat n'est pas entièrement ok. Une panne affiche les messages reçus, sans indicateurs ni cartes.
 */
export function EcranEspaceConseiller({ client, session }: Proprietes) {
  const naviguer = useNavigate();
  const profil = useOutletContext<ResultatProfil | null>();
  const memoire = useMemoireSession();
  const [etat, setEtat] = useState<EtatAffiche | null>(null);
  const [selection, setSelection] = useState<number | null>(null);
  const titreDetail = useRef<HTMLHeadingElement>(null);
  /** Boutons « Voir detail » par client : le focus y revient à la fermeture du panneau. */
  const declencheurs = useRef(new Map<number, HTMLButtonElement>());

  useEffect(() => {
    let actif = true;
    void memoire
      .obtenir<ResultatPortefeuille>(CLE_PORTEFEUILLE, () => chargerPortefeuille(client, session))
      .then((resultat) => {
        if (resultat.type === 'deconnecte' || resultat.portefeuille === null) {
          memoire.oublier(CLE_PORTEFEUILLE);
        }
        if (!actif) {
          return;
        }
        if (resultat.type === 'deconnecte') {
          void naviguer(ROUTE_CONNEXION, { replace: true });
          return;
        }
        setEtat(resultat);
      });
    return () => {
      actif = false;
    };
  }, [client, session, memoire, naviguer]);

  useEffect(() => {
    if (selection === null) {
      return;
    }
    titreDetail.current?.focus();
    const surTouche = (evenement: KeyboardEvent) => {
      if (evenement.key === 'Escape') {
        fermer(selection);
      }
    };
    document.addEventListener('keydown', surTouche);
    return () => document.removeEventListener('keydown', surTouche);
  }, [selection]);

  function fermer(clientId: number) {
    setSelection(null);
    declencheurs.current.get(clientId)?.focus();
  }

  const portefeuille = etat?.portefeuille ?? null;
  const selectionnee = portefeuille !== null && selection !== null ? carteDuClient(portefeuille, selection) : null;
  return (
    <div className="ecran-espace-conseiller" aria-busy={etat === null ? 'true' : undefined} data-testid="conseiller-contenu">
      <title>{TEXTES.titresOnglet.espaceConseiller}</title>
      <p className="ecran-espace-conseiller__bandeau" data-testid="conseiller-bandeau">
        <strong>{T.titre}</strong>
        {portefeuille !== null && profil?.type === 'profil' && (
          <>
            {T.separateur}
            {T.bonjour(profil.nomComplet)}
            <strong>{T.nombreClients(portefeuille.nombreClients)}</strong>
          </>
        )}
      </p>
      {etat !== null && etat.messages.length > 0 && (
        <AlerteErreur testId="conseiller-message-erreur">
          {etat.messages.map((message) => (
            <p key={message}>{message}</p>
          ))}
        </AlerteErreur>
      )}
      {portefeuille !== null && (
        <>
          <Indicateurs portefeuille={portefeuille} />
          {portefeuille.cartes.length === 0 ? (
            <div className="ecran-espace-conseiller__vide" data-testid="conseiller-vide">
              <p>{T.vide}</p>
              <p>{T.videContact}</p>
            </div>
          ) : (
            <ul className="ecran-espace-conseiller__cartes">
              {portefeuille.cartes.map((carte) => (
                <CarteDuClient
                  key={carte.client.clientId}
                  carte={carte}
                  refBouton={(bouton) => {
                    if (bouton === null) {
                      declencheurs.current.delete(carte.client.clientId);
                    } else {
                      declencheurs.current.set(carte.client.clientId, bouton);
                    }
                  }}
                  surVoirDetail={() => setSelection(carte.client.clientId)}
                />
              ))}
            </ul>
          )}
          {selectionnee !== null && (
            <PanneauDetail carte={selectionnee} refTitre={titreDetail} surFermer={() => fermer(selectionnee.client.clientId)} />
          )}
        </>
      )}
    </div>
  );
}

function Indicateurs({ portefeuille }: { portefeuille: Portefeuille }) {
  const I = T.indicateurs;
  const indicateurs = [
    { cle: 'clients', libelle: I.clients, valeur: String(portefeuille.nombreClients), sousLibelle: I.clientsSousLibelle },
    { cle: 'encours', libelle: I.encours, valeur: formaterEurosArrondi(portefeuille.encoursCentimes), sousLibelle: I.encoursSousLibelle },
    { cle: 'prets-en-attente', libelle: I.pretsEnAttente, valeur: String(portefeuille.pretsEnAttente), sousLibelle: I.pretsEnAttenteSousLibelle },
  ];
  return (
    <div className="ecran-espace-conseiller__indicateurs">
      {indicateurs.map(({ cle, libelle, valeur, sousLibelle }) => (
        <Carte key={cle} variante="muted">
          <div data-testid={`conseiller-indicateur-${cle}`}>
            <p className="ecran-espace-conseiller__libelle-indicateur">{libelle}</p>
            <p className="ecran-espace-conseiller__valeur-indicateur" data-testid={`conseiller-indicateur-${cle}-valeur`}>
              {valeur}
            </p>
            <p className="ecran-espace-conseiller__sous-libelle">{sousLibelle}</p>
          </div>
        </Carte>
      ))}
    </div>
  );
}

interface ProprietesCarte {
  carte: CarteClient;
  refBouton: (bouton: HTMLButtonElement | null) => void;
  surVoirDetail: () => void;
}

function CarteDuClient({ carte, refBouton, surVoirDetail }: ProprietesCarte) {
  const { client, indicateur } = carte;
  const S = T.statistiques;
  const statistiques = [
    { libelle: S.soldeTotal, valeur: formaterEurosArrondi(carte.soldeTotalCentimes) },
    { libelle: S.comptes, valeur: String(carte.nombreComptes) },
    { libelle: S.pretsActifs, valeur: String(carte.pretsEnCours) },
    { libelle: S.pretsEnAttente, valeur: String(carte.pretsEnAttente) },
  ];
  const classe = client.actif ? 'ecran-espace-conseiller__carte' : 'ecran-espace-conseiller__carte ecran-espace-conseiller__carte--inactive';
  return (
    <li className={classe} data-testid={`conseiller-carte-${client.clientId}`}>
      <Carte>
        <div className="ecran-espace-conseiller__identite">
          <span className="ecran-espace-conseiller__avatar" aria-hidden="true">
            {initiale(client.nomComplet)}
          </span>
          <div className="ecran-espace-conseiller__nom-login">
            <h2 className="ecran-espace-conseiller__nom">{client.nomComplet}</h2>
            <p className="ecran-espace-conseiller__login">{`@${client.login}`}</p>
          </div>
          <Etiquette data-testid={`conseiller-statut-client-${client.clientId}`}>{client.actif ? T.actif : T.inactif}</Etiquette>
        </div>
        <dl className="ecran-espace-conseiller__statistiques">
          {statistiques.map(({ libelle, valeur }) => (
            <div key={libelle} className="ecran-espace-conseiller__statistique">
              <dt>{libelle}</dt>
              <dd>{valeur}</dd>
            </div>
          ))}
        </dl>
        <div className="ecran-espace-conseiller__pied">
          {indicateur.type === 'attente' && (
            <p className="ecran-espace-conseiller__alerte-prets" data-testid={`conseiller-indicateur-prets-${client.clientId}`}>
              {T.alertePretsEnAttente(indicateur.nombre)}
            </p>
          )}
          {indicateur.type === 'cours' && (
            <p className="ecran-espace-conseiller__prets-en-cours" data-testid={`conseiller-indicateur-prets-${client.clientId}`}>
              {T.pretsEnCours(indicateur.nombre)}
            </p>
          )}
          <BoutonSecondaire ref={refBouton} data-testid={`conseiller-voir-detail-${client.clientId}`} onClick={surVoirDetail}>
            {T.voirDetail}
          </BoutonSecondaire>
        </div>
      </Carte>
    </li>
  );
}

interface ProprietesDetail {
  carte: CarteClient;
  refTitre: RefObject<HTMLHeadingElement | null>;
  surFermer: () => void;
}

function PanneauDetail({ carte, refTitre, surFermer }: ProprietesDetail) {
  const { client, comptes, prets } = carte;
  return (
    <section className="ecran-espace-conseiller__detail" aria-labelledby="conseiller-detail-titre" data-testid="conseiller-detail">
      <Carte variante="muted">
        <div className="ecran-espace-conseiller__detail-entete">
          <div>
            <h2 id="conseiller-detail-titre" ref={refTitre} tabIndex={-1} className="ecran-espace-conseiller__detail-titre">
              {client.nomComplet}
            </h2>
            <p className="ecran-espace-conseiller__login">{coordonnees(client)}</p>
          </div>
          <BoutonSecondaire data-testid="conseiller-fermer-detail" onClick={surFermer}>
            {T.fermer}
          </BoutonSecondaire>
        </div>
        <div className="ecran-espace-conseiller__detail-colonnes">
          <section aria-labelledby="conseiller-detail-comptes">
            <h3 id="conseiller-detail-comptes" className="ecran-espace-conseiller__separateur">
              {T.comptesBancaires}
            </h3>
            {comptes.length === 0 ? (
              <p className="ecran-espace-conseiller__aucun">{T.aucunCompte}</p>
            ) : (
              <Tableau testId="conseiller-detail-comptes" colonnes={COLONNES_COMPTES}>
                {comptes.map((compte) => (
                  <tr key={compte.compteId}>
                    <td>{compte.numero}</td>
                    <td>
                      <Etiquette data-testid={`conseiller-type-compte-${compte.compteId}`}>{compte.type}</Etiquette>
                    </td>
                    <td className="ecran-espace-conseiller__solde">{formaterMontantTableau(centimesDepuisChaine(compte.solde))}</td>
                  </tr>
                ))}
              </Tableau>
            )}
          </section>
          <section aria-labelledby="conseiller-detail-prets">
            <h3 id="conseiller-detail-prets" className="ecran-espace-conseiller__separateur">
              {T.prets}
            </h3>
            {prets.length === 0 ? (
              <p className="ecran-espace-conseiller__aucun">{T.aucunPret}</p>
            ) : (
              <Tableau testId="conseiller-detail-prets" colonnes={COLONNES_PRETS}>
                {prets.map((pret) => (
                  <tr key={pret.pretId}>
                    <td>{pret.type ?? ''}</td>
                    <td>{formaterEurosArrondi(centimesDepuisChaine(pret.montant))}</td>
                    <td>{formaterMontantListe(pret.mensualite)}</td>
                    <td>
                      {statutAvecBadge(pret.statut) ? (
                        <Etiquette data-testid={`conseiller-statut-pret-${pret.pretId}`}>{pret.statut}</Etiquette>
                      ) : (
                        <span data-testid={`conseiller-statut-pret-${pret.pretId}`}>{pret.statut}</span>
                      )}
                    </td>
                  </tr>
                ))}
              </Tableau>
            )}
          </section>
        </div>
      </Carte>
    </section>
  );
}

import { useEffect, useRef, useState, type FormEvent, type RefObject } from 'react';
import { changerEtatClient, type ClientAdministration, type PartieAdministration } from '../../../application/consulterAdministration';
import type { SessionLocale } from '../../../application/session';
import { clientDepuis, filtrerClients, type ClientAdministre } from '../../../domain/administration';
import { TEXTES } from '../../textes';
import { AlerteErreur } from '../../ui/AlerteErreur';
import { AlerteSucces } from '../../ui/AlerteSucces';
import { BoutonFantome } from '../../ui/BoutonFantome';
import { BoutonSecondaire } from '../../ui/BoutonSecondaire';
import { Carte } from '../../ui/Carte';
import { Etiquette } from '../../ui/Etiquette';
import { Tableau } from '../../ui/Tableau';

const G = TEXTES.administration.gestionClients;
const D = TEXTES.administration.detail;
const COLONNES = [G.colonnes.nom, G.colonnes.login, G.colonnes.email, G.colonnes.role, G.colonnes.statut, G.colonnes.conseiller, G.colonnes.actions];

interface Proprietes {
  /** Clients (rôle CLIENT seulement, décidé par le serveur) ; null pendant le chargement. */
  partie: PartieAdministration<readonly ClientAdministre[]> | null;
  client: ClientAdministration;
  session: SessionLocale;
  /** Appelé quand la session est terminée (401 ou session fermée) : retour à la connexion. */
  surDeconnexion: () => void;
}

/** Message de la dernière activation ou désactivation ; chaque nouvelle action remplace le précédent. */
interface Message {
  type: 'succes' | 'erreur';
  texte: string;
}

/**
 * Section « Gestion des clients » de l'écran Administration (FR-016-08 à FR-016-10) : filtre appliqué au clic sur
 * « Filtrer » (ou Entrée), tableau des clients, puis panneau de détail inline sous le tableau (focus au titre ;
 * « Fermer » ou Échap le referme et rend le focus au bouton « Detail » déclencheur). Un nouveau filtrage ferme le
 * panneau. « Client depuis » est la date de création de la fiche (BUG-055 corrigé). « Desactiver » / « Activer »
 * (FR-016-11) agissent sans confirmation : le bouton est `aria-disabled` pendant la requête, et le statut (ligne et
 * panneau) ne bascule, avec « Client désactivé » / « Client activé », qu'après la réponse 2xx de l'API ; en échec, le
 * message reçu s'affiche et rien ne change.
 */
export function SectionGestionClients({ partie, client, session, surDeconnexion }: Proprietes) {
  const [source, setSource] = useState(partie);
  const [clients, setClients] = useState<readonly ClientAdministre[] | null>(partie?.donnees ?? null);
  const [enCours, setEnCours] = useState<ReadonlySet<number>>(new Set());
  const [message, setMessage] = useState<Message | null>(null);
  /** Clients dont la requête est partie : garde synchrone contre un second clic avant le rendu suivant. */
  const demandes = useRef(new Set<number>());
  const [saisie, setSaisie] = useState('');
  const [terme, setTerme] = useState('');
  const [selection, setSelection] = useState<number | null>(null);
  const titreDetail = useRef<HTMLHeadingElement>(null);
  /** Boutons « Detail » par client : le focus y revient à la fermeture du panneau. */
  const declencheurs = useRef(new Map<number, HTMLButtonElement>());

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

  function filtrer(evenement?: FormEvent) {
    evenement?.preventDefault();
    setTerme(saisie);
    setSelection(null);
  }

  if (partie !== source) {
    setSource(partie);
    setClients(partie?.donnees ?? null);
  }

  async function basculer(cible: ClientAdministre) {
    const { clientId } = cible;
    if (demandes.current.has(clientId)) {
      return;
    }
    demandes.current.add(clientId);
    setEnCours((courants) => new Set(courants).add(clientId));
    setMessage(null);
    const actif = !cible.actif;
    const issue = await changerEtatClient(client, session, clientId, actif);
    demandes.current.delete(clientId);
    setEnCours((courants) => {
      const restants = new Set(courants);
      restants.delete(clientId);
      return restants;
    });
    if (issue.type === 'deconnecte') {
      surDeconnexion();
      return;
    }
    if (issue.type === 'erreur') {
      setMessage({ type: 'erreur', texte: issue.message });
      return;
    }
    setClients((courants) => courants?.map((existant) => (existant.clientId === clientId ? { ...existant, actif } : existant)) ?? null);
    setMessage({ type: 'succes', texte: actif ? G.succesActive : G.succesDesactive });
  }

  const affiches = clients === null ? [] : filtrerClients(clients, terme);
  const selectionne = clients?.find((client) => client.clientId === selection) ?? null;
  return (
    <section className="ecran-administration__section" aria-labelledby="admin-clients-titre" data-testid="admin-clients-section">
      <h2 id="admin-clients-titre" className="ecran-administration__titre">
        {G.titre}
      </h2>
      {partie?.message != null && (
        <AlerteErreur testId="admin-message-erreur">
          <p>{partie.message}</p>
        </AlerteErreur>
      )}
      {message !== null &&
        (message.type === 'succes' ? (
          <AlerteSucces testId="admin-message-succes">
            <p>{message.texte}</p>
          </AlerteSucces>
        ) : (
          <AlerteErreur testId="admin-message-erreur">
            <p>{message.texte}</p>
          </AlerteErreur>
        ))}
      {clients !== null && (
        <>
          <form className="ecran-administration__filtre" onSubmit={filtrer}>
            <input
              type="text"
              className="ecran-administration__recherche"
              aria-label={G.recherche}
              placeholder={G.recherche}
              value={saisie}
              onChange={(evenement) => setSaisie(evenement.target.value)}
              autoComplete="off"
              data-testid="admin-recherche"
            />
            <BoutonSecondaire data-testid="admin-filtrer" onClick={() => filtrer()}>
              {G.filtrer}
            </BoutonSecondaire>
          </form>
          <Tableau testId="admin-clients" colonnes={COLONNES}>
            {affiches.length === 0 ? (
              <tr>
                <td colSpan={COLONNES.length}>{G.vide}</td>
              </tr>
            ) : (
              affiches.map((client) => (
                <tr key={client.clientId} data-testid={`admin-client-${client.clientId}`}>
                  <td>{client.nomComplet}</td>
                  <td>{client.login}</td>
                  <td>{client.email}</td>
                  <td>
                    <Etiquette data-testid={`admin-role-${client.clientId}`}>{client.role}</Etiquette>
                  </td>
                  <td>
                    <Etiquette data-testid={`admin-statut-${client.clientId}`}>{client.actif ? G.actif : G.inactif}</Etiquette>
                  </td>
                  <td>{client.nomConseiller ?? ''}</td>
                  <td>
                    <div className="ecran-administration__actions">
                      <BoutonSecondaire
                        ref={(bouton) => {
                          if (bouton === null) {
                            declencheurs.current.delete(client.clientId);
                          } else {
                            declencheurs.current.set(client.clientId, bouton);
                          }
                        }}
                        data-testid={`admin-detail-${client.clientId}`}
                        onClick={() => setSelection(client.clientId)}
                      >
                        {G.detail}
                      </BoutonSecondaire>
                      <BoutonFantome
                        data-testid={`admin-${client.actif ? 'desactiver' : 'activer'}-${client.clientId}`}
                        occupe={enCours.has(client.clientId)}
                        onClick={() => void basculer(client)}
                      >
                        {client.actif ? G.desactiver : G.activer}
                      </BoutonFantome>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </Tableau>
          {selectionne !== null && <PanneauDetail client={selectionne} refTitre={titreDetail} surFermer={() => fermer(selectionne.clientId)} />}
        </>
      )}
    </section>
  );
}

interface ProprietesDetail {
  client: ClientAdministre;
  refTitre: RefObject<HTMLHeadingElement | null>;
  surFermer: () => void;
}

function PanneauDetail({ client, refTitre, surFermer }: ProprietesDetail) {
  const champs = [
    { cle: 'nom-complet', libelle: D.nomComplet, valeur: client.nomComplet },
    { cle: 'identifiant', libelle: D.identifiant, valeur: client.login },
    { cle: 'email', libelle: D.email, valeur: client.email },
    { cle: 'telephone', libelle: D.telephone, valeur: client.telephone ?? '' },
    { cle: 'role', libelle: D.role, valeur: <Etiquette data-testid="admin-panneau-role">{client.role}</Etiquette> },
    {
      cle: 'statut',
      libelle: D.statut,
      valeur: <Etiquette data-testid="admin-panneau-statut">{client.actif ? G.actif : G.inactif}</Etiquette>,
    },
    ...(client.nomConseiller === null ? [] : [{ cle: 'conseiller', libelle: D.conseiller, valeur: client.nomConseiller }]),
    { cle: 'client-depuis', libelle: D.clientDepuis, valeur: clientDepuis(client) },
  ];
  return (
    <section className="ecran-administration__detail" aria-labelledby="admin-detail-titre" data-testid="admin-panneau-detail">
      <Carte variante="muted">
        <div className="ecran-administration__detail-entete">
          <h3 id="admin-detail-titre" ref={refTitre} tabIndex={-1} className="ecran-administration__detail-titre">
            {D.titre(client.nomComplet)}
          </h3>
          <BoutonSecondaire data-testid="admin-fermer-detail" onClick={surFermer}>
            {D.fermer}
          </BoutonSecondaire>
        </div>
        <dl className="ecran-administration__champs">
          {champs.map(({ cle, libelle, valeur }) => (
            <div key={cle} className="ecran-administration__champ">
              <dt>{libelle}</dt>
              <dd data-testid={`admin-panneau-${cle}`}>{valeur}</dd>
            </div>
          ))}
        </dl>
      </Carte>
    </section>
  );
}

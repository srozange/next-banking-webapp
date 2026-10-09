import { useEffect, useRef, useState, type FormEvent, type RefObject } from 'react';
import type { PartieAdministration } from '../../../application/consulterAdministration';
import { clientDepuis, filtrerClients, type ClientAdministre } from '../../../domain/administration';
import { TEXTES } from '../../textes';
import { AlerteErreur } from '../../ui/AlerteErreur';
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
}

/**
 * Section « Gestion des clients » de l'écran Administration (FR-016-08 à FR-016-10) : filtre appliqué au clic sur
 * « Filtrer » (ou Entrée), tableau des clients, puis panneau de détail inline sous le tableau (focus au titre ;
 * « Fermer » ou Échap le referme et rend le focus au bouton « Detail » déclencheur). Un nouveau filtrage ferme le
 * panneau. « Client depuis » est la date de création de la fiche (BUG-055 corrigé).
 */
export function SectionGestionClients({ partie }: Proprietes) {
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

  const clients = partie?.donnees ?? null;
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

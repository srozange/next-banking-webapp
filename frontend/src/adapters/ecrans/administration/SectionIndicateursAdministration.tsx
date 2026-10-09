import type { PartieAdministration } from '../../../application/consulterAdministration';
import { montantATraiter, type IndicateursIdentite, type IndicateursPrets } from '../../../domain/administration';
import { TEXTES } from '../../textes';
import { AlerteErreur } from '../../ui/AlerteErreur';
import { Carte } from '../../ui/Carte';

const I = TEXTES.administration.indicateurs;

interface Proprietes {
  /** Indicateurs d'identite-clients ; null pendant le chargement. */
  identite: PartieAdministration<IndicateursIdentite> | null;
  /** Indicateur des prêts en attente (credits) ; null pendant le chargement. */
  prets: PartieAdministration<IndicateursPrets> | null;
}

interface Indicateur {
  cle: string;
  libelle: string;
  valeur: string;
  sousLibelle: string;
}

/**
 * Indicateurs globaux de l'écran Administration (FR-016-02), en tête de page : « Total clients », « Conseillers »,
 * « Prets en attente », « Total utilisateurs ». Les cartes d'identité dépendent du seul appel identité, la carte des
 * prêts du seul appel credits ; le message d'un appel en échec s'affiche ici, sans empêcher les autres cartes.
 */
export function SectionIndicateursAdministration({ identite, prets }: Proprietes) {
  const id = identite?.donnees ?? null;
  const pr = prets?.donnees ?? null;
  const indicateurs: Indicateur[] = [
    ...(id === null
      ? []
      : [
          { cle: 'total-clients', libelle: I.totalClients, valeur: String(id.totalClients), sousLibelle: I.clientsActifs(id.clientsActifs) },
          { cle: 'conseillers', libelle: I.conseillers, valeur: String(id.conseillers), sousLibelle: I.conseillersSousLibelle },
        ]),
    ...(pr === null
      ? []
      : [{ cle: 'prets-en-attente', libelle: I.pretsEnAttente, valeur: String(pr.pretsEnAttente), sousLibelle: I.aTraiter(montantATraiter(pr)) }]),
    ...(id === null
      ? []
      : [{ cle: 'total-utilisateurs', libelle: I.totalUtilisateurs, valeur: String(id.totalUtilisateurs), sousLibelle: I.totalUtilisateursSousLibelle }]),
  ];
  const messages = [...new Set([identite?.message, prets?.message].filter((message): message is string => typeof message === 'string'))];
  return (
    <div className="ecran-administration__indicateurs-section" data-testid="admin-indicateurs">
      {messages.length > 0 && (
        <AlerteErreur testId="admin-message-erreur">
          {messages.map((message) => (
            <p key={message}>{message}</p>
          ))}
        </AlerteErreur>
      )}
      <div className="ecran-administration__indicateurs">
        {indicateurs.map(({ cle, libelle, valeur, sousLibelle }) => (
          <Carte key={cle} variante="muted">
            <div data-testid={`admin-indicateur-${cle}`}>
              <p className="ecran-administration__libelle-indicateur">{libelle}</p>
              <p className="ecran-administration__valeur-indicateur" data-testid={`admin-indicateur-${cle}-valeur`}>
                {valeur}
              </p>
              <p className="ecran-administration__sous-libelle" data-testid={`admin-indicateur-${cle}-sous-libelle`}>
                {sousLibelle}
              </p>
            </div>
          </Carte>
        ))}
      </div>
    </div>
  );
}

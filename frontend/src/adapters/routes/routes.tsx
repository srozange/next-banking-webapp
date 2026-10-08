import type { RouteObject } from 'react-router';
import { ROUTE_CONNEXION } from '../../domain/connexion';
import type { ClientProfil } from '../../application/chargerProfil';
import type { ClientRevue } from '../../application/revue';
import type { ClientPrets } from '../../application/prets';
import type { ClientBourse } from '../../application/consulterBourse';
import type { ClientConversation } from '../../application/dialoguerAvecSophie';
import type { ClientProduits } from '../../application/consulterProduits';
import type { ClientTableauDeBord } from '../../application/consulterTableauDeBord';
import type { ClientReleve } from '../../application/consulterReleve';
import type { ClientVirement } from '../../application/virement';
import type { ClientComptesTiers } from '../../application/comptesTiers';
import type { ClientConnexion } from '../../application/seConnecter';
import { Application } from '../Application';
import type { Session } from '../session/session';
import { EcranComptesTiers } from '../ecrans/comptesTiers/EcranComptesTiers';
import { EcranBourse } from '../ecrans/bourse/EcranBourse';
import { EcranConseiller } from '../ecrans/conseiller/EcranConseiller';
import { EcranConnexion } from '../ecrans/connexion/EcranConnexion';
import { SectionRevueVirements } from '../ecrans/administration/SectionRevueVirements';
import { EcranProduits } from '../ecrans/produits/EcranProduits';
import { EcranReleve } from '../ecrans/releve/EcranReleve';
import { EcranVirement } from '../ecrans/virement/EcranVirement';
import { EcranTableauDeBord } from '../ecrans/tableauDeBord/EcranTableauDeBord';
import { EcranPrets } from '../ecrans/prets/EcranPrets';
import { TEXTES } from '../textes';
import { ContenuPrincipal } from '../ui/ContenuPrincipal';
import { RedirectionAccueil } from './RedirectionAccueil';
import { RouteProtegee } from './RouteProtegee';

/** Adaptateurs dont dépendent les écrans, assemblés par src/infrastructure. */
export interface Dependances {
  clientConnexion: ClientConnexion;
  clientProfil: ClientProfil;
  clientBourse: ClientBourse;
  clientProduits: ClientProduits;
  clientConversation: ClientConversation;
  clientComptesTiers: ClientComptesTiers;
  /** Envoie le navigateur à une adresse externe (Powens) ; défaut de l'écran Comptes Tiers si absent. */
  allerVers?: (adresse: string) => void;
  session: Session;
  clientVirement: ClientVirement;
  clientRevue: ClientRevue;
  clientReleve: ClientReleve;
  clientTableauDeBord: ClientTableauDeBord;
  clientPrets: ClientPrets;
}

/**
 * Routes protégées : déclarées ici ; le tableau de bord a son écran (comptes, opérations, puis la section « Suivi de mes virements »),
 * l'administration la section de revue des virements suspendus et /prets l'écran Prêts ; le reste du contenu arrive
 * avec les stories suivantes.
 */
export const ROUTES_PROTEGEES = ['/tableau-de-bord', '/espace-conseiller', '/administration', '/prets'] as const;

/** Titre d'onglet des routes protégées sans écran (seul élément rendu en attendant le contenu, hissé dans <head>). */
const TITRES_ONGLET: Readonly<Record<Exclude<(typeof ROUTES_PROTEGEES)[number], '/prets' | '/tableau-de-bord'>, string>> = {
  '/espace-conseiller': TEXTES.titresOnglet.espaceConseiller,
  '/administration': TEXTES.titresOnglet.administration,
};

export function creerRoutes({
  clientConnexion,
  clientProfil,
  clientBourse,
  clientProduits,
  clientConversation,
  clientComptesTiers,
  allerVers,
  session,
  clientVirement,
  clientRevue,
  clientReleve,
  clientTableauDeBord,
  clientPrets,
}: Dependances): RouteObject[] {
  const accueil = <RedirectionAccueil session={session.locale} />;
  return [
    {
      element: <Application />,
      children: [
        {
          path: ROUTE_CONNEXION,
          element: (
            <ContenuPrincipal>
              <EcranConnexion client={clientConnexion} />
            </ContenuPrincipal>
          ),
        },
        {
          // Garde par session puis par rôle, puis gabarit authentifié (en-tête, contenu principal).
          element: <RouteProtegee clientProfil={clientProfil} session={session} />,
          children: [
            // Même écran pour CLIENT, CONSEILLER et ADMIN (FR-009-01).
            { path: '/bourse', element: <EcranBourse client={clientBourse} session={session.locale} /> },
            // Même écran pour CLIENT, CONSEILLER et ADMIN (FR-010-01).
            { path: '/mon-conseiller', element: <EcranConseiller client={clientConversation} session={session.locale} /> },
            // Même écran pour CLIENT, CONSEILLER et ADMIN (FR-011-01).
            { path: '/produits', element: <EcranProduits client={clientProduits} session={session.locale} /> },
            // Même écran pour CLIENT, CONSEILLER et ADMIN (FR-012-01).
            { path: '/comptes-tiers', element: <EcranComptesTiers client={clientComptesTiers} session={session.locale} allerVers={allerVers} /> },
            ...ROUTES_PROTEGEES.map((path) => ({
              path,
              element:
                path === '/prets' ? (
                  <EcranPrets client={clientPrets} session={session.locale} />
                ) : path === '/tableau-de-bord' ? (
                  <EcranTableauDeBord client={clientTableauDeBord} clientVirement={clientVirement} />
                ) : (
                  <>
                    <title>{TITRES_ONGLET[path]}</title>
                    {path === '/administration' && <SectionRevueVirements client={clientRevue} />}
                  </>
                ),
            })),
            { path: '/virement', element: <EcranVirement client={clientVirement} /> },
            // Même écran pour CLIENT, CONSEILLER et ADMIN (FR-005-01) : seuls les comptes du jeton sont proposés.
            { path: '/releve', element: <EcranReleve comptes={clientVirement} releve={clientReleve} /> },
          ],
        },
        { path: '/', element: accueil },
        { path: '*', element: accueil },
      ],
    },
  ];
}

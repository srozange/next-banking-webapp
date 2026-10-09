import type { RouteObject } from 'react-router';
import { ROUTE_CONNEXION } from '../../domain/connexion';
import type { ClientProfil } from '../../application/chargerProfil';
import type { ClientRevue } from '../../application/revue';
import type { ClientPipelinePrets } from '../../application/pipelinePrets';
import type { ClientPrets } from '../../application/prets';
import type { ClientBourse } from '../../application/consulterBourse';
import type { ClientConversation } from '../../application/dialoguerAvecSophie';
import type { ClientProduits } from '../../application/consulterProduits';
import type { ClientTableauDeBord } from '../../application/consulterTableauDeBord';
import type { ClientNotifications } from '../../application/gererNotifications';
import type { ClientReleve } from '../../application/consulterReleve';
import type { ClientBudget } from '../../application/consulterBudget';
import type { ClientPlafonds } from '../../application/gererPlafonds';
import type { ClientDocuments } from '../../application/consulterDocuments';
import type { ClientPortefeuille } from '../../application/consulterPortefeuille';
import { EcranEspaceConseiller } from '../ecrans/espaceConseiller/EcranEspaceConseiller';
import { EcranDocuments } from '../ecrans/documents/EcranDocuments';
import { EcranBudget } from '../ecrans/budget/EcranBudget';
import type { ClientBeneficiaires } from '../../application/gererBeneficiaires';
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
import { SectionsPretsAdministration } from '../ecrans/administration/SectionsPretsAdministration';
import { EcranProduits } from '../ecrans/produits/EcranProduits';
import type { ClientVirementsProgrammes } from '../../application/gererVirementsProgrammes';
import { EcranBeneficiaires } from '../ecrans/beneficiaires/EcranBeneficiaires';
import { EcranReleve } from '../ecrans/releve/EcranReleve';
import { EcranVirement } from '../ecrans/virement/EcranVirement';
import { EcranTableauDeBord } from '../ecrans/tableauDeBord/EcranTableauDeBord';
import { EcranPrets } from '../ecrans/prets/EcranPrets';
import { TEXTES } from '../textes';
import { EcranVirementsProgrammes } from '../ecrans/virementsProgrammes/EcranVirementsProgrammes';
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
  clientPipelinePrets: ClientPipelinePrets;
  clientReleve: ClientReleve;
  clientBeneficiaires: ClientBeneficiaires;
  clientVirementsProgrammes: ClientVirementsProgrammes;
  clientTableauDeBord: ClientTableauDeBord;
  clientNotifications: ClientNotifications;
  clientPrets: ClientPrets;
  clientBudget: ClientBudget;
  clientPlafonds: ClientPlafonds;
  clientDocuments: ClientDocuments;
  clientPortefeuille: ClientPortefeuille;
}

/**
 * Routes protégées : déclarées ici ; le tableau de bord a son écran (comptes, opérations, puis les sections « Centre de notifications » et « Suivi de mes virements »),
 * l'Espace Conseiller son écran (portefeuille), l'administration le pipeline des prêts, l'historique des prêts puis la revue des virements suspendus,
 * et /prets l'écran Prêts ; le reste du contenu arrive avec les stories suivantes.
 */
export const ROUTES_PROTEGEES = ['/tableau-de-bord', '/espace-conseiller', '/administration', '/prets'] as const;

/** Titre d'onglet des routes protégées sans écran propre (hissé dans <head>), suivi de leurs sections. */
const TITRES_ONGLET: Readonly<Record<Exclude<(typeof ROUTES_PROTEGEES)[number], '/prets' | '/tableau-de-bord' | '/espace-conseiller'>, string>> = {
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
  clientPipelinePrets,
  clientReleve,
  clientBeneficiaires,
  clientVirementsProgrammes,
  clientTableauDeBord,
  clientNotifications,
  clientPrets,
  clientBudget,
  clientPlafonds,
  clientDocuments,
  clientPortefeuille,
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
                ) : path === '/espace-conseiller' ? (
                  <EcranEspaceConseiller client={clientPortefeuille} session={session.locale} />
                ) : path === '/tableau-de-bord' ? (
                  <EcranTableauDeBord client={clientTableauDeBord} clientComptesTiers={clientComptesTiers} clientNotifications={clientNotifications} clientVirement={clientVirement} />
                ) : (
                  <>
                    <title>{TITRES_ONGLET[path]}</title>
                    <>
                      <SectionsPretsAdministration client={clientPipelinePrets} />
                      <SectionRevueVirements client={clientRevue} />
                    </>
                  </>
                ),
            })),
            { path: '/virement', element: <EcranVirement client={clientVirement} /> },
            // Même écran pour CLIENT, CONSEILLER et ADMIN (FR-008-01) : seuls les comptes du jeton sont proposés.
            { path: '/documents', element: <EcranDocuments client={clientDocuments} /> },
            // Même écran pour CLIENT, CONSEILLER et ADMIN (FR-005-01) : seuls les comptes du jeton sont proposés.
            { path: '/releve', element: <EcranReleve comptes={clientVirement} releve={clientReleve} /> },
            // Même écran pour CLIENT, CONSEILLER et ADMIN (FR-006-01) : seul le budget du client du jeton est rendu.
            { path: '/budget', element: <EcranBudget client={clientBudget} plafonds={clientPlafonds} /> },
            // Même écran pour CLIENT, CONSEILLER et ADMIN (FR-013-01) : seuls les bénéficiaires du client du jeton sont rendus.
            { path: '/beneficiaires', element: <EcranBeneficiaires client={clientBeneficiaires} /> },
            // Même écran pour CLIENT, CONSEILLER et ADMIN (FR-004-01) : seuls les comptes et virements du client du jeton sont rendus.
            { path: '/virements-programmes', element: <EcranVirementsProgrammes comptes={clientVirement} client={clientVirementsProgrammes} /> },
          ],
        },
        { path: '/', element: accueil },
        { path: '*', element: accueil },
      ],
    },
  ];
}

import { render } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router';
import { vi } from 'vitest';
import type { ClientSession, ReponseRenouvellement } from '../../application/renouvelerSession';
import type { ClientProfil, ReponseProfil } from '../../application/chargerProfil';
import type { ClientRevue, ReponseDecision, ReponseFile, ReponseHistorique } from '../../application/revue';
import type { ClientVirement, ReponseComptes, ReponseSuivi, ReponseVirement } from '../../application/virement';
import type { ClientBourse } from '../../application/consulterBourse';
import type { ClientConversation } from '../../application/dialoguerAvecSophie';
import type { ClientProduits } from '../../application/consulterProduits';
import type { ClientReleve, ReponseOperations } from '../../application/consulterReleve';
import type { ClientBeneficiaires, ReponseAjout, ReponseBeneficiaires, ReponseSuppression, ReponseVerificationIban } from '../../application/gererBeneficiaires';
import type { ClientVirementsProgrammes, ReponseCreation, ReponseVirementsProgrammes } from '../../application/gererVirementsProgrammes';
import type { ClientTableauDeBord, ReponseTableauDeBord } from '../../application/consulterTableauDeBord';
import type { ClientNotifications, ReponseActionNotifications, ReponseNotifications } from '../../application/gererNotifications';
import type { ClientComptesTiers } from '../../application/comptesTiers';
import type {
  ClientPrets,
  ReponseAction,
  ReponseDepot,
  ReponseEcheances,
  ReponsePrets,
  ReponseSimulation,
} from '../../application/prets';
import type { ClientConnexion, ReponseConnexion } from '../../application/seConnecter';
import { accueilDuRole } from '../../domain/connexion';
import { decodeurJetonJwt } from '../jeton/DecodeurJetonJwt';
import { jetonDeTest } from '../jeton/jetonDeTest';
import { creerRoutes } from '../routes/routes';
import { creerExpirationJeton } from '../session/ExpirationJetonJwt';
import { creerSessionLocale } from '../session/SessionLocaleJwt';
import { horlogeSysteme, type Horloge } from '../session/horloge';
import { creerSignalExpiration } from '../session/signalExpiration';
import { creerStockageJetonLocal } from '../stockage/StockageJetonLocal';

/**
 * Outil des tests DOM (jsdom) : rend l'application complète sur un chemin, avec un routeur en mémoire,
 * le vrai stockage localStorage, un client de connexion simulé (qui, comme l'adaptateur HTTP, enregistre un jeton
 * `jetonDeTest({ sub: '1', roles })` quand la connexion est acceptée avec un rôle connu) et un client de profil simulé
 * (par défaut, le profil « Jean Dupont »), un client de session simulé (déconnexion sans effet, renouvellement
 * technique), un client bourse et un client produits et un client de conversation simulés (par défaut, panne technique), un client de relevé simulé (par défaut, aucune opération), un client des bénéficiaires simulé (par défaut, aucune liste, suppression en panne), un client des virements programmés simulé (par défaut, aucune liste, création en panne), une horloge réelle ou simulée, un client de revue simulé (par défaut, files vides), un client des prêts simulé (par défaut : aucun prêt, simulation et dépôt
 * en panne) et un client Comptes Tiers simulé (par défaut, panne technique), un client du tableau de bord simulé (par défaut, aucun compte) et un client des notifications simulé (par défaut, aucune notification) ; `allerVers` simule l'envoi du navigateur à Powens.
 */
export function rendreApplication(
  chemin: string,
  repondre: ClientConnexion['connecter'] = () => Promise.resolve<ReponseConnexion>({ statut: 'refusee' }),
  lireProfil: ClientProfil['lire'] = () => Promise.resolve<ReponseProfil>({ statut: 'profil', nomComplet: 'Jean Dupont' }),
  horloge: Horloge = horlogeSysteme,
  clientSession: Partial<ClientSession> = {},
  clientVirement: Partial<ClientVirement> = {},
  clientRevue: Partial<ClientRevue> = {},
  clientBourse: Partial<ClientBourse> = {},
  clientProduits: Partial<ClientProduits> = {},
  clientConversation: Partial<ClientConversation> = {},
  clientReleve: Partial<ClientReleve> = {},
  clientPrets: Partial<ClientPrets> = {},
  clientComptesTiers: Partial<ClientComptesTiers> = {},
  clientTableauDeBord: Partial<ClientTableauDeBord> = {},
  clientNotifications: Partial<ClientNotifications> = {},
  clientBeneficiaires: Partial<ClientBeneficiaires> = {},
  clientVirementsProgrammes: Partial<ClientVirementsProgrammes> = {},
) {
  const deconnecter = vi.fn(clientSession.deconnecter ?? (() => Promise.resolve()));
  const renouveler = vi.fn(
    clientSession.renouveler ?? (() => Promise.resolve<ReponseRenouvellement>({ statut: 'technique' })),
  );
  const technique = () => Promise.resolve({ statut: 'technique' as const });
  const bourse: ClientBourse = {
    lireWatchlist: vi.fn(clientBourse.lireWatchlist ?? technique),
    lireHistorique: vi.fn(clientBourse.lireHistorique ?? technique),
    ajouter: vi.fn(clientBourse.ajouter ?? technique),
    retirer: vi.fn(clientBourse.retirer ?? technique),
  };
  const produits: ClientProduits = {
    lireCatalogue: vi.fn(clientProduits.lireCatalogue ?? technique),
    lireTraitements: vi.fn(clientProduits.lireTraitements ?? technique),
  };
  const conversation: ClientConversation = {
    envoyer: vi.fn(clientConversation.envoyer ?? technique),
  };
  const lireComptes = vi.fn(clientVirement.lireComptes ?? (() => Promise.resolve<ReponseComptes>({ statut: 'comptes', comptes: [] })));
  const executer = vi.fn(clientVirement.executer ?? (() => Promise.resolve<ReponseVirement>({ statut: 'erreur', message: null })));
  const lireSuivi = vi.fn(clientVirement.lireSuivi ?? (() => Promise.resolve<ReponseSuivi>({ statut: 'suivi', virements: [] })));
  const lireFile = vi.fn(clientRevue.lireFile ?? (() => Promise.resolve<ReponseFile>({ statut: 'liste', virements: [] })));
  const lireHistorique = vi.fn(clientRevue.lireHistorique ?? (() => Promise.resolve<ReponseHistorique>({ statut: 'liste', virements: [] })));
  const decider = vi.fn(clientRevue.decider ?? (() => Promise.resolve<ReponseDecision>({ statut: 'enregistre' })));
  const lireOperations = vi.fn(clientReleve.lireOperations ?? (() => Promise.resolve<ReponseOperations>({ statut: 'operations', operations: [] })));
  const listerBeneficiaires = vi.fn(clientBeneficiaires.lister ?? (() => Promise.resolve<ReponseBeneficiaires>({ statut: 'liste', beneficiaires: [] })));
  const supprimerBeneficiaire = vi.fn(clientBeneficiaires.supprimer ?? (() => Promise.resolve<ReponseSuppression>({ statut: 'technique' })));
  const verifierIbanBeneficiaire = vi.fn(clientBeneficiaires.verifierIban ?? (() => Promise.resolve<ReponseVerificationIban>({ statut: 'technique' })));
  const ajouterBeneficiaire = vi.fn(clientBeneficiaires.ajouter ?? (() => Promise.resolve<ReponseAjout>({ statut: 'technique' })));
  const listerVirementsProgrammes = vi.fn(clientVirementsProgrammes.lister ?? (() => Promise.resolve<ReponseVirementsProgrammes>({ statut: 'liste', virements: [] })));
  const creerVirementProgramme = vi.fn(clientVirementsProgrammes.creer ?? (() => Promise.resolve<ReponseCreation>({ statut: 'technique' })));
  const lireTableauDeBord = vi.fn(
    clientTableauDeBord.lire ?? (() => Promise.resolve<ReponseTableauDeBord>({ statut: 'tableau', comptes: [], dernieresOperations: [] })),
  );
  const listerNotifications = vi.fn(
    clientNotifications.lister ?? (() => Promise.resolve<ReponseNotifications>({ statut: 'liste', notifications: [] })),
  );
  const toutMarquerLu = vi.fn(clientNotifications.toutMarquerLu ?? (() => Promise.resolve<ReponseActionNotifications>({ statut: 'effectuee' })));
  const toutEffacer = vi.fn(clientNotifications.toutEffacer ?? (() => Promise.resolve<ReponseActionNotifications>({ statut: 'effectuee' })));
  const tiers: ClientComptesTiers = {
    lireEtat: vi.fn(clientComptesTiers.lireEtat ?? technique),
    connecter: vi.fn(clientComptesTiers.connecter ?? technique),
    selectionner: vi.fn(clientComptesTiers.selectionner ?? technique),
    rafraichir: vi.fn(clientComptesTiers.rafraichir ?? technique),
    deconnecter: vi.fn(clientComptesTiers.deconnecter ?? technique),
  };
  const allerVers = vi.fn<(adresse: string) => void>();
  const signal = creerSignalExpiration();
  const simuler = vi.fn(clientPrets.simuler ?? (() => Promise.resolve<ReponseSimulation>({ statut: 'technique' })));
  const deposer = vi.fn(clientPrets.deposer ?? (() => Promise.resolve<ReponseDepot>({ statut: 'technique' })));
  const lister = vi.fn(clientPrets.lister ?? (() => Promise.resolve<ReponsePrets>({ statut: 'prets', prets: [] })));
  const echeances = vi.fn(clientPrets.echeances ?? (() => Promise.resolve<ReponseEcheances>({ statut: 'technique' })));
  const payer = vi.fn(clientPrets.payer ?? (() => Promise.resolve<ReponseAction>({ statut: 'technique' })));
  const supprimer = vi.fn(clientPrets.supprimer ?? (() => Promise.resolve<ReponseAction>({ statut: 'technique' })));
  const connecter = vi.fn(repondre);
  const stockage = creerStockageJetonLocal();
  const clientConnexion: ClientConnexion = {
    async connecter(identifiant, motDePasse) {
      const reponse = await connecter(identifiant, motDePasse);
      if (reponse.statut === 'acceptee' && accueilDuRole(reponse.roles) !== null) {
        stockage.enregistrer(jetonDeTest({ sub: '1', roles: reponse.roles }));
      }
      return reponse;
    },
  };
  const lire = vi.fn(lireProfil);
  const routeur = createMemoryRouter(
    creerRoutes({
      clientConnexion,
      clientProfil: { lire },
      clientBourse: bourse,
      clientProduits: produits,
      clientConversation: conversation,
      session: { client: { deconnecter, renouveler }, locale: creerSessionLocale(stockage, decodeurJetonJwt, creerExpirationJeton(horloge)), signal },
      clientVirement: { lireComptes, executer, lireSuivi },
      clientRevue: { lireFile, lireHistorique, decider },
      clientReleve: { lireOperations },
      clientBeneficiaires: { lister: listerBeneficiaires, supprimer: supprimerBeneficiaire, verifierIban: verifierIbanBeneficiaire, ajouter: ajouterBeneficiaire },
      clientVirementsProgrammes: { lister: listerVirementsProgrammes, creer: creerVirementProgramme },
      clientTableauDeBord: { lire: lireTableauDeBord },
      clientNotifications: { lister: listerNotifications, toutMarquerLu, toutEffacer },
      clientComptesTiers: tiers,
      allerVers,
      clientPrets: { simuler, lister, deposer, echeances, payer, supprimer },
    }),
    { initialEntries: [chemin] },
  );
  const rendu = render(<RouterProvider router={routeur} />);
  return { ...rendu, routeur, connecter, lire, deconnecter, renouveler, signal, lireComptes, executer, lireSuivi, lireFile, lireHistorique, decider, bourse, produits, conversation, lireOperations, listerBeneficiaires, supprimerBeneficiaire, verifierIbanBeneficiaire, ajouterBeneficiaire, listerVirementsProgrammes, creerVirementProgramme, lireTableauDeBord, listerNotifications, toutMarquerLu, toutEffacer, tiers, allerVers, simuler, lister, deposer, echeances, payer, supprimer };
}

import { useCallback, useEffect, useState } from 'react';
import { Navigate } from 'react-router';
import { ROUTE_CONNEXION } from '../../../domain/connexion';
import type { Echeance, Pret } from '../../../domain/pret';
import {
  consulterEcheances,
  listerPrets,
  payerEcheance,
  supprimerPret,
  type ClientPrets,
} from '../../../application/prets';
import type { SessionLocale } from '../../../application/session';
import { TEXTES } from '../../textes';
import { SectionDemande } from './SectionDemande';
import { SectionEcheancier } from './SectionEcheancier';
import { SectionEmprunts } from './SectionEmprunts';
import { SectionSimulateur } from './SectionSimulateur';
import './EcranPrets.css';

interface Proprietes {
  client: ClientPrets;
  session: SessionLocale;
}

/**
 * Écran /prets (FEAT-007), le même pour tout rôle authentifié : simulateur, formulaire de demande et liste des
 * emprunts de l'utilisateur, relue après chaque demande transmise, paiement d'échéance ou suppression, et échéancier
 * d'un prêt à la demande. Session perdue : retour à /login.
 */
export function EcranPrets({ client, session }: Proprietes) {
  const [prets, setPrets] = useState<readonly Pret[] | null>(null);
  const [listeIndisponible, setListeIndisponible] = useState(false);
  const [deconnecte, setDeconnecte] = useState(false);
  const [echeancier, setEcheancier] = useState<{ pretId: number; echeances: readonly Echeance[] } | null>(null);
  const [succes, setSucces] = useState<string | null>(null);
  const [erreur, setErreur] = useState<string | null>(null);

  const charger = useCallback(
    async (estActif: () => boolean) => {
      const resultat = await listerPrets(client, session);
      if (!estActif()) {
        return;
      }
      if (resultat.type === 'deconnecte') {
        setDeconnecte(true);
      } else if (resultat.type === 'indisponible') {
        setListeIndisponible(true);
      } else {
        setListeIndisponible(false);
        setPrets(resultat.prets);
      }
    },
    [client, session],
  );

  /** Lit l'échéancier du prêt et l'ouvre ; rend faux (avec le texte d'erreur technique) si la lecture échoue. */
  async function ouvrirEcheancier(pretId: number): Promise<boolean> {
    const resultat = await consulterEcheances(client, session, pretId);
    if (resultat.type === 'deconnecte') {
      setDeconnecte(true);
      return false;
    }
    if (resultat.type === 'indisponible') {
      setErreur(TEXTES.erreurTechnique);
      return false;
    }
    setEcheancier({ pretId, echeances: resultat.echeances });
    return true;
  }

  async function voir(pretId: number) {
    setSucces(null);
    setErreur(null);
    await ouvrirEcheancier(pretId);
  }

  async function payer(echeanceId: number) {
    const resultat = await payerEcheance(client, session, echeanceId);
    if (resultat.type === 'deconnecte') {
      setDeconnecte(true);
      return;
    }
    if (resultat.type === 'indisponible') {
      setSucces(null);
      setErreur(TEXTES.prets.erreurPaiement);
      return;
    }
    setErreur(null);
    setSucces(TEXTES.prets.succesPaiement);
    if (echeancier !== null) {
      await ouvrirEcheancier(echeancier.pretId);
    }
    await charger(() => true);
  }

  async function supprimer(pretId: number) {
    const resultat = await supprimerPret(client, session, pretId);
    if (resultat.type === 'deconnecte') {
      setDeconnecte(true);
      return;
    }
    setSucces(null);
    if (resultat.type === 'indisponible') {
      setErreur(TEXTES.prets.erreurSuppression);
      return;
    }
    setErreur(null);
    if (echeancier?.pretId === pretId) {
      setEcheancier(null);
    }
    await charger(() => true);
  }

  useEffect(() => {
    let actif = true;
    void charger(() => actif);
    return () => {
      actif = false;
    };
  }, [charger]);

  if (deconnecte) {
    return <Navigate to={ROUTE_CONNEXION} replace />;
  }
  return (
    <div className="ecran-prets">
      <title>{TEXTES.titresOnglet.prets}</title>
      <SectionSimulateur client={client} session={session} surDeconnexion={() => setDeconnecte(true)} />
      <SectionDemande
        client={client}
        session={session}
        surDeconnexion={() => setDeconnecte(true)}
        surDepot={() => void charger(() => true)}
      />
      <SectionEmprunts
        prets={prets}
        indisponible={listeIndisponible}
        succes={succes}
        erreur={erreur}
        surVoir={(pretId) => void voir(pretId)}
        surSupprimer={(pretId) => void supprimer(pretId)}
      />
      {echeancier !== null && (
        <SectionEcheancier
          pretId={echeancier.pretId}
          echeances={echeancier.echeances}
          surPayer={(echeanceId) => void payer(echeanceId)}
        />
      )}
    </div>
  );
}

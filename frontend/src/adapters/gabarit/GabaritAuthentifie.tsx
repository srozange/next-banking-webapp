import { useEffect, useState } from 'react';
import { Navigate, Outlet, useLocation, useNavigate } from 'react-router';
import { ROUTE_CONNEXION } from '../../domain/connexion';
import { sousNavigation } from '../../domain/navigation';
import { chargerProfil, type ClientProfil, type ResultatProfil } from '../../application/chargerProfil';
import { renouvelerSession } from '../../application/renouvelerSession';
import { seDeconnecter } from '../../application/seDeconnecter';
import type { Session } from '../session/session';
import { ContenuPrincipal } from '../ui/ContenuPrincipal';
import { EnTete } from './EnTete';
import { SousEnTete } from './SousEnTete';

/** Gestes de l'utilisateur qui comptent comme une activité. */
const EVENEMENTS_ACTIVITE = ['pointerdown', 'keydown'] as const;

interface Proprietes {
  /** Rôles de la session, lus par la garde. */
  roles: readonly string[];
  clientProfil: ClientProfil;
  session: Session;
}

/**
 * Gabarit des pages authentifiées : en-tête (landmark banner), sous-en-tête des espaces réservés, puis contenu
 * principal en aria-busy jusqu'à la réponse de /moi. Profil refusé (401) ou tout autre 401 : retour à /login, sans message ;
 * Deconnexion : service informé, session terminée, retour à /login ; renouvellement de la session sur activité.
 */
export function GabaritAuthentifie({ roles, clientProfil, session }: Proprietes) {
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const [profil, setProfil] = useState<ResultatProfil | null>(null);

  useEffect(() => {
    let actif = true;
    void chargerProfil(clientProfil, session.locale).then((resultat) => {
      if (actif) {
        setProfil(resultat);
      }
    });
    return () => {
      actif = false;
    };
  }, [clientProfil, session]);

  // Tout 401 de tout service (signal) : la session est déjà terminée, retour à /login sans message.
  useEffect(
    () => session.signal.ecouter(() => void navigate(ROUTE_CONNEXION, { replace: true })),
    [session, navigate],
  );

  // Renouvellement sur activité : le cas d'usage n'appelle le service que s'il reste moins de 10 minutes.
  useEffect(() => {
    let actif = true;
    let enCours = false;
    const surActivite = () => {
      if (enCours) {
        return;
      }
      enCours = true;
      void renouvelerSession(session.client, session.locale)
        .then((resultat) => {
          if (actif && resultat === 'deconnecte') {
            void navigate(ROUTE_CONNEXION, { replace: true });
          }
        })
        .finally(() => {
          enCours = false;
        });
    };
    EVENEMENTS_ACTIVITE.forEach((nom) => document.addEventListener(nom, surActivite));
    return () => {
      actif = false;
      EVENEMENTS_ACTIVITE.forEach((nom) => document.removeEventListener(nom, surActivite));
    };
  }, [session, navigate]);

  const deconnecter = () => {
    void navigate(seDeconnecter(session.client, session.locale), { replace: true });
  };

  if (profil?.type === 'deconnecte') {
    return <Navigate to={ROUTE_CONNEXION} replace />;
  }
  const sousNav = sousNavigation(pathname);
  return (
    <>
      <header>
        <EnTete
          roles={roles}
          nomComplet={profil?.type === 'profil' ? profil.nomComplet : null}
          surDeconnexion={deconnecter}
        />
        {sousNav !== null && <SousEnTete navigation={sousNav} />}
      </header>
      <ContenuPrincipal occupe={profil === null}>
        <Outlet context={profil} />
      </ContenuPrincipal>
    </>
  );
}

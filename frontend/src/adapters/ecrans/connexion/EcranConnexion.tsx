import { useEffect, useRef, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router';
import type { ChampConnexion } from '../../../domain/connexion';
import { seConnecter, type ClientConnexion, type StockageJeton } from '../../../application/seConnecter';
import { TEXTES } from '../../textes';
import { AlerteErreur } from '../../ui/AlerteErreur';
import { BoutonPrimaire } from '../../ui/BoutonPrimaire';
import { Carte } from '../../ui/Carte';
import { ChampTexte } from '../../ui/ChampTexte';
import './EcranConnexion.css';

const T = TEXTES.connexion;

interface Proprietes {
  client: ClientConnexion;
  stockage: StockageJeton;
}

/** Erreur globale affichée ; `numero` change à chaque soumission pour reprendre le focus. */
interface ErreurGlobale {
  type: 'refus' | 'technique';
  numero: number;
}

/** Écran /login (FEAT-001) : formulaire soumis par Entrée, validation locale, connexion, accueil du rôle. */
export function EcranConnexion({ client, stockage }: Proprietes) {
  const naviguer = useNavigate();
  const [identifiant, setIdentifiant] = useState('');
  const [motDePasse, setMotDePasse] = useState('');
  const [champsEnErreur, setChampsEnErreur] = useState<readonly ChampConnexion[]>([]);
  const [erreur, setErreur] = useState<ErreurGlobale | null>(null);
  const [occupe, setOccupe] = useState(false);
  const envoiEnCours = useRef(false);
  const champIdentifiant = useRef<HTMLInputElement>(null);
  const champMotDePasse = useRef<HTMLInputElement>(null);
  const alerte = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (erreur !== null) {
      alerte.current?.focus();
    }
  }, [erreur]);

  async function soumettre(evenement: FormEvent<HTMLFormElement>) {
    evenement.preventDefault();
    if (envoiEnCours.current) {
      return;
    }
    envoiEnCours.current = true;
    setOccupe(true);
    const resultat = await seConnecter(identifiant, motDePasse, client, stockage);
    if (resultat.type === 'accepte') {
      naviguer(resultat.route, { replace: true });
      return;
    }
    envoiEnCours.current = false;
    setOccupe(false);
    if (resultat.type === 'champs') {
      setErreur(null);
      setChampsEnErreur(resultat.champs);
      const premier = resultat.champs[0] === 'identifiant' ? champIdentifiant : champMotDePasse;
      premier.current?.focus();
      return;
    }
    setChampsEnErreur([]);
    setErreur((precedente) => ({ type: resultat.type, numero: (precedente?.numero ?? 0) + 1 }));
  }

  return (
    <div className="ecran-connexion">
      <title>{T.titreOnglet}</title>
      <div className="ecran-connexion__cadre">
        <Carte>
          <div className="ecran-connexion__marque">
            <span className="ecran-connexion__logo">{T.logo}</span>
            <h1 className="ecran-connexion__titre">{T.titre}</h1>
          </div>
          <p className="ecran-connexion__sous-titre">{T.sousTitre}</p>
          <form className="ecran-connexion__formulaire" noValidate onSubmit={soumettre}>
            <ChampTexte
              ref={champIdentifiant}
              id="login-identifiant"
              name="username"
              type="text"
              libelle={T.identifiant}
              valeur={identifiant}
              onChange={setIdentifiant}
              autoComplete="username"
              erreur={champsEnErreur.includes('identifiant') ? T.identifiantRequis : undefined}
              testId="login-identifiant"
            />
            <ChampTexte
              ref={champMotDePasse}
              id="login-mot-de-passe"
              name="password"
              type="password"
              libelle={T.motDePasse}
              valeur={motDePasse}
              onChange={setMotDePasse}
              autoComplete="current-password"
              erreur={champsEnErreur.includes('motDePasse') ? T.motDePasseRequis : undefined}
              testId="login-mot-de-passe"
            />
            <BoutonPrimaire testId="login-me-connecter" occupe={occupe}>
              {T.meConnecter}
            </BoutonPrimaire>
            {erreur !== null && (
              <AlerteErreur ref={alerte} testId="login-message-erreur">
                {erreur.type === 'refus' ? (
                  <>
                    <p>{T.identifiantsIncorrects}</p>
                    <p>{T.verifiezLogin}</p>
                  </>
                ) : (
                  <p>{TEXTES.erreurTechnique}</p>
                )}
              </AlerteErreur>
            )}
          </form>
        </Carte>
      </div>
    </div>
  );
}

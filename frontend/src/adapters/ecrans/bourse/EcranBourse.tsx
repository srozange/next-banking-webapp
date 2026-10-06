import { useEffect, useRef, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router';
import { ROUTE_CONNEXION } from '../../../domain/connexion';
import {
  PERIODE_PAR_DEFAUT,
  PERIODES,
  calculerIndicateurs,
  dernieresSeances,
  type CodePeriode,
  type HistoriqueCours,
  type ValeurWatchlist,
} from '../../../domain/bourse';
import { chargerBourse, chargerHistorique, type ClientBourse, type EtatBourse } from '../../../application/consulterBourse';
import { ajouterValeur, retirerValeur } from '../../../application/gererWatchlist';
import type { StockageJeton } from '../../../application/seConnecter';
import { TEXTES } from '../../textes';
import { AlerteErreur } from '../../ui/AlerteErreur';
import { BoutonPrimaire } from '../../ui/BoutonPrimaire';
import { BoutonSecondaire } from '../../ui/BoutonSecondaire';
import { BoutonTexte } from '../../ui/BoutonTexte';
import { Carte } from '../../ui/Carte';
import { ChampTexte } from '../../ui/ChampTexte';
import { ListeDeroulante } from '../../ui/ListeDeroulante';
import './EcranBourse.css';

const T = TEXTES.bourse;

const MARCHE_PAR_DEFAUT = 'Euronext Paris';
const DEVISE_PAR_DEFAUT = 'EUR';
const LONGUEUR_SYMBOLE = 20;
const LONGUEUR_NOM = 100;

const OPTIONS_PERIODE = PERIODES.map((code) => ({ valeur: code, libelle: T.periodes[code].selecteur }));
const OPTIONS_DEVISE = T.devises.map((devise) => ({ valeur: devise, libelle: devise }));

interface Proprietes {
  client: ClientBourse;
  stockage: StockageJeton;
}

const SAISIE_VIDE = { symbole: '', nom: '', marche: MARCHE_PAR_DEFAUT, devise: DEVISE_PAR_DEFAUT };

const classeSens = (hausse: boolean) => (hausse ? 'ecran-bourse--hausse' : 'ecran-bourse--baisse');

/**
 * Écran /bourse (FEAT-009, FEAT-029), identique pour CLIENT, CONSEILLER et ADMIN : watchlist commune, indicateurs,
 * graphique SVG du service, dernières séances, ajout et retrait d'une valeur. Le choix de valeur ou de période
 * recharge seulement l'historique, sans bouton.
 */
export function EcranBourse({ client, stockage }: Proprietes) {
  const naviguer = useNavigate();
  const [etat, setEtat] = useState<EtatBourse | null>(null);
  const [periode, setPeriode] = useState<CodePeriode>(PERIODE_PAR_DEFAUT);
  const [historiqueEnCours, setHistoriqueEnCours] = useState(false);
  const [panne, setPanne] = useState(false);
  const [formulaireOuvert, setFormulaireOuvert] = useState(false);
  const [saisie, setSaisie] = useState(SAISIE_VIDE);
  const [refus, setRefus] = useState<string | null>(null);
  const [confirmation, setConfirmation] = useState(false);
  const [occupe, setOccupe] = useState(false);
  const boutonOuverture = useRef<HTMLButtonElement>(null);
  const alerteRefus = useRef<HTMLDivElement>(null);
  const champSymbole = useRef<HTMLInputElement>(null);
  const envoiEnCours = useRef(false);
  /** Numéro de la dernière demande : une réponse plus ancienne est ignorée. */
  const demande = useRef(0);
  const ouvertureDemandee = useRef(false);
  const fermetureDemandee = useRef(false);

  const finDeSession = () => void naviguer(ROUTE_CONNEXION, { replace: true });

  function appliquer(nouvelEtat: EtatBourse) {
    setEtat(nouvelEtat);
    setPanne(nouvelEtat.technique);
  }

  useEffect(() => {
    let actif = true;
    void chargerBourse(client, stockage, null, PERIODE_PAR_DEFAUT).then((resultat) => {
      if (!actif) {
        return;
      }
      if (resultat.type === 'deconnecte') {
        finDeSession();
      } else if (resultat.type === 'etat') {
        appliquer(resultat.etat);
      } else {
        appliquer({ valeurs: [], selection: null, historique: null, technique: true });
      }
    });
    return () => {
      actif = false;
    };
    // Chargement unique à l'ouverture de l'écran.
  }, []);

  useEffect(() => {
    if (refus !== null) {
      alerteRefus.current?.focus();
    }
  }, [refus]);

  useEffect(() => {
    if (formulaireOuvert && ouvertureDemandee.current) {
      champSymbole.current?.focus();
    }
    if (!formulaireOuvert && fermetureDemandee.current) {
      boutonOuverture.current?.focus();
    }
    ouvertureDemandee.current = false;
    fermetureDemandee.current = false;
  }, [formulaireOuvert]);

  async function rechargerHistorique(symbole: string, codePeriode: CodePeriode) {
    const numero = ++demande.current;
    setHistoriqueEnCours(true);
    setConfirmation(false);
    const resultat = await chargerHistorique(client, stockage, symbole, codePeriode);
    if (numero !== demande.current) {
      return;
    }
    setHistoriqueEnCours(false);
    if (resultat.type === 'deconnecte') {
      finDeSession();
      return;
    }
    const historique = resultat.type === 'historique' ? resultat.historique : null;
    setPanne(resultat.type === 'technique');
    setEtat((precedent) => (precedent === null ? precedent : { ...precedent, historique }));
  }

  function choisirValeur(symbole: string) {
    if (etat === null) {
      return;
    }
    setEtat({ ...etat, selection: symbole, historique: null });
    void rechargerHistorique(symbole, periode);
  }

  function choisirPeriode(code: string) {
    const choisie = PERIODES.find((connue) => connue === code) ?? PERIODE_PAR_DEFAUT;
    setPeriode(choisie);
    if (etat?.selection != null) {
      setEtat({ ...etat, historique: null });
      void rechargerHistorique(etat.selection, choisie);
    }
  }

  function ouvrirFormulaire() {
    ouvertureDemandee.current = true;
    setConfirmation(false);
    setFormulaireOuvert(true);
  }

  function fermerFormulaire() {
    fermetureDemandee.current = true;
    setFormulaireOuvert(false);
    setSaisie(SAISIE_VIDE);
    setRefus(null);
  }

  async function soumettre(evenement: FormEvent<HTMLFormElement>) {
    evenement.preventDefault();
    if (envoiEnCours.current) {
      return;
    }
    envoiEnCours.current = true;
    setOccupe(true);
    setRefus(null);
    ++demande.current;
    setHistoriqueEnCours(false);
    const resultat = await ajouterValeur(client, stockage, saisie, periode);
    envoiEnCours.current = false;
    setOccupe(false);
    if (resultat.type === 'deconnecte') {
      finDeSession();
    } else if (resultat.type === 'refuse') {
      setPanne(false);
      setRefus(resultat.message);
    } else if (resultat.type === 'technique') {
      setRefus(null);
      setPanne(true);
    } else {
      appliquer(resultat.etat);
      fermerFormulaire();
      setConfirmation(true);
    }
  }

  async function retirer(valeur: ValeurWatchlist) {
    if (etat === null) {
      return;
    }
    ++demande.current;
    setHistoriqueEnCours(false);
    setConfirmation(false);
    const resultat = await retirerValeur(client, stockage, valeur.id, etat.selection, periode);
    if (resultat.type === 'deconnecte') {
      finDeSession();
    } else if (resultat.type === 'technique') {
      setPanne(true);
    } else {
      appliquer(resultat.etat);
    }
  }

  const chargement = etat === null || historiqueEnCours;
  if (etat === null) {
    return (
      <div className="ecran-bourse" aria-busy="true" data-testid="bourse-contenu">
        <title>{TEXTES.titresOnglet.bourse}</title>
      </div>
    );
  }
  const valeurSelectionnee = etat.valeurs.find((valeur) => valeur.symbole === etat.selection) ?? null;
  const historique = etat.historique;
  const libellePeriode = T.periodes[periode].libelle;

  return (
    <div className="ecran-bourse" aria-busy={chargement ? 'true' : undefined} data-testid="bourse-contenu">
      <title>{TEXTES.titresOnglet.bourse}</title>
      <Carte>
        <div className="ecran-bourse__barre">
          <ListeDeroulante
            id="bourse-valeur"
            name="valeur"
            libelle={T.valeur}
            complement={T.nombreDeValeurs(etat.valeurs.length)}
            valeur={etat.selection ?? ''}
            options={etat.valeurs.map((valeur) => ({ valeur: valeur.symbole, libelle: `${valeur.symbole} — ${valeur.nom}` }))}
            onChange={choisirValeur}
            testId="bourse-valeur"
          />
          <ListeDeroulante
            id="bourse-periode"
            name="periode"
            libelle={T.periode}
            valeur={periode}
            options={OPTIONS_PERIODE}
            onChange={choisirPeriode}
            testId="bourse-periode"
          />
          <BoutonSecondaire ref={boutonOuverture} data-testid="bourse-ajouter" onClick={ouvrirFormulaire}>
            {T.ajouterUneValeur}
          </BoutonSecondaire>
        </div>
      </Carte>
      <p className="ecran-bourse__confirmation" role="status" data-testid="bourse-message-succes">
        {confirmation ? T.valeurAjoutee : ''}
      </p>
      {panne && (
        <AlerteErreur testId="bourse-erreur-technique">
          <p>{TEXTES.erreurTechnique}</p>
        </AlerteErreur>
      )}
      {formulaireOuvert && (
        <Carte>
          <form className="ecran-bourse__formulaire" noValidate onSubmit={soumettre}>
            <h2 className="ecran-bourse__titre-formulaire">{T.formulaire.titre}</h2>
            <ChampTexte
              ref={champSymbole}
              id="bourse-symbole"
              name="symbole"
              type="text"
              libelle={T.formulaire.symbole}
              valeur={saisie.symbole}
              onChange={(symbole) => setSaisie({ ...saisie, symbole })}
              maxLength={LONGUEUR_SYMBOLE}
              placeholder={T.formulaire.symboleExemple}
              aide={T.formulaire.symboleAide}
              testId="bourse-symbole"
            />
            <ChampTexte
              id="bourse-nom"
              name="nom"
              type="text"
              libelle={T.formulaire.nom}
              valeur={saisie.nom}
              onChange={(nom) => setSaisie({ ...saisie, nom })}
              maxLength={LONGUEUR_NOM}
              placeholder={T.formulaire.nomExemple}
              testId="bourse-nom"
            />
            <ListeDeroulante
              id="bourse-marche"
              name="marche"
              libelle={T.formulaire.marche}
              valeur={saisie.marche}
              options={T.marches}
              onChange={(marche) => setSaisie({ ...saisie, marche })}
              testId="bourse-marche"
            />
            <ListeDeroulante
              id="bourse-devise"
              name="devise"
              libelle={T.formulaire.devise}
              valeur={saisie.devise}
              options={OPTIONS_DEVISE}
              onChange={(devise) => setSaisie({ ...saisie, devise })}
              testId="bourse-devise"
            />
            <div className="ecran-bourse__actions">
              <BoutonPrimaire testId="bourse-formulaire-ajouter" occupe={occupe}>
                {T.formulaire.ajouter}
              </BoutonPrimaire>
              <BoutonTexte data-testid="bourse-formulaire-annuler" onClick={fermerFormulaire}>
                {T.formulaire.annuler}
              </BoutonTexte>
            </div>
            {refus !== null && (
              <AlerteErreur ref={alerteRefus} testId="bourse-ajout-erreur">
                <p>{refus}</p>
              </AlerteErreur>
            )}
          </form>
        </Carte>
      )}
      {valeurSelectionnee !== null && historique !== null && (
        <Historique historique={historique} valeur={valeurSelectionnee} periode={periode} libellePeriode={libellePeriode} />
      )}
      <section className="ecran-bourse__section" aria-labelledby="bourse-titre-watchlist">
        <Carte>
          <h2 id="bourse-titre-watchlist" className="ecran-bourse__titre">
            {T.watchlist.titre(etat.valeurs.length)}
          </h2>
          <table className="ecran-bourse__tableau" aria-labelledby="bourse-titre-watchlist">
            <thead>
              <tr>
                <th scope="col">{T.watchlist.symbole}</th>
                <th scope="col">{T.watchlist.nom}</th>
                <th scope="col">{T.watchlist.marche}</th>
                <th scope="col">{T.watchlist.devise}</th>
                <th scope="col" className="ecran-bourse__colonne-action">
                  {T.watchlist.supprimer}
                </th>
              </tr>
            </thead>
            <tbody>
              {etat.valeurs.map((valeur) => (
                <tr key={valeur.id}>
                  <td className="ecran-bourse__symbole">{valeur.symbole}</td>
                  <td>{valeur.nom}</td>
                  <td>{valeur.marche}</td>
                  <td>{valeur.devise}</td>
                  <td className="ecran-bourse__colonne-action">
                    <BoutonTexte
                      data-testid={`bourse-retirer-${valeur.id}`}
                      titre={T.watchlist.infobulle(valeur.nom)}
                      onClick={() => void retirer(valeur)}
                    >
                      {T.watchlist.retirer}
                    </BoutonTexte>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Carte>
      </section>
    </div>
  );
}

interface ProprietesHistorique {
  historique: HistoriqueCours;
  valeur: ValeurWatchlist;
  periode: CodePeriode;
  libellePeriode: string;
}

/** Indicateurs, graphique et dernières séances d'une valeur ; sans point, le message d'absence de données. */
function Historique({ historique, valeur, periode, libellePeriode }: ProprietesHistorique) {
  const indicateurs = calculerIndicateurs(historique.points);
  if (indicateurs === null) {
    return (
      <p className="ecran-bourse__aucune-donnee" role="status" data-testid="bourse-aucune-donnee">
        {T.aucuneDonnee(valeur.symbole, libellePeriode)}
      </p>
    );
  }
  const sens = classeSens(indicateurs.hausse);
  return (
    <>
      <div className="ecran-bourse__indicateurs">
        <div className="ecran-bourse__indicateur ecran-bourse__indicateur--principal">
          <span className="ecran-bourse__libelle">{T.kpi.titre(valeur.nom, valeur.symbole)}</span>
          <span className="ecran-bourse__valeur">{`${indicateurs.cours} ${valeur.devise}`}</span>
        </div>
        <div className="ecran-bourse__indicateur">
          <span className="ecran-bourse__libelle">{T.kpi.variation}</span>
          <span className={`ecran-bourse__valeur ${sens}`}>
            {`${indicateurs.hausse ? T.kpi.plus : ''}${indicateurs.variation} ${valeur.devise}`}
          </span>
        </div>
        <div className="ecran-bourse__indicateur">
          <span className="ecran-bourse__libelle">{T.kpi.performance}</span>
          <span className={`ecran-bourse__valeur ${sens}`}>
            {`${indicateurs.hausse ? T.kpi.hausse : T.kpi.baisse}${indicateurs.performance}%`}
          </span>
        </div>
        <div className="ecran-bourse__indicateur">
          <span className="ecran-bourse__libelle">{T.kpi.marche}</span>
          <span className="ecran-bourse__valeur ecran-bourse__valeur--texte">{valeur.marche}</span>
        </div>
      </div>
      {historique.graphique !== null && (
        <section className="ecran-bourse__section" aria-labelledby="bourse-titre-graphique">
          <h2 id="bourse-titre-graphique" className="ecran-bourse__titre">
            {T.graphique.titre}
          </h2>
          <Carte image={{ nom: T.graphique.nom(historique.symbole, T.periodes[periode].libelle) }}>
            {/* SVG produit par le service bourse (story 7.3) : coordonnées et dates calculées, aucun texte saisi. */}
            <div className="ecran-bourse__graphique" dangerouslySetInnerHTML={{ __html: historique.graphique }} />
          </Carte>
        </section>
      )}
      <section className="ecran-bourse__section" aria-labelledby="bourse-titre-seances">
        <Carte>
          <h2 id="bourse-titre-seances" className="ecran-bourse__titre">
            {T.seances.titre}
          </h2>
          <table className="ecran-bourse__tableau" aria-labelledby="bourse-titre-seances">
            <thead>
              <tr>
                <th scope="col">{T.seances.date}</th>
                <th scope="col">{T.seances.ouverture}</th>
                <th scope="col">{T.seances.haut}</th>
                <th scope="col">{T.seances.bas}</th>
                <th scope="col">{T.seances.cloture}</th>
                <th scope="col">{T.seances.volume}</th>
                <th scope="col">{T.seances.variationJour}</th>
              </tr>
            </thead>
            <tbody>
              {dernieresSeances(historique.points).map((seance) => (
                <tr key={seance.date} className={classeSens(seance.hausse)}>
                  <td>{seance.date}</td>
                  <td>{seance.ouverture}</td>
                  <td>{seance.haut}</td>
                  <td>{seance.bas}</td>
                  <td>{seance.cloture}</td>
                  <td>{seance.volume}</td>
                  <td>{`${seance.hausse ? `${T.seances.plus} ` : ''}${seance.variationJour}%`}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </Carte>
      </section>
    </>
  );
}

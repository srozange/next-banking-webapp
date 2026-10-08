import { useEffect, useRef, useState, type FormEvent } from 'react';
import { chargerComptes, type ClientVirement, type Compte } from '../../../application/virement';
import {
  agirSurVirementProgramme,
  chargerVirementsProgrammes,
  creerVirementProgramme,
  type ActionVirementProgramme,
  type ClientVirementsProgrammes,
} from '../../../application/gererVirementsProgrammes';
import {
  CHAMP_DU_REFUS,
  dateAffichee,
  FORMULAIRE_VIREMENT_PROGRAMME_VIDE,
  FREQUENCES,
  LONGUEUR_MAX_LIBELLE,
  LONGUEUR_MAX_MOTIF,
  LONGUEUR_MAX_NUMERO,
  type ChampVirementProgramme,
  type FormulaireVirementProgramme,
  type LigneVirementProgramme,
} from '../../../domain/virementsProgrammes';
import { TEXTES } from '../../textes';
import { AlerteErreur } from '../../ui/AlerteErreur';
import { AlerteSucces } from '../../ui/AlerteSucces';
import { BoutonPrimaire } from '../../ui/BoutonPrimaire';
import { BoutonTexte } from '../../ui/BoutonTexte';
import { Carte } from '../../ui/Carte';
import { ChampListe } from '../../ui/ChampListe';
import { ChampTexte } from '../../ui/ChampTexte';
import { Tableau } from '../../ui/Tableau';
import './EcranVirementsProgrammes.css';

const T = TEXTES.virementsProgrammes;
const C = T.colonnes;
const COLONNES = [C.source, C.beneficiaire, C.montant, C.frequence, C.prochain, C.executions, C.etat, C.actions];
const OPTIONS_FREQUENCE = FREQUENCES.map((frequence) => ({ valeur: frequence, libelle: T.frequences[frequence] }));

interface Proprietes {
  /** Comptes du client (liste déroulante du compte débiteur). */
  comptes: ClientVirement;
  client: ClientVirementsProgrammes;
}

/**
 * Écran /virements-programmes (FEAT-004) : formulaire de création au-dessus de la liste des virements programmés du
 * client du jeton, triés par le service. Les contrôles de saisie locaux suivent l'ordre du legacy ; le service
 * contrôle aussi la saisie, les comptes source et destinataire. Une erreur reste sur l'écran, le champ fautif en
 * `aria-invalid`, valeurs conservées ; un succès vide le formulaire et recharge la liste sans rechargement de page.
 * Suspendre, réactiver et supprimer agissent sans modale puis rechargent la liste ; un 404 affiche le refus générique
 * d'appartenance, un échec technique le texte dédié ; les boutons sont désactivés le temps de l'action.
 */
export function EcranVirementsProgrammes({ comptes, client }: Proprietes) {
  const [lesComptes, setLesComptes] = useState<readonly Compte[]>([]);
  const [lignes, setLignes] = useState<readonly LigneVirementProgramme[]>([]);
  const [formulaire, setFormulaire] = useState<FormulaireVirementProgramme>(FORMULAIRE_VIREMENT_PROGRAMME_VIDE);
  const [erreur, setErreur] = useState<string | null>(null);
  const [champEnErreur, setChampEnErreur] = useState<ChampVirementProgramme | null>(null);
  const [succes, setSucces] = useState(false);
  const [occupe, setOccupe] = useState(false);
  const [actionEnCours, setActionEnCours] = useState(false);
  const derniereDemande = useRef(0);
  const actionEnvoyee = useRef(false);
  const envoiEnCours = useRef(false);
  const alerte = useRef<HTMLDivElement>(null);
  const champs = {
    compteSourceId: useRef<HTMLSelectElement>(null),
    montant: useRef<HTMLInputElement>(null),
    frequence: useRef<HTMLSelectElement>(null),
    numeroBeneficiaire: useRef<HTMLInputElement>(null),
    libelleBeneficiaire: useRef<HTMLInputElement>(null),
    datePremiereExecution: useRef<HTMLInputElement>(null),
    motif: useRef<HTMLInputElement>(null),
  };

  function saisir(champ: ChampVirementProgramme, valeur: string) {
    setFormulaire((precedent) => ({ ...precedent, [champ]: valeur }));
  }

  useEffect(() => {
    let actif = true;
    void chargerComptes(comptes).then((lus) => {
      if (!actif) {
        return;
      }
      if (lus === null) {
        setErreur(TEXTES.erreurTechnique);
      } else {
        setLesComptes(lus);
      }
    });
    return () => {
      actif = false;
    };
  }, [comptes]);

  useEffect(() => {
    const demande = ++derniereDemande.current;
    void chargerVirementsProgrammes(client).then((lus) => {
      if (demande !== derniereDemande.current) {
        return;
      }
      if (lus === null) {
        setErreur(TEXTES.erreurTechnique);
      } else {
        setLignes(lus);
      }
    });
    return () => {
      derniereDemande.current++;
    };
  }, [client]);

  async function creer(evenement: FormEvent<HTMLFormElement>) {
    evenement.preventDefault();
    if (envoiEnCours.current) {
      return;
    }
    envoiEnCours.current = true;
    setOccupe(true);
    const demande = ++derniereDemande.current;
    const resultat = await creerVirementProgramme(formulaire, client);
    envoiEnCours.current = false;
    setOccupe(false);
    if (demande !== derniereDemande.current) {
      return;
    }
    setSucces(false);
    if (resultat.statut === 'cree') {
      setFormulaire(FORMULAIRE_VIREMENT_PROGRAMME_VIDE);
      setChampEnErreur(null);
      setSucces(true);
      setErreur(resultat.liste === null ? TEXTES.erreurTechnique : null);
      if (resultat.liste !== null) {
        setLignes(resultat.liste);
      }
      return;
    }
    if (resultat.statut === 'invalide') {
      setChampEnErreur(CHAMP_DU_REFUS[resultat.refus]);
      setErreur(T.refus[resultat.refus]);
      champs[CHAMP_DU_REFUS[resultat.refus]].current?.focus();
    } else if (resultat.statut === 'refuse') {
      setChampEnErreur(resultat.champ);
      setErreur(resultat.message);
      if (resultat.champ !== null) {
        champs[resultat.champ].current?.focus();
      }
    } else {
      setChampEnErreur(null);
      setErreur(T.erreurCreation);
      alerte.current?.focus();
    }
  }

  async function agir(action: ActionVirementProgramme, id: number) {
    if (actionEnvoyee.current) {
      return;
    }
    actionEnvoyee.current = true;
    setActionEnCours(true);
    const demande = ++derniereDemande.current;
    const resultat = await agirSurVirementProgramme(action, id, client);
    actionEnvoyee.current = false;
    setActionEnCours(false);
    if (demande !== derniereDemande.current) {
      return;
    }
    setSucces(false);
    setChampEnErreur(null);
    if (resultat.statut === 'ok') {
      setErreur(resultat.liste === null ? TEXTES.erreurTechnique : null);
      if (resultat.liste !== null) {
        setLignes(resultat.liste);
      }
      return;
    }
    setErreur(resultat.statut === 'introuvable' ? T.erreurAppartenance : action === 'supprimer' ? T.erreurSuppression : T.erreurEtat);
    alerte.current?.focus();
  }

  const optionsComptes = lesComptes.map((compte) => ({ valeur: String(compte.id), libelle: T.optionCompte(compte.numero, compte.type, compte.solde) }));

  return (
    <section className="ecran-virements-programmes">
      <title>{TEXTES.titresOnglet.virementsProgrammes}</title>
      <h1 className="ecran-virements-programmes__separateur">{T.nouveau}</h1>
      <Carte>
        {succes && (
          <AlerteSucces testId="vp-message-succes">
            <p>{T.succes}</p>
          </AlerteSucces>
        )}
        {erreur !== null && (
          <AlerteErreur ref={alerte} testId="vp-message-erreur">
            <p>{erreur}</p>
          </AlerteErreur>
        )}
        <form className="ecran-virements-programmes__formulaire" noValidate onSubmit={(evenement) => void creer(evenement)}>
          <ChampListe
            ref={champs.compteSourceId}
            id="vp-compte-debiteur"
            name="compteSourceId"
            libelle={T.champs.compteDebiteur}
            libelleVide={T.choisir}
            options={optionsComptes}
            valeur={formulaire.compteSourceId}
            onChange={(valeur) => saisir('compteSourceId', valeur)}
            invalide={champEnErreur === 'compteSourceId'}
            testId="vp-compte-debiteur"
          />
          <ChampTexte
            ref={champs.montant}
            id="vp-montant"
            name="montant"
            type="text"
            libelle={T.champs.montant}
            valeur={formulaire.montant}
            onChange={(valeur) => saisir('montant', valeur)}
            placeholder={T.placeholders.montant}
            inputMode="decimal"
            autoComplete="off"
            invalide={champEnErreur === 'montant'}
            testId="vp-montant"
          />
          <ChampListe
            ref={champs.frequence}
            id="vp-frequence"
            name="frequence"
            libelle={T.champs.frequence}
            options={OPTIONS_FREQUENCE}
            valeur={formulaire.frequence}
            onChange={(valeur) => saisir('frequence', valeur)}
            invalide={champEnErreur === 'frequence'}
            testId="vp-frequence"
          />
          <ChampTexte
            ref={champs.numeroBeneficiaire}
            id="vp-numero-beneficiaire"
            name="numeroBeneficiaire"
            type="text"
            libelle={T.champs.numeroBeneficiaire}
            valeur={formulaire.numeroBeneficiaire}
            onChange={(valeur) => saisir('numeroBeneficiaire', valeur)}
            maxLength={LONGUEUR_MAX_NUMERO}
            placeholder={T.placeholders.numeroBeneficiaire}
            autoComplete="off"
            invalide={champEnErreur === 'numeroBeneficiaire'}
            testId="vp-numero-beneficiaire"
          />
          <ChampTexte
            ref={champs.libelleBeneficiaire}
            id="vp-libelle-beneficiaire"
            name="libelleBeneficiaire"
            type="text"
            libelle={T.champs.libelleBeneficiaire}
            valeur={formulaire.libelleBeneficiaire}
            onChange={(valeur) => saisir('libelleBeneficiaire', valeur)}
            maxLength={LONGUEUR_MAX_LIBELLE}
            placeholder={T.placeholders.libelleBeneficiaire}
            autoComplete="off"
            invalide={champEnErreur === 'libelleBeneficiaire'}
            testId="vp-libelle-beneficiaire"
          />
          <ChampTexte
            ref={champs.datePremiereExecution}
            id="vp-date-premiere-execution"
            name="datePremiereExecution"
            type="text"
            libelle={T.champs.datePremiereExecution}
            valeur={formulaire.datePremiereExecution}
            onChange={(valeur) => saisir('datePremiereExecution', valeur)}
            placeholder={T.placeholders.datePremiereExecution}
            autoComplete="off"
            invalide={champEnErreur === 'datePremiereExecution'}
            testId="vp-date-premiere-execution"
          />
          <ChampTexte
            ref={champs.motif}
            id="vp-motif"
            name="motif"
            type="text"
            libelle={T.champs.motif}
            facultatif
            valeur={formulaire.motif}
            onChange={(valeur) => saisir('motif', valeur)}
            maxLength={LONGUEUR_MAX_MOTIF}
            placeholder={T.placeholders.motif}
            autoComplete="off"
            invalide={champEnErreur === 'motif'}
            testId="vp-motif"
          />
          <BoutonPrimaire testId="vp-creer" occupe={occupe}>
            {T.creer}
          </BoutonPrimaire>
        </form>
      </Carte>
      <h2 className="ecran-virements-programmes__separateur">{T.mes}</h2>
      <Carte>
        <Tableau testId="vp-tableau" colonnes={COLONNES} libelle={T.mes}>
          {lignes.length === 0 ? (
            <tr>
              <td colSpan={COLONNES.length} data-testid="vp-aucun">
                {T.aucun}
              </td>
            </tr>
          ) : (
            lignes.map((ligne) => (
              <tr key={ligne.id} data-testid={`vp-ligne-${ligne.id}`}>
                <td>{ligne.numeroCompteSource}</td>
                <td>
                  <strong className="ecran-virements-programmes__beneficiaire">{ligne.libelleBeneficiaire}</strong>
                  <br />
                  {ligne.numeroBeneficiaire}
                </td>
                <td>{T.montantAffiche(ligne.montant)}</td>
                <td>{ligne.frequence}</td>
                <td>{dateAffichee(ligne.prochaineExecution)}</td>
                <td>{ligne.nbExecutions}</td>
                <td>{ligne.actif ? T.actif : T.inactif}</td>
                <td>
                  <div className="ecran-virements-programmes__actions">
                    {ligne.actif ? (
                      <BoutonTexte data-testid={`vp-suspendre-${ligne.id}`} desactive={actionEnCours} onClick={() => void agir('suspendre', ligne.id)}>{T.suspendre}</BoutonTexte>
                    ) : (
                      <BoutonTexte data-testid={`vp-reactiver-${ligne.id}`} desactive={actionEnCours} onClick={() => void agir('reactiver', ligne.id)}>{T.reactiver}</BoutonTexte>
                    )}
                    <BoutonTexte data-testid={`vp-supprimer-${ligne.id}`} desactive={actionEnCours} onClick={() => void agir('supprimer', ligne.id)}>{T.supprimer}</BoutonTexte>
                  </div>
                </td>
              </tr>
            ))
          )}
        </Tableau>
      </Carte>
    </section>
  );
}

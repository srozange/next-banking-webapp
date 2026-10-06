import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router';
import { ROUTE_TABLEAU_DE_BORD } from '../../../domain/navigation';
import { formaterDate, formaterHeure, type ChampVirement } from '../../../domain/virement';
import { chargerComptes, executerVirement, type ClientVirement, type Compte } from '../../../application/virement';
import { TEXTES } from '../../textes';
import { AlerteAvertissement } from '../../ui/AlerteAvertissement';
import { AlerteErreur } from '../../ui/AlerteErreur';
import { BoutonPrimaire } from '../../ui/BoutonPrimaire';
import { Carte } from '../../ui/Carte';
import { ChampTexte } from '../../ui/ChampTexte';
import { ListeDeroulante } from '../../ui/ListeDeroulante';
import { SectionSuiviVirements } from './SectionSuiviVirements';
import './EcranVirement.css';

const T = TEXTES.virement;
const LONGUEUR_MAX_MOTIF = 100;

interface Proprietes {
  client: ClientVirement;
}

/** Message global affiché ; `numero` change à chaque soumission pour reprendre le focus. */
interface Erreur {
  texte: string;
  numero: number;
}

/** Virement suspendu : l'avis reste sur l'écran, formulaire vidé (FR-027-04). */
interface Suspension {
  heureButoir: string;
  montant: string;
  numeroCompte: string;
}

/**
 * Écran /virement (FEAT-003) : comptes du client, contrôles locaux, soumission sans confirmation, redirection
 * vers le tableau de bord ; toute erreur du service reste sur l'écran, valeurs conservées. Un virement suspendu reste
 * sur l'écran avec son avis, formulaire vidé ; un refus pour erreur du contrôle remplace le formulaire par la vue de
 * refus et sa référence. Le suivi des virements passés en revue est en bas de page.
 */
export function EcranVirement({ client }: Proprietes) {
  const naviguer = useNavigate();
  const [comptes, setComptes] = useState<readonly Compte[]>([]);
  const [compteSource, setCompteSource] = useState('');
  const [compteDestinataire, setCompteDestinataire] = useState('');
  const [montant, setMontant] = useState('');
  const [motif, setMotif] = useState('');
  const [champsEnErreur, setChampsEnErreur] = useState<readonly ChampVirement[]>([]);
  const [erreur, setErreur] = useState<Erreur | null>(null);
  const [occupe, setOccupe] = useState(false);
  const [suspension, setSuspension] = useState<Suspension | null>(null);
  const [referenceRefus, setReferenceRefus] = useState<string | null>(null);
  const [versionSuivi, setVersionSuivi] = useState(0);
  const envoiEnCours = useRef(false);
  const champSource = useRef<HTMLSelectElement>(null);
  const champDestinataire = useRef<HTMLSelectElement>(null);
  const champMontant = useRef<HTMLInputElement>(null);
  const alerte = useRef<HTMLDivElement>(null);
  const avis = useRef<HTMLDivElement>(null);
  const titre = useRef<HTMLHeadingElement>(null);

  useEffect(() => {
    let actif = true;
    void chargerComptes(client).then((lus) => {
      if (!actif) {
        return;
      }
      if (lus === null) {
        setErreur({ texte: TEXTES.erreurTechnique, numero: 1 });
      } else {
        setComptes(lus);
      }
    });
    return () => {
      actif = false;
    };
  }, [client]);

  useEffect(() => {
    if (erreur !== null) {
      alerte.current?.focus();
    }
  }, [erreur]);

  useEffect(() => {
    if (suspension !== null) {
      avis.current?.focus();
    }
  }, [suspension]);

  useEffect(() => {
    if (referenceRefus !== null) {
      titre.current?.focus();
    }
  }, [referenceRefus]);

  const options = comptes.map((compte) => ({ valeur: String(compte.id), libelle: `${compte.type} ${compte.numero}` }));

  function signaler(texte: string) {
    setChampsEnErreur([]);
    setErreur((precedente) => ({ texte, numero: (precedente?.numero ?? 0) + 1 }));
  }

  async function soumettre(evenement: FormEvent<HTMLFormElement>) {
    evenement.preventDefault();
    if (envoiEnCours.current) {
      return;
    }
    envoiEnCours.current = true;
    setOccupe(true);
    setSuspension(null);
    const resultat = await executerVirement({ compteSource, compteDestinataire, montant, motif }, client);
    if (resultat.type === 'execute') {
      naviguer(ROUTE_TABLEAU_DE_BORD);
      return;
    }
    envoiEnCours.current = false;
    setOccupe(false);
    if (resultat.type === 'suspendu') {
      setErreur(null);
      setChampsEnErreur([]);
      setCompteSource('');
      setCompteDestinataire('');
      setMontant('');
      setMotif('');
      setSuspension({
        heureButoir: resultat.heureButoir,
        montant: resultat.montant,
        numeroCompte: comptes.find((compte) => compte.id === resultat.compteDestinataireId)?.numero ?? String(resultat.compteDestinataireId),
      });
      setVersionSuivi((precedente) => precedente + 1);
    } else if (resultat.type === 'refuse') {
      setErreur(null);
      setChampsEnErreur([]);
      setReferenceRefus(resultat.reference);
      setVersionSuivi((precedente) => precedente + 1);
    } else if (resultat.type === 'champs') {
      setErreur(null);
      setChampsEnErreur(resultat.champs);
      const premier = resultat.champs[0];
      const cible = premier === 'compteSource' ? champSource : premier === 'compteDestinataire' ? champDestinataire : champMontant;
      cible.current?.focus();
    } else if (resultat.type === 'identiques') {
      signaler(T.comptesIdentiques);
    } else {
      signaler(resultat.message ?? TEXTES.erreurTechnique);
    }
  }

  const butoir =
    suspension === null ? '' : T.suivi.butoir(formaterDate(suspension.heureButoir), formaterHeure(suspension.heureButoir));

  return (
    <div className="ecran-virement">
      <title>{T.titreOnglet}</title>
      <div className="ecran-virement__cadre">
        <Carte>
          <h1 ref={titre} tabIndex={-1} className="ecran-virement__titre">
            {T.titre}
          </h1>
          {referenceRefus !== null ? (
            <AlerteAvertissement testId="virement-avis-refus" role="alert">
              <p>
                {T.refusErreurControle.avant}
                <span className="ecran-virement__reference" data-testid="virement-refus-reference">
                  {referenceRefus}
                </span>
                {T.refusErreurControle.apres}
              </p>
            </AlerteAvertissement>
          ) : (
            <>
              <form className="ecran-virement__formulaire" noValidate onSubmit={soumettre}>
                {suspension !== null && (
                  <AlerteAvertissement ref={avis} testId="virement-avis-suspension" role="status">
                    <p>
                      {T.avisSuspension.avant(suspension.montant, suspension.numeroCompte)}
                      <span data-testid="virement-avis-heure-butoir">{butoir}</span>
                      {T.avisSuspension.apres}
                    </p>
                  </AlerteAvertissement>
                )}
                <ListeDeroulante
                  ref={champSource}
                  id="virement-compte-source"
                  name="compteSource"
                  libelle={T.compteSource}
                  libelleVide={T.choisirUnCompte}
                  options={options}
                  valeur={compteSource}
                  onChange={setCompteSource}
                  erreur={champsEnErreur.includes('compteSource') ? T.validationComptes : undefined}
                  testId="virement-compte-source"
                />
                <ListeDeroulante
                  ref={champDestinataire}
                  id="virement-compte-destinataire"
                  name="compteDestinataire"
                  libelle={T.compteDestinataire}
                  libelleVide={T.choisirUnCompte}
                  options={options}
                  valeur={compteDestinataire}
                  onChange={setCompteDestinataire}
                  erreur={champsEnErreur.includes('compteDestinataire') ? T.validationComptes : undefined}
                  testId="virement-compte-destinataire"
                />
                <ChampTexte
                  ref={champMontant}
                  id="virement-montant"
                  name="montant"
                  type="text"
                  inputMode="decimal"
                  libelle={T.montant}
                  indice={T.indiceMontant}
                  valeur={montant}
                  onChange={setMontant}
                  autoComplete="off"
                  erreur={champsEnErreur.includes('montant') ? T.validationMontant : undefined}
                  testId="virement-montant"
                />
                <ChampTexte
                  id="virement-motif"
                  name="motif"
                  type="text"
                  libelle={T.motif}
                  facultatif
                  maxLength={LONGUEUR_MAX_MOTIF}
                  valeur={motif}
                  onChange={setMotif}
                  autoComplete="off"
                  testId="virement-motif"
                />
                <BoutonPrimaire testId="virement-executer" occupe={occupe}>
                  {T.executer}
                </BoutonPrimaire>
                {erreur !== null && (
                  <AlerteErreur ref={alerte} testId="virement-message-erreur">
                    <p>{erreur.texte}</p>
                  </AlerteErreur>
                )}
              </form>
              <p>
                <Link className="ecran-virement__lien" to="/beneficiaires" data-testid="virement-lien-beneficiaires">
                  {T.lienBeneficiaires}
                </Link>
              </p>
            </>
          )}
        </Carte>
        <SectionSuiviVirements client={client} testId="virement-virements-revue" version={versionSuivi} />
      </div>
    </div>
  );
}

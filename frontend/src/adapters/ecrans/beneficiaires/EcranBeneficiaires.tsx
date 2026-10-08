import { useEffect, useRef, useState, type FormEvent } from 'react';
import {
  ajouterBeneficiaire,
  chargerBeneficiaires,
  supprimerBeneficiaire,
  verifierIban,
  type ClientBeneficiaires,
  type ResultatVerificationIban,
} from '../../../application/gererBeneficiaires';
import {
  FORMULAIRE_BENEFICIAIRE_VIDE,
  LONGUEUR_MAX_BENEFICIAIRE,
  type ChampBeneficiaire,
  type FormulaireBeneficiaire,
  type LigneBeneficiaire,
} from '../../../domain/beneficiaires';
import { TEXTES } from '../../textes';
import { AlerteErreur } from '../../ui/AlerteErreur';
import { AlerteSucces } from '../../ui/AlerteSucces';
import { BoutonPrimaire } from '../../ui/BoutonPrimaire';
import { BoutonSecondaire } from '../../ui/BoutonSecondaire';
import { BoutonTexte } from '../../ui/BoutonTexte';
import { Carte } from '../../ui/Carte';
import { ChampTexte } from '../../ui/ChampTexte';
import { Tableau } from '../../ui/Tableau';
import './EcranBeneficiaires.css';

const T = TEXTES.beneficiaires;
const C = T.colonnes;
const COLONNES = [C.libelle, C.titulaire, C.iban, C.bic, C.banque, C.zoneSepa, C.action];

interface Proprietes {
  client: ClientBeneficiaires;
}

/** Texte de la vérification d'un IBAN, composé ici à partir du résultat structuré du service (aucun calcul MOD-97). */
function texteVerification(resultat: ResultatVerificationIban): string {
  switch (resultat.statut) {
    case 'vide':
      return T.verification.vide;
    case 'invalide':
      return T.verification.invalide;
    case 'valide':
      return T.verification.valide(resultat.codePays, resultat.zoneSepa);
    case 'technique':
      return TEXTES.erreurTechnique;
  }
}

/**
 * Écran /beneficiaires (FEAT-013) : formulaire d'ajout au-dessus de la liste des bénéficiaires externes actifs du
 * client du jeton, IBAN masqué par le service, « Supprimer » par ligne sans confirmation ; la liste est relue sans
 * rechargement de page. Le service valide et normalise la saisie ; un refus s'affiche en alerte, le champ fautif en
 * `aria-invalid`, valeurs conservées. Les en-têtes restent affichés au-dessus de la ligne « vide ». Toute erreur reste
 * sur l'écran, sans détail technique.
 */
export function EcranBeneficiaires({ client }: Proprietes) {
  const [lignes, setLignes] = useState<readonly LigneBeneficiaire[]>([]);
  const [erreur, setErreur] = useState<string | null>(null);
  const [formulaire, setFormulaire] = useState<FormulaireBeneficiaire>(FORMULAIRE_BENEFICIAIRE_VIDE);
  const [champEnErreur, setChampEnErreur] = useState<ChampBeneficiaire | null>(null);
  const [verification, setVerification] = useState<string | null>(null);
  const [verificationReussie, setVerificationReussie] = useState(false);
  const [succes, setSucces] = useState(false);
  const [occupe, setOccupe] = useState(false);
  const derniereDemande = useRef(0);
  const envoiEnCours = useRef(false);
  const champs = {
    libelle: useRef<HTMLInputElement>(null),
    titulaire: useRef<HTMLInputElement>(null),
    iban: useRef<HTMLInputElement>(null),
    bic: useRef<HTMLInputElement>(null),
    banque: useRef<HTMLInputElement>(null),
  };

  function saisir(champ: ChampBeneficiaire, valeur: string) {
    setFormulaire((precedent) => ({ ...precedent, [champ]: valeur }));
  }

  async function verifier() {
    const resultat = await verifierIban(formulaire.iban, client);
    setVerificationReussie(resultat.statut === 'valide');
    setVerification(texteVerification(resultat));
  }

  async function ajouter(evenement: FormEvent<HTMLFormElement>) {
    evenement.preventDefault();
    if (envoiEnCours.current) {
      return;
    }
    envoiEnCours.current = true;
    setOccupe(true);
    const demande = ++derniereDemande.current;
    const resultat = await ajouterBeneficiaire(formulaire, client);
    envoiEnCours.current = false;
    setOccupe(false);
    if (demande !== derniereDemande.current) {
      return;
    }
    setSucces(false);
    if (resultat.statut === 'ajoute') {
      setFormulaire(FORMULAIRE_BENEFICIAIRE_VIDE);
      setChampEnErreur(null);
      setVerification(null);
      setSucces(true);
      setErreur(resultat.liste === null ? TEXTES.erreurTechnique : null);
      if (resultat.liste !== null) {
        setLignes(resultat.liste);
      }
    } else if (resultat.statut === 'refuse') {
      setChampEnErreur(resultat.champ);
      setErreur(resultat.message);
      if (resultat.champ !== null) {
        champs[resultat.champ].current?.focus();
      }
    } else {
      setChampEnErreur(null);
      setErreur(T.erreurAjout);
    }
  }

  useEffect(() => {
    const demande = ++derniereDemande.current;
    void chargerBeneficiaires(client).then((lues) => {
      if (demande !== derniereDemande.current) {
        return;
      }
      if (lues === null) {
        setErreur(TEXTES.erreurTechnique);
      } else {
        setLignes(lues);
      }
    });
    return () => {
      derniereDemande.current++;
    };
  }, [client]);

  async function supprimer(beneficiaireId: number) {
    const demande = ++derniereDemande.current;
    const resultat = await supprimerBeneficiaire(beneficiaireId, client);
    if (demande !== derniereDemande.current) {
      return;
    }
    if (resultat.statut === 'technique') {
      setErreur(TEXTES.erreurTechnique);
      return;
    }
    setChampEnErreur(null);
    setSucces(false);
    setErreur(resultat.statut === 'refuse' ? T.erreurSuppression : resultat.liste === null ? TEXTES.erreurTechnique : null);
    if (resultat.liste !== null) {
      setLignes(resultat.liste);
    }
  }

  return (
    <section className="ecran-beneficiaires">
      <title>{TEXTES.titresOnglet.beneficiaires}</title>
      <h1 className="ecran-beneficiaires__separateur">{T.ajouter}</h1>
      <Carte>
        <h2 className="ecran-beneficiaires__titre">{T.nouveau}</h2>
        {succes && (
          <AlerteSucces testId="beneficiaires-message-info">
            <p>{T.succes}</p>
          </AlerteSucces>
        )}
        {erreur !== null && (
          <AlerteErreur testId="beneficiaires-message-erreur">
            <p>{erreur}</p>
          </AlerteErreur>
        )}
        <form className="ecran-beneficiaires__formulaire" noValidate onSubmit={(evenement) => void ajouter(evenement)}>
          <ChampTexte
            ref={champs.libelle}
            id="beneficiaires-libelle"
            name="libelle"
            type="text"
            libelle={T.champs.libelle}
            valeur={formulaire.libelle}
            onChange={(valeur) => saisir('libelle', valeur)}
            maxLength={LONGUEUR_MAX_BENEFICIAIRE}
            autoComplete="off"
            invalide={champEnErreur === 'libelle'}
            testId="beneficiaires-libelle"
          />
          <ChampTexte
            ref={champs.titulaire}
            id="beneficiaires-titulaire"
            name="titulaire"
            type="text"
            libelle={T.champs.titulaire}
            valeur={formulaire.titulaire}
            onChange={(valeur) => saisir('titulaire', valeur)}
            maxLength={LONGUEUR_MAX_BENEFICIAIRE}
            autoComplete="off"
            invalide={champEnErreur === 'titulaire'}
            testId="beneficiaires-titulaire"
          />
          <div className="ecran-beneficiaires__iban">
            <ChampTexte
              ref={champs.iban}
              id="beneficiaires-iban"
              name="iban"
              type="text"
              libelle={T.champs.iban}
              valeur={formulaire.iban}
              onChange={(valeur) => saisir('iban', valeur)}
              placeholder={T.placeholders.iban}
              autoComplete="off"
              invalide={champEnErreur === 'iban'}
              testId="beneficiaires-iban"
            />
            <BoutonSecondaire data-testid="beneficiaires-verifier-iban" onClick={() => void verifier()}>
              {T.verifierIban}
            </BoutonSecondaire>
          </div>
          <p
            className={verificationReussie ? 'ecran-beneficiaires__verification ecran-beneficiaires__verification--valide' : 'ecran-beneficiaires__verification ecran-beneficiaires__verification--refus'}
            aria-live="polite"
            data-testid="beneficiaires-resultat-iban"
          >
            {verification}
          </p>
          <ChampTexte
            ref={champs.bic}
            id="beneficiaires-bic"
            name="bic"
            type="text"
            libelle={T.champs.bic}
            facultatif
            valeur={formulaire.bic}
            onChange={(valeur) => saisir('bic', valeur)}
            placeholder={T.placeholders.bic}
            autoComplete="off"
            invalide={champEnErreur === 'bic'}
            testId="beneficiaires-bic"
          />
          <ChampTexte
            ref={champs.banque}
            id="beneficiaires-banque"
            name="banque"
            type="text"
            libelle={T.champs.banque}
            facultatif
            valeur={formulaire.banque}
            onChange={(valeur) => saisir('banque', valeur)}
            maxLength={LONGUEUR_MAX_BENEFICIAIRE}
            placeholder={T.placeholders.banque}
            autoComplete="off"
            invalide={champEnErreur === 'banque'}
            testId="beneficiaires-banque"
          />
          <BoutonPrimaire testId="beneficiaires-ajouter" occupe={occupe}>
            {T.ajouterBeneficiaire}
          </BoutonPrimaire>
        </form>
      </Carte>
      <h2 className="ecran-beneficiaires__separateur">{T.separateur}</h2>
      <Carte>
        <h3 className="ecran-beneficiaires__titre" data-testid="beneficiaires-titre">
          {T.titre(lignes.length)}
        </h3>
        <Tableau testId="beneficiaires-tableau" colonnes={COLONNES} libelle={T.separateur}>
          {lignes.length === 0 ? (
            <tr>
              <td colSpan={COLONNES.length} data-testid="beneficiaires-aucun">
                {T.aucun}
              </td>
            </tr>
          ) : (
            lignes.map((ligne) => (
              <tr key={ligne.id} data-testid={`beneficiaires-ligne-${ligne.id}`}>
                <td>{ligne.libelle}</td>
                <td>{ligne.titulaire}</td>
                <td data-testid={`beneficiaires-iban-${ligne.id}`}>{ligne.iban}</td>
                <td>{ligne.bic ?? T.absent}</td>
                <td>{ligne.banque ?? T.absent}</td>
                <td data-testid={`beneficiaires-zone-${ligne.id}`}>{ligne.zoneSepa ? T.sepa : T.horsSepa}</td>
                <td>
                  <BoutonTexte data-testid={`beneficiaires-supprimer-${ligne.id}`} onClick={() => void supprimer(ligne.id)}>
                    {T.supprimer}
                  </BoutonTexte>
                </td>
              </tr>
            ))
          )}
        </Tableau>
      </Carte>
    </section>
  );
}

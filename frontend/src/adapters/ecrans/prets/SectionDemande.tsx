import { useId, useState, type FormEvent } from 'react';
import { TYPE_PRET_DEFAUT, TYPES_PRET, type SaisieDemande } from '../../../domain/pret';
import { deposerDemande, type ClientPrets } from '../../../application/prets';
import type { SessionLocale } from '../../../application/session';
import { TEXTES } from '../../textes';
import { AlerteErreur } from '../../ui/AlerteErreur';
import { AlerteInfo } from '../../ui/AlerteInfo';
import { BoutonPrimaire } from '../../ui/BoutonPrimaire';
import { Carte } from '../../ui/Carte';
import { ChampListe } from '../../ui/ChampListe';
import { ChampSaisie } from '../../ui/ChampSaisie';
import './SectionDemande.css';

const T = TEXTES.prets.demande;

interface Proprietes {
  client: ClientPrets;
  session: SessionLocale;
  /** Session perdue pendant l'envoi. */
  surDeconnexion: () => void;
  /** Demande transmise : la liste des emprunts doit être relue. */
  surDepot: () => void;
}

/**
 * Section « Demande de pret » (FR-007-04, FR-007-05, FR-007-07, FR-024-02) : saisie envoyée telle quelle au service
 * credits. Succès : message d'information et liste relue ; refus (400, 503) : message du service ; autre panne : texte
 * fixe. Les valeurs saisies sont conservées dans tous les cas.
 */
export function SectionDemande({ client, session, surDeconnexion, surDepot }: Proprietes) {
  const [type, setType] = useState<string>(TYPE_PRET_DEFAUT);
  const [montant, setMontant] = useState('');
  const [taux, setTaux] = useState('3.50');
  const [duree, setDuree] = useState('36');
  const [objet, setObjet] = useState('');
  const [occupe, setOccupe] = useState(false);
  const [info, setInfo] = useState<string | null>(null);
  const [erreur, setErreur] = useState<string | null>(null);
  const idTitre = useId();

  async function soumettre(evenement: FormEvent<HTMLFormElement>) {
    evenement.preventDefault();
    const saisie: SaisieDemande = { type, montant, tauxAnnuel: taux, dureeMois: duree, objet };
    setOccupe(true);
    const resultat = await deposerDemande(client, session, saisie);
    if (resultat.type === 'deconnecte') {
      surDeconnexion();
      return;
    }
    setOccupe(false);
    if (resultat.type === 'deposee') {
      setErreur(null);
      setInfo(T.transmise);
      surDepot();
      return;
    }
    setInfo(null);
    setErreur(resultat.type === 'refus' ? resultat.message : TEXTES.prets.erreurDemande);
  }

  return (
    <section className="section-demande" aria-labelledby={idTitre}>
      <h2 id={idTitre} className="section-demande__separateur">
        {TEXTES.prets.separateurs.demande}
      </h2>
      <Carte>
        <h3 className="section-demande__titre">{T.titre}</h3>
        <form className="section-demande__formulaire" noValidate onSubmit={soumettre}>
          <ChampListe
            id="pret-type"
            name="typePret"
            libelle={T.type}
            valeur={type}
            onChange={setType}
            options={TYPES_PRET}
            testId="pret-type"
          />
          <ChampSaisie
            id="pret-montant"
            name="montant"
            libelle={T.montant}
            valeur={montant}
            onChange={setMontant}
            modeSaisie="decimal"
            testId="pret-montant"
          />
          <ChampSaisie
            id="pret-taux"
            name="taux"
            libelle={T.taux}
            valeur={taux}
            onChange={setTaux}
            modeSaisie="decimal"
            testId="pret-taux"
          />
          <ChampSaisie
            id="pret-duree"
            name="duree"
            libelle={T.duree}
            valeur={duree}
            onChange={setDuree}
            modeSaisie="numeric"
            testId="pret-duree"
          />
          <ChampSaisie id="pret-objet" name="objetPret" libelle={T.objet} valeur={objet} onChange={setObjet} testId="pret-objet" />
          <BoutonPrimaire testId="pret-soumettre" occupe={occupe}>
            {T.soumettre}
          </BoutonPrimaire>
        </form>
        {info !== null && (
          <AlerteInfo testId="pret-message-info">
            <p>{info}</p>
          </AlerteInfo>
        )}
        {erreur !== null && (
          <AlerteErreur testId="pret-message-erreur">
            <p>{erreur}</p>
          </AlerteErreur>
        )}
      </Carte>
    </section>
  );
}

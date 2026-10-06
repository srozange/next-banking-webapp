import { useRef, useState, type FormEvent } from 'react';
import { deciderVirement, type ActionRevue, type ClientRevue, type ResultatDecision } from '../../../application/revue';
import type { VirementEnRevue } from '../../../domain/virement';
import { TEXTES } from '../../textes';
import { BoutonPrimaire } from '../../ui/BoutonPrimaire';
import { BoutonTexte } from '../../ui/BoutonTexte';
import { ChampTexte } from '../../ui/ChampTexte';
import { Modale } from '../../ui/Modale';
import './ModaleDecision.css';

const T = TEXTES.revue;

interface Proprietes {
  action: ActionRevue;
  virement: VirementEnRevue;
  client: ClientRevue;
  /** Annuler ou Échap. */
  surFermeture: () => void;
  /** Résultat du service, hors motif manquant (affiché dans la modale). */
  surResultat: (resultat: ResultatDecision) => Promise<void>;
}

/**
 * Modale de décision : titre « Libérer/Rejeter le virement de {montant} EUR vers le compte {numCompte} », champ
 * « Motif de la décision * » (`revue-motif`) obligatoire, Valider (`revue-valider`) et Annuler. Un motif vide ou d'espaces
 * reste dans la modale, champ en `aria-invalid`, sans appel ; un refus de motif du serveur s'y affiche de même.
 */
export function ModaleDecision({ action, virement, client, surFermeture, surResultat }: Proprietes) {
  const [motif, setMotif] = useState('');
  const [motifManquant, setMotifManquant] = useState(false);
  const [occupe, setOccupe] = useState(false);
  const envoiEnCours = useRef(false);
  const champ = useRef<HTMLInputElement>(null);
  const titre = (action === 'liberer' ? T.modale.titreLiberer : T.modale.titreRejeter)(virement.montant, virement.compteDestinataire);

  async function valider(evenement: FormEvent<HTMLFormElement>) {
    evenement.preventDefault();
    if (envoiEnCours.current) {
      return;
    }
    envoiEnCours.current = true;
    setOccupe(true);
    const resultat = await deciderVirement(action, virement.virementId, motif, client);
    envoiEnCours.current = false;
    setOccupe(false);
    if (resultat.type === 'motif') {
      setMotifManquant(true);
      champ.current?.focus();
      return;
    }
    await surResultat(resultat);
  }

  return (
    <Modale idTitre="revue-modale-titre" titre={titre} testId="revue-modale" surFermeture={surFermeture}>
      <form className="modale-decision" noValidate onSubmit={valider}>
        <ChampTexte
          ref={champ}
          id="revue-motif"
          name="motif"
          type="text"
          libelle={T.modale.champMotif}
          valeur={motif}
          onChange={setMotif}
          autoComplete="off"
          erreur={motifManquant ? T.erreurMotifManquant : undefined}
          testId="revue-motif"
        />
        <div className="modale-decision__boutons">
          <BoutonPrimaire testId="revue-valider" occupe={occupe}>
            {T.modale.valider}
          </BoutonPrimaire>
          <BoutonTexte data-testid="revue-annuler" onClick={surFermeture}>
            {T.modale.annuler}
          </BoutonTexte>
        </div>
      </form>
    </Modale>
  );
}

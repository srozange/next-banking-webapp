import { useId, useState } from 'react';
import { TYPE_PRET_DEFAUT, TYPES_PRET } from '../../../domain/pret';
import { TEXTES } from '../../textes';
import { BoutonPrimaire } from '../../ui/BoutonPrimaire';
import { Carte } from '../../ui/Carte';
import { ChampListe } from '../../ui/ChampListe';
import { ChampSaisie } from '../../ui/ChampSaisie';
import './SectionDemande.css';

const T = TEXTES.prets.demande;

/**
 * Section « Demande de pret » (FR-007-04) : formulaire complet mais inerte. Le bouton n'envoie rien : la soumission
 * est livrée par la story 5.2.
 */
export function SectionDemande() {
  const [type, setType] = useState<string>(TYPE_PRET_DEFAUT);
  const [montant, setMontant] = useState('');
  const [taux, setTaux] = useState('3.50');
  const [duree, setDuree] = useState('36');
  const [objet, setObjet] = useState('');
  const idTitre = useId();

  return (
    <section className="section-demande" aria-labelledby={idTitre}>
      <h2 id={idTitre} className="section-demande__separateur">
        {TEXTES.prets.separateurs.demande}
      </h2>
      <Carte>
        <h3 className="section-demande__titre">{T.titre}</h3>
        <form className="section-demande__formulaire" noValidate>
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
          <BoutonPrimaire testId="pret-soumettre" type="button">
            {T.soumettre}
          </BoutonPrimaire>
        </form>
      </Carte>
    </section>
  );
}

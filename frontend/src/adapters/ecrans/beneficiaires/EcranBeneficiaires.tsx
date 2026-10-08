import { useEffect, useRef, useState } from 'react';
import { chargerBeneficiaires, supprimerBeneficiaire, type ClientBeneficiaires } from '../../../application/gererBeneficiaires';
import type { LigneBeneficiaire } from '../../../domain/beneficiaires';
import { TEXTES } from '../../textes';
import { AlerteErreur } from '../../ui/AlerteErreur';
import { BoutonTexte } from '../../ui/BoutonTexte';
import { Carte } from '../../ui/Carte';
import { Tableau } from '../../ui/Tableau';
import './EcranBeneficiaires.css';

const T = TEXTES.beneficiaires;
const C = T.colonnes;
const COLONNES = [C.libelle, C.titulaire, C.iban, C.bic, C.banque, C.zoneSepa, C.action];

interface Proprietes {
  client: ClientBeneficiaires;
}

/**
 * Écran /beneficiaires (FEAT-013) : bénéficiaires externes actifs du client du jeton, IBAN masqué par le service,
 * « Supprimer » par ligne sans confirmation ; la liste est relue sans rechargement de page. Les en-têtes restent
 * affichés au-dessus de la ligne « vide ». Toute erreur reste sur l'écran, sans détail technique.
 */
export function EcranBeneficiaires({ client }: Proprietes) {
  const [lignes, setLignes] = useState<readonly LigneBeneficiaire[]>([]);
  const [erreur, setErreur] = useState<string | null>(null);
  const derniereDemande = useRef(0);

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
    setErreur(resultat.statut === 'refuse' ? T.erreurSuppression : resultat.liste === null ? TEXTES.erreurTechnique : null);
    if (resultat.liste !== null) {
      setLignes(resultat.liste);
    }
  }

  return (
    <section className="ecran-beneficiaires">
      <title>{TEXTES.titresOnglet.beneficiaires}</title>
      <h1 className="ecran-beneficiaires__separateur">{T.separateur}</h1>
      <Carte>
        <h2 className="ecran-beneficiaires__titre" data-testid="beneficiaires-titre">
          {T.titre(lignes.length)}
        </h2>
        {erreur !== null && (
          <AlerteErreur testId="beneficiaires-message-erreur">
            <p>{erreur}</p>
          </AlerteErreur>
        )}
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

import { useEffect, useState, type MouseEvent } from 'react';
import { chargerDocuments, telechargerDocument, type ClientDocuments, type PageDocuments } from '../../../application/consulterDocuments';
import { testIdLien, type DemandeDocument } from '../../../domain/documents';
import { TEXTES } from '../../textes';
import { AlerteErreur } from '../../ui/AlerteErreur';
import { Carte } from '../../ui/Carte';
import { Etiquette } from '../../ui/Etiquette';
import './EcranDocuments.css';

const T = TEXTES.documents;

/** Délai avant de libérer l'URL d'objet : le nouvel onglet a eu le temps de charger le PDF. */
const DELAI_LIBERATION_MS = 60_000;

/** Ouvre le PDF reçu dans un nouvel onglet, puis libère l'URL d'objet. */
export function ouvrirPdfDansUnNouvelOnglet(pdf: Blob): void {
  const adresse = URL.createObjectURL(pdf);
  window.open(adresse, '_blank');
  setTimeout(() => URL.revokeObjectURL(adresse), DELAI_LIBERATION_MS);
}

interface Proprietes {
  client: ClientDocuments;
  /** Ouvre le PDF reçu ; substituable en test (par défaut : nouvel onglet du navigateur). */
  ouvrirPdf?: (pdf: Blob) => void;
}

interface LienProprietes {
  demande: DemandeDocument;
  libelle: string;
  telecharger: (demande: DemandeDocument) => void;
}

/** Lien de téléchargement : nom accessible suivi de « (nouvel onglet) », réservé aux lecteurs d'écran. */
function LienDocument({ demande, libelle, telecharger }: LienProprietes) {
  function activer(evenement: MouseEvent<HTMLAnchorElement>) {
    evenement.preventDefault();
    telecharger(demande);
  }
  return (
    <a className="ecran-documents__lien" href="#telecharger" data-testid={testIdLien(demande)} onClick={activer}>
      {libelle}{' '}
      <span className="ecran-documents__lecteur-ecran">{TEXTES.a11y.nouvelOnglet}</span>
    </a>
  );
}

interface CarteProprietes {
  idEtiquette: string;
  etiquette: string;
  titre: string;
  sousTitre: string;
  demande: DemandeDocument;
  telecharger: (demande: DemandeDocument) => void;
}

function CarteDocument({ idEtiquette, etiquette, titre, sousTitre, demande, telecharger }: CarteProprietes) {
  return (
    <Carte>
      <Etiquette data-testid={idEtiquette}>{etiquette}</Etiquette>
      <h3 className="ecran-documents__titre-carte">{titre}</h3>
      <p className="ecran-documents__sous-titre">{sousTitre}</p>
      <LienDocument demande={demande} libelle={T.telecharger} telecharger={telecharger} />
    </Carte>
  );
}

/**
 * Écran /documents (FEAT-008), même contenu pour CLIENT, CONSEILLER et ADMIN : documents fiscaux (IFU des deux années
 * précédentes), puis par compte du connecté le RIB, l'attestation de solde et les 12 relevés mensuels. Chaque lien demande
 * le PDF avec le jeton puis l'ouvre dans un nouvel onglet ; toute réponse autre que le PDF affiche le texte technique,
 * sans onglet (le 401 ramène à /login par la règle de fetchAuthentifie).
 */
export function EcranDocuments({ client, ouvrirPdf = ouvrirPdfDansUnNouvelOnglet }: Proprietes) {
  const [page, setPage] = useState<PageDocuments | null | undefined>(undefined);
  const [erreurTelechargement, setErreurTelechargement] = useState(false);

  useEffect(() => {
    let actif = true;
    void chargerDocuments(client).then((lue) => {
      if (actif) {
        setPage(lue);
      }
    });
    return () => {
      actif = false;
    };
  }, [client]);

  function telecharger(demande: DemandeDocument) {
    void telechargerDocument(client, demande).then((pdf) => {
      setErreurTelechargement(pdf === null);
      if (pdf !== null) {
        ouvrirPdf(pdf);
      }
    });
  }

  return (
    <div className="ecran-documents" aria-busy={page === undefined ? 'true' : undefined} data-testid="documents-ecran">
      <title>{TEXTES.titresOnglet.documents}</title>
      {(page === null || erreurTelechargement) && (
        <AlerteErreur testId="documents-message-erreur">
          <p>{TEXTES.erreurTechnique}</p>
        </AlerteErreur>
      )}
      {page && (
        <>
          <section className="ecran-documents__section" data-testid="documents-section-ifu">
            <p className="ecran-documents__eyebrow">{T.documentsFiscaux}</p>
            <h2 className="ecran-documents__titre">{T.titreIfu}</h2>
            <div className="ecran-documents__cartes">
              {page.anneesIfu.map((annee) => (
                <CarteDocument
                  key={annee}
                  idEtiquette={`documents-etiquette-ifu-${annee}`}
                  etiquette={T.etiquetteFiscal}
                  titre={T.ifu(annee)}
                  sousTitre={T.sousTitreIfu}
                  demande={{ type: 'IFU', annee }}
                  telecharger={telecharger}
                />
              ))}
            </div>
          </section>
          {page.comptes.map((compte) => (
            <section key={compte.compteId} className="ecran-documents__section" data-testid={`documents-section-compte-${compte.compteId}`}>
              <p className="ecran-documents__eyebrow">{T.compte(compte.type, compte.numero)}</p>
              <div className="ecran-documents__cartes">
                <CarteDocument
                  idEtiquette={`documents-etiquette-rib-${compte.compteId}`}
                  etiquette={T.etiquetteRib}
                  titre={T.titreRib}
                  sousTitre={T.sousTitreRib}
                  demande={{ type: 'RIB', compteId: compte.compteId }}
                  telecharger={telecharger}
                />
                <CarteDocument
                  idEtiquette={`documents-etiquette-attestation-${compte.compteId}`}
                  etiquette={T.etiquetteAttestation}
                  titre={T.titreAttestation}
                  sousTitre={T.sousTitreAttestation}
                  demande={{ type: 'ATTESTATION', compteId: compte.compteId }}
                  telecharger={telecharger}
                />
              </div>
              <h3 className="ecran-documents__titre-carte">{T.releves(compte.numero)}</h3>
              <ul className="ecran-documents__releves">
                {compte.releves.map((releve) => (
                  <li key={`${releve.annee}-${releve.mois}`}>
                    <LienDocument
                      demande={{ type: 'RELEVE', compteId: compte.compteId, annee: releve.annee, mois: releve.mois }}
                      libelle={releve.libelle}
                      telecharger={telecharger}
                    />
                  </li>
                ))}
              </ul>
            </section>
          ))}
        </>
      )}
    </div>
  );
}

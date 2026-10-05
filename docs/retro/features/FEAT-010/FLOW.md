# FLOW — FEAT-010 Chat avec le conseiller

```mermaid
stateDiagram-v2
    [*] --> Vide
    Vide --> Chargé : GET /banking-legacy/conseiller.xhtml — salutation Sophie chargée
    Chargé --> Erreur : POST chatForm:userInput — envoi message texte libre
    Erreur --> Vide : Nouvelle conversation — réinitialisation du fil
    Chargé --> Vide : Nouvelle conversation — réinitialisation sans messages
    Chargé --> Erreur : POST chatForm:userInput — action rapide 'Je voudrais optimiser mon epargne, que me conseillez-vous ?'
    Vide --> Chargé : GET /banking-legacy/conseiller.xhtml — role CONSEILLER
    Vide --> Chargé : GET /banking-legacy/conseiller.xhtml — role ADMIN
```

## Captures par état

| État | Screenshot | Rôle | Notes |
|---|---|---|---|
| Chargé | SCR-010-01_charge.png | CLIENT | Page chargée avec message de bienvenue personnalisé (prénom du client). Boutons d'actions rapides visibles. Textbox message vide. |
| Erreur | SCR-010-02_erreur.png | CLIENT | AI backend Azure AI Foundry retourne HTTP 401 en environnement local. Message affiché : 'Je suis momentanement indisponible (HTTP 401). Veuillez reessayer.' Textbox non vidée après envoi (dette-legacy). |
| Vide | SCR-010-03_vide.png | CLIENT | Conversation réinitialisée : messages précédents effacés, nouvelle salutation Sophie envoyée. Textbox non vidée (contient encore l'ancien message — dette-legacy). |
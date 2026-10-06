-- État Powens par client (FEAT-028, FEAT-012) : une ligne par `sub` du jeton d'accès, pas de session serveur.
-- Le jeton permanent n'est jamais rendu par l'API ni journalisé.
create table etat_powens (
    sub             varchar(100) primary key,
    jeton_permanent varchar(1000),
    connecte        boolean      not null default false,
    message         varchar(500)
);

-- `state` émis au lancement de la connexion : le nonce est consommé une seule fois (retour, story 8.2).
create table states_powens (
    nonce     varchar(64)  primary key,
    sub       varchar(100) not null,
    expire_a  timestamptz  not null,
    utilise   boolean      not null default false
);

create index states_powens_sub on states_powens (sub);

# Secrets locaux : .env (mots de passe aléatoires des bases, paire RSA du jeton en DER base64) et paire RSA PEM
# du jeton dans .secrets/ (source de la forme DER).
# Idempotent variable par variable : un mot de passe déjà présent dans .env est conservé, un manquant est ajouté.
# Une clé PEM existante est conservée ; seule exception : une clé privée régénérée entraîne sa clé publique, qui en
# dérive. NB_JETON_CLE_PRIVEE et NB_JETON_CLE_PUBLIQUE sont toujours réécrites depuis .secrets/.
.PHONY: secrets

MOTS_DE_PASSE := NB_IDENTITE_CLIENTS_BD_MOT_DE_PASSE NB_COEUR_BANCAIRE_BD_MOT_DE_PASSE NB_NOTIFICATIONS_BD_MOT_DE_PASSE \
                 NB_AUDIT_BD_MOT_DE_PASSE NB_BOURSE_BD_MOT_DE_PASSE \
                 NB_OPEN_BANKING_CONSEILLER_BD_MOT_DE_PASSE

secrets:
	@umask 077; \
	touch .env; \
	if [ -s .env ] && [ -n "$$(tail -c 1 .env)" ]; then echo >> .env; fi; \
	for variable in $(MOTS_DE_PASSE); do \
	  if grep -q "^$$variable=." .env; then \
	    echo "$$variable existe déjà dans .env : conservée"; \
	  else \
	    valeur=$$(openssl rand -hex 24); \
	    if [ -z "$$valeur" ]; then echo "openssl rand a échoué : $$variable non générée" >&2; exit 1; fi; \
	    grep -v "^$$variable=" .env > .env.tmp || true; \
	    echo "$$variable=$$valeur" >> .env.tmp && mv .env.tmp .env \
	    && echo "$$variable générée dans .env"; \
	  fi; \
	done; \
	mkdir -p .secrets; \
	if [ -f .secrets/jeton-prive.pem ]; then \
	  echo "clé privée de .secrets/ existe déjà : conservée"; \
	else \
	  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out .secrets/jeton-prive.pem 2>/dev/null \
	  && rm -f .secrets/jeton-public.pem \
	  && echo "clé privée du jeton générée dans .secrets/"; \
	fi && \
	if [ -f .secrets/jeton-public.pem ]; then \
	  echo "clé publique de .secrets/ existe déjà : conservée"; \
	else \
	  openssl pkey -in .secrets/jeton-prive.pem -pubout -out .secrets/jeton-public.pem \
	  && echo "clé publique du jeton dérivée de la clé privée dans .secrets/"; \
	fi && \
	prive=$$(openssl pkcs8 -topk8 -nocrypt -in .secrets/jeton-prive.pem -outform DER | openssl base64 -A) && \
	public=$$(openssl pkey -pubin -in .secrets/jeton-public.pem -outform DER | openssl base64 -A) && \
	[ -n "$$prive" ] && [ -n "$$public" ] && \
	{ grep -v -e '^NB_JETON_CLE_PRIVEE=' -e '^NB_JETON_CLE_PUBLIQUE=' .env > .env.tmp || true; } && \
	echo "NB_JETON_CLE_PRIVEE=$$prive" >> .env.tmp && \
	echo "NB_JETON_CLE_PUBLIQUE=$$public" >> .env.tmp && \
	mv .env.tmp .env && \
	echo "NB_JETON_CLE_PRIVEE et NB_JETON_CLE_PUBLIQUE réécrites dans .env depuis .secrets/"

# Secrets locaux : .env (mots de passe aléatoires des bases) et paire RSA du jeton dans .secrets/.
# Rien n'est écrasé : un fichier existant est conservé. Seule exception : une clé privée régénérée
# entraîne sa clé publique, qui en dérive.
.PHONY: secrets

secrets:
	@umask 077; \
	if [ -f .env ]; then \
	  echo ".env existe déjà : conservé"; \
	else \
	  { echo "NB_COEUR_BANCAIRE_BD_MOT_DE_PASSE=$$(openssl rand -hex 24)"; \
	    echo "NB_NOTIFICATIONS_BD_MOT_DE_PASSE=$$(openssl rand -hex 24)"; } > .env; \
	  echo ".env généré"; \
	fi; \
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
	fi

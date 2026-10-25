# Installer openRun

openRun n'est pas (encore) sur le Play Store. On l'installe depuis un fichier APK.

## Sur le téléphone

1. Télécharge le fichier `openRun-<version>.apk` de la dernière version (page des releases du projet).
2. Ouvre-le. Android te demande d'autoriser l'installation depuis ce navigateur ou ce gestionnaire de fichiers:
   accepte une fois.
3. Lance openRun. Réponds à « Où en es-tu ? », puis appuie sur Démarrer.
4. Au premier départ, autorise la **position** (« Lorsque vous utilisez l'appli ») et les **notifications**.
   Sans position, tu peux courir sans GPS (tapis).

Pour vérifier que le fichier est bien celui du projet, compare son empreinte SHA-256 avec celle donnée dans les notes
de la release.

## Mises à jour

Installe le nouvel APK par-dessus l'ancien: tes séances et tes réglages sont conservés, à condition qu'il soit signé
avec la même clé (c'est le cas des releases officielles).

## Google Health

Réglages → Santé → Connecter, puis accepte les autorisations. Chaque séance enregistrée apparaît ensuite dans Health
Connect, que Google Health lit.

## Si la séance s'arrête quand l'écran s'éteint

Certains constructeurs coupent les applis en arrière-plan. Dans les réglages Android de l'appli, mets la batterie sur
« Sans restriction » (le nom varie). Si le téléphone arrête quand même openRun, la séance interrompue t'est proposée
à l'ouverture suivante.

## Montre

La version pour montre Wear OS n'est pas encore là. Le téléphone suffit.

# Contribuer à openRun

Merci de t'y intéresser. Quelques règles courtes pour que ça reste simple.

## Avant de coder

- Ouvre une issue pour discuter d'une fonctionnalité avant de t'y mettre: la [bible](docs/BIBLE.md) garde les décisions
  déjà prises (et pourquoi), et la [roadmap](README.md#roadmap) dit ce qui est prévu.
- Un bug se décrit avec: le téléphone, la version d'Android, la version d'openRun, et ce qui s'est passé.
  Pour un souci de GPS, la précision indiquée par l'appli et le type de sortie aident beaucoup.

## Règles du code

- **Ce qui décide vit dans `core/`, avec un test à côté.** Tout ce qui choisit la prochaine séance, calcule des
  calories, un rang, une allure, ou applique une règle (audio, progression, écriture santé) est une fonction pure en
  Kotlin, sans Android, testée dans `core/src/test`. Ça ne se vérifie pas en cliquant.
- **Peu de dépendances.** Une nouvelle bibliothèque doit justifier sa présence.
- **Aucun pistage, aucun compte, aucun réseau.** L'appli ne se connecte nulle part.
- **Les textes sont dans `strings.xml`**, en français pour l'instant, pour être traduisibles plus tard.
- Respecte le style des fichiers voisins (commentaires qui disent *pourquoi*, noms clairs, pas de code mort).

## Vérifier

```bash
./gradlew :core:test                      # doit rester vert
./gradlew :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest  # si tu as touché au stockage ou aux réglages (téléphone branché)
```

## Licence

En contribuant, tu acceptes que ton code soit publié sous la licence du projet, [GPL-3.0 ou plus](LICENSE).

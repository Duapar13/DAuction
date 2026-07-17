# Dossier `libs/`

Même API Spigot que pour les autres plugins D(nom) (26.1.2). Le dépôt Maven
local (`~/.m2`) est partagé par machine, pas par projet : si tu as déjà fait
la procédure une fois sur cette machine, rien à refaire ici.

Sinon, voir `DFaction/libs/README.md` pour la procédure complète (BuildTools
+ extraction + `mvn install:install-file`).

Ce projet dépend aussi de `com.duapar.dapi:DAPI:1.0.0` (dépendance dure) : fais
`mvn install` dans le dossier `DAPI` avant de compiler DAuction.

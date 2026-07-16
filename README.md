# DAuction

**Hôtel des ventes entre joueurs.** Mets un objet en vente à prix fixe,
d'autres joueurs le parcourent en GUI et l'achètent — l'argent passe par
[EconomyService](../DAPI) (fourni par [DEconomy](../DEconomy)), directement
dans le compte du vendeur, même hors ligne.

## Fonctionnalités

- **`/ah`** : parcourt toutes les annonces actives en GUI (tête d'objet,
  vendeur, prix, temps restant). Clique pour acheter.
- **`/ah sell <prix> [quantité]`** : met en vente l'objet tenu en main (toute
  la pile, ou une partie si une quantité est précisée).
- **`/ah my`** : GUI de tes propres annonces actives — clique pour annuler.
- **`/ah reclaim`** : GUI de tes objets annulés/expirés en attente de retrait
  — clique pour les récupérer dans ton inventaire.
- **`/ah cancel <id>`** / **`/ah collect <id>`** : équivalents en commande
  des clics GUI (utile à distance ou pour un admin).
- **`/ah history`** : tes 10 dernières transactions (vendues/achetées).
- **Taxe configurable** sur chaque vente conclue (prélevée, non reversée —
  un vrai sink économique).
- **Expiration automatique** : une annonce non vendue après X heures
  (configurable) expire, le vendeur est prévenu en jeu s'il est connecté et
  peut récupérer l'objet via `/ah reclaim`.
- **Limite d'annonces actives par joueur** et **liste noire d'objets**
  (bedrock, command blocks, spawners...) configurables.
- Stockage YAML local par défaut, ou MySQL — même pattern que les autres
  plugins `D(nom)` (les objets sont sérialisés en Base64, compatible avec
  les deux backends).

## Intégration DAPI

DAuction **dépend directement** de DAPI (`depend: [DAPI]`, pas
`softdepend`) : comme DGuard, sa fonction principale a réellement besoin
d'un service DAPI pour exister — ici, `EconomyService`. Sans lui, il n'y a
rien pour transférer l'argent d'un achat, donc pas d'hôtel des ventes
possible : le plugin se désactive proprement au démarrage si aucun plugin
d'économie (ex: DEconomy) n'est installé.

`plugin.yml` ajoute aussi `softdepend: [DEconomy]`, uniquement pour
influencer l'ordre de chargement de Bukkit : sans dépendance explicite
entre deux plugins, cet ordre n'est pas garanti (ni alphabétique, ni
stable), et un vrai déploiement a montré DAuction s'activer avant
DEconomy et se désactiver aussitôt faute d'`EconomyService` enregistré.
Ce n'est pas une vraie dépendance : un autre plugin fournissant
`EconomyService` fonctionnerait tout aussi bien, tant qu'il se charge
avant DAuction.

- **Consomme `EconomyService`** (DEconomy) : `has`/`withdraw`/`deposit` à
  chaque achat, `format` pour l'affichage des prix.
- **Consomme `FactionService`** (DFaction, optionnel) : affiche la faction
  du vendeur dans le détail d'une annonce en GUI, si disponible
  (`integration.show-faction-in-listing` dans `config.yml`).
- **Fournit `AuctionService`** : un futur plugin (ex: un événement
  automatique distribuant une récompense) pourrait mettre un objet en vente
  directement via DAPI, sans dépendre de DAuction.

### Autres idées d'interconnexion possibles

- Un futur `DLogs` pourrait s'abonner à chaque vente conclue pour un
  historique centralisé des plus grosses transactions du serveur.
- Un futur événement (`DEvents`) pourrait mettre en vente automatiquement
  un objet rare à la fin d'un boss/event via `AuctionService`.

## Commandes

| Commande | Description |
|---|---|
| `/ah` | Parcourir les annonces actives. |
| `/ah sell <prix> [quantité]` | Vendre l'objet en main. |
| `/ah my` | Tes annonces actives (annulation en un clic). |
| `/ah reclaim` | Récupérer un objet annulé/expiré. |
| `/ah cancel <id>` | Annuler une annonce (la tienne, ou n'importe laquelle en admin). |
| `/ah collect <id>` | Récupérer un objet par identifiant. |
| `/ah history` | Historique de tes 10 dernières transactions. |

## Permissions

| Permission | Défaut | Description |
|---|---|---|
| `dauction.use` | `true` | Vendre, acheter, consulter l'hôtel des ventes. |
| `dauction.admin` | `op` | Annuler la mise en vente de n'importe quel joueur. |

## Configuration (`config.yml`)

```yaml
storage:
  type: local   # local ou mysql
  mysql:
    host: localhost
    port: 3306
    database: dauction
    username: root
    password: ""

listing:
  duration-hours: 48
  max-active-per-player: 5
  min-price: 1
  max-price: 1000000
  tax-percent: 5

blacklisted-items:
  - BEDROCK
  - COMMAND_BLOCK
  - SPAWNER
  # ...

integration:
  show-faction-in-listing: true
```

## Compiler le projet

Dépend de l'API Spigot 26.1.2 et, en `provided`, de DAPI :

```
cd ../DAPI && mvn install
cd ../DAuction && mvn clean package
```

Pour tester réellement des achats, il faut aussi [DEconomy](../DEconomy)
installé sur le serveur (c'est lui qui fournit `EconomyService`). Voir
[`libs/README.md`](libs/README.md) pour la mise en place de l'API Spigot.

## Roadmap / idées d'extension

- Recherche/filtrage dans le GUI de navigation (par catégorie d'objet).
- Pagination au-delà de 54 annonces actives affichables simultanément.
- Vente aux enchères (prix qui monte, pas seulement prix fixe).
- Notification Discord des plus grosses ventes (comme DTicket).

## Licence

MIT — voir [`LICENSE`](LICENSE).

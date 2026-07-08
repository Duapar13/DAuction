package com.dauction.integration;

import com.dapi.DAPI;
import com.dapi.service.AuctionService;
import com.dapi.service.FactionService;
import com.dauction.manager.AuctionManager;
import com.dauction.service.DAuctionServiceImpl;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
 * Centralise les appels à DAPI. DAuction dépend "en dur" de DAPI (depend: [DAPI] dans
 * plugin.yml) : DAPI lui-même est garanti présent au démarrage, aucune isolation
 * contre NoClassDefFoundError n'est nécessaire ici (voir DGuard pour le même
 * raisonnement). FactionService reste individuellement optionnel (DFaction peut être
 * absent), d'où le null-check.
 */
public final class DAPIHook {

    private DAPIHook() {
    }

    public static void registerAuctionService(JavaPlugin plugin, AuctionManager auctionManager) {
        DAPI.registerPlugin(plugin, "AuctionService");
        DAPI.registerService(AuctionService.class, new DAuctionServiceImpl(auctionManager), plugin);
    }

    /**
     * @return une ligne du style "Faction: NomDeLaFaction" (ou "Faction: aucune"),
     * ou {@code null} si aucun plugin ne fournit FactionService (ex: DFaction non installé).
     */
    public static String getFactionLine(UUID playerId) {
        FactionService factionService = DAPI.getService(FactionService.class);
        if (factionService == null) {
            return null;
        }
        String factionName = factionService.getFactionName(playerId);
        return "Faction: " + (factionName != null ? factionName : "aucune");
    }
}

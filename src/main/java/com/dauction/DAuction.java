package com.dauction;

import com.dapi.DAPI;
import com.dapi.service.EconomyService;
import com.dauction.commands.AuctionCommand;
import com.dauction.gui.GuiListener;
import com.dauction.integration.DAPIHook;
import com.dauction.manager.AuctionManager;
import com.dauction.storage.AuctionStorage;
import com.dauction.storage.MySQLAuctionStorage;
import com.dauction.storage.YamlAuctionStorage;
import com.dauction.tasks.ExpirationTask;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class DAuction extends JavaPlugin {

    private AuctionStorage storage;
    private AuctionManager auctionManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        EconomyService economyService = DAPI.getService(EconomyService.class);
        if (economyService == null) {
            getLogger().severe("Aucun plugin d'économie (ex: DEconomy) n'est installé - "
                    + "DAuction ne peut pas fonctionner sans EconomyService.");
            getLogger().severe("Le plugin va se désactiver.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        String storageType = getConfig().getString("storage.type", "local");
        if ("mysql".equalsIgnoreCase(storageType)) {
            storage = new MySQLAuctionStorage(this,
                    getConfig().getString("storage.mysql.host", "localhost"),
                    getConfig().getInt("storage.mysql.port", 3306),
                    getConfig().getString("storage.mysql.database", "dauction"),
                    getConfig().getString("storage.mysql.username", "root"),
                    getConfig().getString("storage.mysql.password", ""),
                    getConfig().getBoolean("storage.mysql.useSSL", false));
        } else {
            storage = new YamlAuctionStorage(getDataFolder(), getLogger());
        }

        try {
            storage.init();
        } catch (Exception e) {
            getLogger().severe("Impossible d'initialiser le stockage (" + storageType + "): " + e.getMessage());
            getLogger().severe("Le plugin va se désactiver.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        auctionManager = new AuctionManager(storage, economyService);
        auctionManager.loadConfig(this, getLogger());

        try {
            auctionManager.seed(storage.loadListings());
        } catch (Exception e) {
            getLogger().severe("Erreur lors du chargement des annonces existantes: " + e.getMessage());
        }

        AuctionCommand auctionCommand = new AuctionCommand(this, auctionManager);
        PluginCommand ah = getCommand("ah");
        if (ah != null) {
            ah.setExecutor(auctionCommand);
            ah.setTabCompleter(auctionCommand);
        }

        getServer().getPluginManager().registerEvents(new GuiListener(auctionManager), this);

        new ExpirationTask(auctionManager).runTaskTimer(this, 20L * 60, 20L * 60 * 5);

        DAPIHook.registerAuctionService(this, auctionManager);

        getLogger().info("DAuction activé (stockage: " + storageType + ").");
    }

    @Override
    public void onDisable() {
        if (storage != null) {
            storage.close();
        }
    }
}

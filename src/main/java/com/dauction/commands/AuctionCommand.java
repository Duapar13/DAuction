package com.dauction.commands;

import com.dauction.gui.AuctionGuiBuilder;
import com.dauction.integration.DAPIHook;
import com.dauction.manager.AuctionException;
import com.dauction.manager.AuctionManager;
import com.dauction.model.AuctionListing;
import com.dauction.model.AuctionStatus;
import com.dauction.util.Display;
import com.dauction.util.Msg;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class AuctionCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "sell", "my", "reclaim", "cancel", "collect", "history", "help"
    );

    private final JavaPlugin plugin;
    private final AuctionManager auctionManager;

    public AuctionCommand(JavaPlugin plugin, AuctionManager auctionManager) {
        this.plugin = plugin;
        this.auctionManager = auctionManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("dauction.use")) {
            Msg.error(sender, "Tu n'as pas la permission d'utiliser l'hôtel des ventes.");
            return true;
        }

        try {
            if (args.length == 0) {
                handleBrowse(sender);
                return true;
            }
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "sell":
                    handleSell(sender, args);
                    break;
                case "my":
                    handleMy(sender);
                    break;
                case "reclaim":
                    handleReclaim(sender);
                    break;
                case "cancel":
                    handleCancel(sender, args);
                    break;
                case "collect":
                    handleCollect(sender, args);
                    break;
                case "history":
                    handleHistory(sender);
                    break;
                case "help":
                    sendHelp(sender);
                    break;
                default:
                    sendHelp(sender);
                    break;
            }
        } catch (AuctionException e) {
            Msg.error(sender, e.getMessage());
        }
        return true;
    }

    private Player requirePlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            throw new AuctionException("Seul un joueur peut utiliser cette commande.");
        }
        return (Player) sender;
    }

    private void handleBrowse(CommandSender sender) {
        Player player = requirePlayer(sender);
        List<AuctionListing> active = auctionManager.getActive();
        if (active.isEmpty()) {
            Msg.send(player, "Aucune annonce active pour le moment. Vends-en une avec /ah sell <prix>.");
            return;
        }
        boolean showFaction = plugin.getConfig().getBoolean("integration.show-faction-in-listing", true);
        Inventory gui = AuctionGuiBuilder.buildBrowseGui(active, auctionManager,
                listing -> factionLineFor(listing.getSellerId(), showFaction));
        player.openInventory(gui);
    }

    private String factionLineFor(UUID sellerId, boolean enabled) {
        if (!enabled || !plugin.getServer().getPluginManager().isPluginEnabled("DAPI")) {
            return null;
        }
        return DAPIHook.getFactionLine(sellerId);
    }

    private void handleSell(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (args.length < 2) {
            throw new AuctionException("Utilisation: /ah sell <prix> [quantité]");
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            throw new AuctionException("Tiens l'objet à vendre en main.");
        }

        long price = parsePositive(args[1], "Prix invalide");

        int amount = hand.getAmount();
        if (args.length >= 3) {
            amount = (int) parsePositive(args[2], "Quantité invalide");
            if (amount > hand.getAmount()) {
                throw new AuctionException("Tu n'as que " + hand.getAmount() + " de cet objet en main.");
            }
        }

        ItemStack toSell = hand.clone();
        toSell.setAmount(amount);

        AuctionListing listing = auctionManager.createListing(player.getUniqueId(), player.getName(), toSell, price);

        if (amount >= hand.getAmount()) {
            player.getInventory().setItemInMainHand(null);
        } else {
            ItemStack remaining = hand.clone();
            remaining.setAmount(hand.getAmount() - amount);
            player.getInventory().setItemInMainHand(remaining);
        }

        Msg.success(player, "Annonce #" + listing.getId() + " créée : " + Display.itemName(toSell)
                + " x" + amount + " pour " + auctionManager.format(price) + ".");
    }

    private void handleMy(CommandSender sender) {
        Player player = requirePlayer(sender);
        List<AuctionListing> own = auctionManager.getOwnActive(player.getUniqueId());
        if (own.isEmpty()) {
            Msg.send(player, "Tu n'as aucune annonce active.");
            return;
        }
        player.openInventory(AuctionGuiBuilder.buildMyGui(own, auctionManager));
    }

    private void handleReclaim(CommandSender sender) {
        Player player = requirePlayer(sender);
        List<AuctionListing> unclaimed = auctionManager.getOwnUnclaimed(player.getUniqueId());
        if (unclaimed.isEmpty()) {
            Msg.send(player, "Rien à récupérer pour le moment.");
            return;
        }
        player.openInventory(AuctionGuiBuilder.buildReclaimGui(unclaimed, auctionManager));
    }

    private void handleCancel(CommandSender sender, String[] args) {
        if (args.length < 2) {
            throw new AuctionException("Utilisation: /ah cancel <id>");
        }
        int id = parseId(args[1]);
        boolean isAdmin = sender.hasPermission("dauction.admin");
        UUID actorId = sender instanceof Player ? ((Player) sender).getUniqueId() : null;
        if (actorId == null && !isAdmin) {
            throw new AuctionException("Seul un joueur (ou un admin) peut utiliser cette commande.");
        }
        auctionManager.cancel(actorId, isAdmin, id);
        Msg.success(sender, "Annonce #" + id + " annulée.");
    }

    private void handleCollect(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (args.length < 2) {
            throw new AuctionException("Utilisation: /ah collect <id>");
        }
        int id = parseId(args[1]);
        AuctionListing listing = auctionManager.collect(player.getUniqueId(), id);

        Map<Integer, ItemStack> leftover = player.getInventory().addItem(listing.getItem().clone());
        for (ItemStack extra : leftover.values()) {
            player.getWorld().dropItem(player.getLocation(), extra);
        }
        Msg.success(player, "Tu as récupéré " + Display.itemName(listing.getItem()) + ".");
    }

    private void handleHistory(CommandSender sender) {
        Player player = requirePlayer(sender);
        List<AuctionListing> history = auctionManager.getOwnHistory(player.getUniqueId(), 10);
        if (history.isEmpty()) {
            Msg.send(player, "Aucun historique pour le moment.");
            return;
        }
        sender.sendMessage(ChatColor.DARK_GRAY + "==== " + ChatColor.YELLOW + "Historique" + ChatColor.DARK_GRAY + " ====");
        for (AuctionListing listing : history) {
            boolean wasSeller = listing.getSellerId().equals(player.getUniqueId());
            String role = wasSeller ? "Vendu" : "Acheté";
            sender.sendMessage(ChatColor.GOLD + "#" + listing.getId() + " " + ChatColor.GRAY + role + " "
                    + ChatColor.WHITE + Display.itemName(listing.getItem()) + ChatColor.GRAY + " - "
                    + auctionManager.format(listing.getPrice()) + " (" + statusLabel(listing.getStatus()) + ChatColor.GRAY + ")");
        }
    }

    private String statusLabel(AuctionStatus status) {
        switch (status) {
            case SOLD:
                return ChatColor.GREEN + "vendue";
            case CANCELLED:
                return ChatColor.RED + "annulée";
            case EXPIRED:
                return ChatColor.YELLOW + "expirée";
            case COLLECTED:
                return ChatColor.GRAY + "récupérée";
            default:
                return status.name();
        }
    }

    private int parseId(String raw) {
        String cleaned = raw.startsWith("#") ? raw.substring(1) : raw;
        try {
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            throw new AuctionException("Identifiant invalide: " + raw);
        }
    }

    private long parsePositive(String raw, String errorPrefix) {
        long value;
        try {
            value = Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new AuctionException(errorPrefix + ": " + raw);
        }
        if (value <= 0) {
            throw new AuctionException(errorPrefix + ": doit être positif.");
        }
        return value;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.DARK_GRAY + "==== " + ChatColor.YELLOW + "DAuction" + ChatColor.DARK_GRAY + " ====");
        sender.sendMessage(ChatColor.GOLD + "/ah" + ChatColor.GRAY + " - Parcourir l'hôtel des ventes.");
        sender.sendMessage(ChatColor.GOLD + "/ah sell <prix> [quantité]" + ChatColor.GRAY + " - Vendre l'objet en main.");
        sender.sendMessage(ChatColor.GOLD + "/ah my" + ChatColor.GRAY + " - Tes annonces actives.");
        sender.sendMessage(ChatColor.GOLD + "/ah reclaim" + ChatColor.GRAY + " - Récupérer un objet annulé/expiré.");
        sender.sendMessage(ChatColor.GOLD + "/ah cancel <id>" + ChatColor.GRAY + " - Annuler une annonce.");
        sender.sendMessage(ChatColor.GOLD + "/ah collect <id>" + ChatColor.GRAY + " - Récupérer un objet par ID.");
        sender.sendMessage(ChatColor.GOLD + "/ah history" + ChatColor.GRAY + " - Historique de tes transactions.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase(Locale.ROOT);
            return SUBCOMMANDS.stream().filter(s -> s.startsWith(partial)).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}

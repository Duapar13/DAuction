package com.duapar.dauction.storage;

import com.duapar.dauction.model.AuctionListing;
import com.duapar.dauction.model.AuctionStatus;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class MySQLAuctionStorage implements AuctionStorage {

    private final JavaPlugin plugin;
    private final String url;
    private final String username;
    private final String password;

    private Connection connection;

    public MySQLAuctionStorage(JavaPlugin plugin, String host, int port, String database,
                                String username, String password, boolean useSSL) {
        this.plugin = plugin;
        this.url = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=" + useSSL + "&allowPublicKeyRetrieval=true&autoReconnect=true";
        this.username = username;
        this.password = password;
    }

    @Override
    public void init() throws Exception {
        Class.forName(com.mysql.cj.jdbc.Driver.class.getName());
        connect();
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS dauction_listings (" +
                    "id INT PRIMARY KEY," +
                    "seller_uuid VARCHAR(36) NOT NULL," +
                    "seller_name VARCHAR(16) NOT NULL," +
                    "item LONGTEXT NOT NULL," +
                    "price BIGINT NOT NULL," +
                    "status VARCHAR(16) NOT NULL," +
                    "listed_at BIGINT NOT NULL," +
                    "expires_at BIGINT NOT NULL," +
                    "resolved_at BIGINT," +
                    "buyer_uuid VARCHAR(36)," +
                    "buyer_name VARCHAR(16))");
        }
    }

    private void connect() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(url, username, password);
        }
    }

    @Override
    public Map<Integer, AuctionListing> loadListings() throws SQLException {
        Map<Integer, AuctionListing> result = new HashMap<>();
        connect();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM dauction_listings")) {
            while (rs.next()) {
                int id = rs.getInt("id");
                UUID sellerId = UUID.fromString(rs.getString("seller_uuid"));
                String sellerName = rs.getString("seller_name");
                ItemStack item = ItemSerialization.deserialize(rs.getString("item"));
                long price = rs.getLong("price");
                AuctionStatus status = AuctionStatus.valueOf(rs.getString("status"));
                long listedAt = rs.getLong("listed_at");
                long expiresAt = rs.getLong("expires_at");
                long resolvedAtRaw = rs.getLong("resolved_at");
                Long resolvedAt = rs.wasNull() ? null : resolvedAtRaw;
                String buyerUuidStr = rs.getString("buyer_uuid");
                UUID buyerId = buyerUuidStr == null ? null : UUID.fromString(buyerUuidStr);
                String buyerName = rs.getString("buyer_name");

                result.put(id, new AuctionListing(id, sellerId, sellerName, item, price, status,
                        listedAt, expiresAt, resolvedAt, buyerId, buyerName));
            }
        }
        return result;
    }

    @Override
    public void saveListing(AuctionListing listing) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                connect();
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO dauction_listings (id, seller_uuid, seller_name, item, price, status, " +
                                "listed_at, expires_at, resolved_at, buyer_uuid, buyer_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                                "ON DUPLICATE KEY UPDATE status = VALUES(status), resolved_at = VALUES(resolved_at), " +
                                "buyer_uuid = VALUES(buyer_uuid), buyer_name = VALUES(buyer_name)")) {
                    ps.setInt(1, listing.getId());
                    ps.setString(2, listing.getSellerId().toString());
                    ps.setString(3, listing.getSellerName());
                    ps.setString(4, ItemSerialization.serialize(listing.getItem()));
                    ps.setLong(5, listing.getPrice());
                    ps.setString(6, listing.getStatus().name());
                    ps.setLong(7, listing.getListedAt());
                    ps.setLong(8, listing.getExpiresAt());
                    if (listing.getResolvedAt() != null) {
                        ps.setLong(9, listing.getResolvedAt());
                    } else {
                        ps.setNull(9, Types.BIGINT);
                    }
                    ps.setString(10, listing.getBuyerId() == null ? null : listing.getBuyerId().toString());
                    ps.setString(11, listing.getBuyerName());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Erreur MySQL lors de la sauvegarde de l'annonce #" + listing.getId(), e);
            }
        });
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erreur lors de la fermeture de la connexion MySQL", e);
        }
    }
}

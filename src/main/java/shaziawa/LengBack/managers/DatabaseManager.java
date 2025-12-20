package shaziawa.lengback.managers;

import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {
    private final JavaPlugin plugin;
    private Connection connection;
    private boolean initialized = false;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public synchronized void initializeDatabase() {
        if (initialized) return;
        
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(
                    "jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/death_locations.db");
            connection.setAutoCommit(true);

            try (Statement stmt = connection.createStatement()) {
                // 创建死亡位置表
                stmt.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS death_locations (" +
                        "uuid TEXT PRIMARY KEY, " +
                        "world TEXT NOT NULL, " +
                        "x REAL NOT NULL, " +
                        "y REAL NOT NULL, " +
                        "z REAL NOT NULL, " +
                        "yaw REAL NOT NULL, " +
                        "pitch REAL NOT NULL)");
                
                // 创建冷却时间表
                stmt.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS cooldowns (" +
                        "uuid TEXT PRIMARY KEY, " +
                        "cooldown_end INTEGER NOT NULL)");
                
                plugin.getLogger().info("数据库表初始化完成");
                initialized = true;
            }
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("§c找不到SQLite JDBC驱动: " + e.getMessage());
            closeConnection();
        } catch (SQLException e) {
            plugin.getLogger().severe("§c数据库初始化失败: " + e.getMessage());
            closeConnection();
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public void saveDeathLocation(UUID playerId, Location location) {
        if (!isConnected()) {
            plugin.getLogger().warning("尝试保存死亡位置时数据库连接不可用");
            return;
        }

        String sql = "INSERT OR REPLACE INTO death_locations (uuid, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerId.toString());
            stmt.setString(2, location.getWorld().getName());
            stmt.setDouble(3, location.getX());
            stmt.setDouble(4, location.getY());
            stmt.setDouble(5, location.getZ());
            stmt.setFloat(6, location.getYaw());
            stmt.setFloat(7, location.getPitch());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "§c保存死亡位置失败: " + e.getMessage(), e);
        }
    }

    public Location getDeathLocation(UUID playerId) {
        if (!isConnected()) {
            plugin.getLogger().warning("尝试获取死亡位置时数据库连接不可用");
            return null;
        }

        String sql = "SELECT * FROM death_locations WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerId.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Location(
                        plugin.getServer().getWorld(rs.getString("world")),
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z"),
                        rs.getFloat("yaw"),
                        rs.getFloat("pitch"));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "§c获取死亡位置失败: " + e.getMessage(), e);
        }
        return null;
    }

    public synchronized void saveCooldowns(Map<UUID, Long> cooldowns) throws SQLException {
        if (!isConnected()) {
            throw new SQLException("数据库连接不可用");
        }

        connection.setAutoCommit(false);
        try (PreparedStatement clearStmt = connection.prepareStatement("DELETE FROM cooldowns");
             PreparedStatement insertStmt = connection.prepareStatement(
                     "INSERT OR REPLACE INTO cooldowns (uuid, cooldown_end) VALUES (?, ?)")) {
            
            clearStmt.executeUpdate();
            
            for (Map.Entry<UUID, Long> entry : cooldowns.entrySet()) {
                insertStmt.setString(1, entry.getKey().toString());
                insertStmt.setLong(2, entry.getValue());
                insertStmt.addBatch();
            }
            insertStmt.executeBatch();
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public synchronized Map<UUID, Long> loadCooldowns() throws SQLException {
        if (!isConnected()) {
            throw new SQLException("数据库连接不可用");
        }

        Map<UUID, Long> result = new HashMap<>();
        String sql = "SELECT * FROM cooldowns";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                try {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    long cooldownEnd = rs.getLong("cooldown_end");
                    result.put(uuid, cooldownEnd);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("无效的UUID格式: " + rs.getString("uuid"));
                }
            }
        }
        return result;
    }

    public synchronized void clearAllCooldowns() throws SQLException {
        if (!isConnected()) {
            throw new SQLException("数据库连接不可用");
        }

        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM cooldowns")) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "清除冷却时间时发生SQL错误", e);
            throw e;
        }
    }

    public synchronized void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    // 尝试提交任何挂起的事务
                    try {
                        if (!connection.getAutoCommit()) {
                            connection.commit();
                        }
                    } catch (SQLException e) {
                        plugin.getLogger().log(Level.WARNING, "提交挂起事务时出错", e);
                    }
                    
                    connection.close();
                    plugin.getLogger().info("数据库连接已关闭");
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "§c关闭数据库连接失败: " + e.getMessage(), e);
            } finally {
                connection = null;
                initialized = false;
            }
        }
    }
}
package shaziawa.lengback.managers;

import org.bukkit.entity.Player;
import shaziawa.lengback.LengBack;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class CooldownManager {
    private final LengBack plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long DEFAULT_COOLDOWN = 20; // 20秒
    private static final long VIP_COOLDOWN = 10; // 10秒
    private static final long MAX_COOLDOWN = 300; // 5分钟，防止异常值
    private boolean loaded = false;

    public CooldownManager(LengBack plugin) {
        this.plugin = plugin;
    }

    private void ensureLoaded() {
        if (!loaded) {
            try {
                if (plugin.getDatabaseManager() != null && plugin.getDatabaseManager().isConnected()) {
                    Map<UUID, Long> loadedData = plugin.getDatabaseManager().loadCooldowns();
                    long currentTime = System.currentTimeMillis() / 1000;
                    
                    loadedData.forEach((uuid, endTime) -> {
                        if (endTime > currentTime && (endTime - currentTime) <= MAX_COOLDOWN) {
                            cooldowns.put(uuid, endTime);
                        }
                    });
                    loaded = true;
                    plugin.getLogger().info("成功加载 " + cooldowns.size() + " 个冷却时间记录");
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "加载冷却时间失败", e);
            }
        }
    }

    public boolean isOnCooldown(Player player) {
        ensureLoaded();
        UUID playerId = player.getUniqueId();
        if (!cooldowns.containsKey(playerId)) return false;
        
        long currentTime = System.currentTimeMillis() / 1000;
        long cooldownEnd = cooldowns.get(playerId);
        
        // 检测异常冷却时间
        if (cooldownEnd - currentTime > MAX_COOLDOWN) {
            plugin.getLogger().warning("检测到异常冷却时间，玩家: " + player.getName() + 
                                   "，已自动修正。原冷却剩余: " + (cooldownEnd - currentTime) + "秒");
            player.sendMessage(LengBack.PREFIX + "§c检测到异常冷却时间，已自动修复！");
            cooldownEnd = currentTime + getCooldownSeconds(player);
            cooldowns.put(playerId, cooldownEnd);
            saveCooldowns();
        }
        
        return currentTime < cooldownEnd;
    }

    public long getCooldownLeft(Player player) {
        ensureLoaded();
        UUID playerId = player.getUniqueId();
        if (!cooldowns.containsKey(playerId)) return 0;
        
        long currentTime = System.currentTimeMillis() / 1000;
        long cooldownEnd = cooldowns.get(playerId);
        
        long timeLeft = Math.max(0, cooldownEnd - currentTime);
        return Math.min(timeLeft, MAX_COOLDOWN);
    }

    public void setCooldown(Player player) {
        ensureLoaded();
        long cooldownTime = getCooldownSeconds(player);
        long currentTime = System.currentTimeMillis() / 1000;
        long cooldownEnd = currentTime + Math.min(cooldownTime, MAX_COOLDOWN);
        
        cooldowns.put(player.getUniqueId(), cooldownEnd);
        saveCooldowns();
    }

    public void clearCooldowns() {
        cooldowns.clear();
        loaded = true;
        
        try {
            // 检查数据库是否可用
            if (plugin.getDatabaseManager() != null && plugin.getDatabaseManager().isConnected()) {
                plugin.getDatabaseManager().clearAllCooldowns();
                plugin.getLogger().info("已清除所有冷却时间记录");
            } else {
                plugin.getLogger().warning("无法清除数据库中的冷却时间 - 数据库连接不可用");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "清除所有冷却时间失败", e);
        }
    }

    private long getCooldownSeconds(Player player) {
        return player.hasPermission(LengBack.VIP_PERMISSION) ? VIP_COOLDOWN : DEFAULT_COOLDOWN;
    }

    private void saveCooldowns() {
        try {
            if (plugin.getDatabaseManager() != null && plugin.getDatabaseManager().isConnected()) {
                plugin.getDatabaseManager().saveCooldowns(cooldowns);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "保存冷却时间失败", e);
        }
    }
}
package shaziawa.lengback.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import shaziawa.lengback.LengBack;
import shaziawa.lengback.managers.CooldownManager;
import shaziawa.lengback.managers.DatabaseManager;
import org.bukkit.Location; 

public class BackCommand implements CommandExecutor {
    private final LengBack plugin;
    private final DatabaseManager databaseManager;
    private final CooldownManager cooldownManager;

    public BackCommand(LengBack plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        this.cooldownManager = plugin.getCooldownManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(LengBack.PREFIX + "§c只有玩家才能使用此命令！");
            return true;
        }

        Player player = (Player) sender;

        // 检查权限
        if (!player.hasPermission(LengBack.USE_PERMISSION)) {
            player.sendMessage(LengBack.PREFIX + "§c你没有使用此命令的权限！");
            return true;
        }

        // 检查冷却
        if (cooldownManager.isOnCooldown(player)) {
            long secondsLeft = cooldownManager.getCooldownLeft(player);
            player.sendMessage(LengBack.PREFIX + String.format("§c命令冷却中，请等待 %d 秒后再试！", secondsLeft));
            return true;
        }

        // 获取死亡位置
        Location deathLocation = databaseManager.getDeathLocation(player.getUniqueId());
        if (deathLocation == null) {
            player.sendMessage(LengBack.PREFIX + "§c没有找到你的死亡位置记录！");
            return true;
        }

        // 传送玩家
        player.teleport(deathLocation);
        cooldownManager.setCooldown(player);
        player.sendMessage(LengBack.PREFIX + "§a已传送至上次死亡地点！");

        return true;
    }
}
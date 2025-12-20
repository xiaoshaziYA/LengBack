package shaziawa.lengback.listeners;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import shaziawa.lengback.LengBack;
import shaziawa.lengback.managers.DatabaseManager;
import org.bukkit.Location;

import java.util.UUID;

public class PlayerDeathListener implements Listener {
    private final LengBack plugin;
    private final DatabaseManager databaseManager;

    public PlayerDeathListener(LengBack plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerId = player.getUniqueId();
        Location deathLocation = player.getLocation();
        
        databaseManager.saveDeathLocation(playerId, deathLocation);
        
        // 构建可点击消息
        TextComponent prefix = new TextComponent(LengBack.PREFIX);
        TextComponent message = new TextComponent("§6检测到你不小心噶了，是否要返回上一死亡地点 ");
        
        TextComponent clickable = new TextComponent("§e[点我返回]");
        clickable.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/lback"));
        clickable.setHoverEvent(new HoverEvent(
            HoverEvent.Action.SHOW_TEXT, 
            new Text("§a点击返回死亡地点")
        ));
        
        player.spigot().sendMessage(prefix, message, clickable);
    }
}
package shaziawa.lengback;

import org.bukkit.plugin.java.JavaPlugin;
import shaziawa.lengback.commands.BackCommand;
import shaziawa.lengback.listeners.PlayerDeathListener;
import shaziawa.lengback.managers.CooldownManager;
import shaziawa.lengback.managers.DatabaseManager;

public class LengBack extends JavaPlugin {
    private DatabaseManager databaseManager;
    private CooldownManager cooldownManager;

    public static final String PREFIX = "§6[胡桃]§r ";
    public static final String USE_PERMISSION = "lengback.back";
    public static final String VIP_PERMISSION = "CFC.zanzhu";

    @Override
    public void onEnable() {
        // 保证数据文件夹存在
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        this.databaseManager = new DatabaseManager(this);
        
        // 先初始化数据库
        databaseManager.initializeDatabase();

        // 如果数据库连接失败，直接禁用插件
        if (databaseManager.getConnection() == null) {
            getLogger().severe("§c数据库初始化失败，插件已自动禁用！");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 数据库初始化成功后再创建CooldownManager
        this.cooldownManager = new CooldownManager(this);

        // 注册命令与监听器
        getCommand("lback").setExecutor(new BackCommand(this));
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);

        getLogger().info("§aLengBack 插件已启用! 作者: shazi_awa");
    }

    @Override
    public void onDisable() {
        // 先清除冷却时间，再关闭数据库连接
        if (cooldownManager != null) {
            cooldownManager.clearCooldowns();
        }
        if (databaseManager != null) {
            databaseManager.closeConnection();
        }
        getLogger().info("§cLengBack 插件已禁用!");
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }
}
package net.windit.mcpl.sudoku;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Main extends JavaPlugin {
    public static Config config;
    public static Logger logger;
    private EventListener listener;

    @Override
    public void onEnable() {
        logger = getLogger();
        config = new Config(getDataFolder());
        ConfigurationSerialization.registerClass(BoardPoint.class);
        try {
            config.loadConfig();
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "Can't load the config.", t);
        }
        listener = new EventListener(this);
        Bukkit.getServer().getPluginManager().registerEvents(listener, this);
        getCommand("sudoku").setExecutor(this);
    }

    @Override
    public void onDisable() {
        listener = null;
        config = null;
        logger = null;
    }

    @Override
    public boolean onCommand(final CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§c请确定你是OP后再执行本命令.");
            return true;
        }
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("set")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("该命令仅玩家可用.");
                    return true;
                }
                Player player = (Player) sender;
                listener.startSetting(player);
                player.sendMessage("§d现在开始设置吧~左击/右击选择方块.第一步:选择左上角羊毛.");
                return true;
            } else if (args[0].equalsIgnoreCase("reload")) {
                config.loadConfig();
                sender.sendMessage("§a载入配置成功");
                return true;
            }
        }
        return false;
    }
}

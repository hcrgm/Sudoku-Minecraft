package net.windit.mcpl.sudoku;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class Config {
    public BoardPoint board;
    public Location sign1; // start/restart-game sign location
    public Location sign2; // Sudoku-verification sign location
    public Location sign3; // End-game sign location
    public int generateCooldown;
    public boolean debug;
    public boolean alreadySet = false;
    private File configFile;
    private FileConfiguration config;

    public Config(File dataFolder) {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        configFile = new File(dataFolder, "config.yml");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void loadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
        config.addDefault("generate-cooldown", 20);
        config.addDefault("debug", false);
        board = (BoardPoint) config.get("board");
        sign1 = (Location) config.get("start-game-sign");
        sign2 = (Location) config.get("sudoku-verification-sign");
        sign3 = (Location) config.get("end-game-sign");
        generateCooldown = config.getInt("generate-cooldown");
        debug = config.getBoolean("debug");
        if (board != null && sign1 != null && sign2 != null && sign3 != null) {
            alreadySet = true;
        }
    }

    public void saveConfig() throws IOException {
        config.set("board", board);
        config.set("start-game-sign", sign1);
        config.set("sudoku-verification-sign", sign2);
        config.set("end-game-sign", sign3);
        config.set("generate-cooldown", generateCooldown);
        config.set("debug", debug);
        config.save(configFile);
        loadConfig();
    }
}

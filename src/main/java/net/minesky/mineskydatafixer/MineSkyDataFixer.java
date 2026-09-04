package net.minesky.mineskydatafixer;

import net.minesky.mineskydatafixer.command.RescueCommand;
import net.minesky.mineskydatafixer.service.PlayerDataManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class MineSkyDataFixer extends JavaPlugin {

    private PlayerDataManager playerDataManager;

    @Override
    public void onEnable() {
        this.playerDataManager = new PlayerDataManager(this);

        RescueCommand rescueCommand = new RescueCommand(this, this.playerDataManager);
        PluginCommand command = getCommand("mineskydatafixer");
        if (command != null) {
            command.setExecutor(rescueCommand);
            command.setTabCompleter(rescueCommand);
        }
    }

    @Override
    public void onDisable() {
    }

    public PlayerDataManager getPlayerDataManager() {
        return this.playerDataManager;
    }
}
package net.minesky.mineskydatafixer.command;

import net.kyori.adventure.text.Component;
import net.minesky.mineskydatafixer.service.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RescueCommand implements CommandExecutor, TabCompleter {

    private final Plugin plugin;
    private final PlayerDataManager dataManager;

    public RescueCommand(Plugin plugin, PlayerDataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("mineskydatafixer.admin")) {
            sender.sendMessage(Component.text("§cVoce nao tem permissao para executar este comando."));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Component.text("§cUso: /" + label + " <jogador> [mundo] [x] [y] [z]"));
            return true;
        }

        String targetName = args[0];
        Location targetLocation;

        if (args.length == 1) {
            World defaultWorld = Bukkit.getWorlds().get(0);
            targetLocation = defaultWorld.getSpawnLocation();
        } else if (args.length == 2) {
            World specifiedWorld = Bukkit.getWorld(args[1]);
            if (specifiedWorld == null) {
                sender.sendMessage(Component.text("§cMundo especificado nao existe."));
                return true;
            }
            targetLocation = specifiedWorld.getSpawnLocation();
        } else if (args.length >= 5) {
            World specifiedWorld = Bukkit.getWorld(args[1]);
            if (specifiedWorld == null) {
                sender.sendMessage(Component.text("§cMundo especificado nao existe."));
                return true;
            }
            try {
                double x = Double.parseDouble(args[2]);
                double y = Double.parseDouble(args[3]);
                double z = Double.parseDouble(args[4]);
                targetLocation = new Location(specifiedWorld, x, y, z);
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("§cCoordenadas invalidas."));
                return true;
            }
        } else {
            sender.sendMessage(Component.text("§cUso: /" + label + " <jogador> [mundo] [x] [y] [z]"));
            return true;
        }

        sender.sendMessage(Component.text("§eLocalizando dados e resgatando jogador §f" + targetName + "§e..."));

        this.dataManager.resolveUUID(targetName).thenAccept(uuid -> {
            if (uuid == null) {
                sender.sendMessage(Component.text("§cNao foi possivel encontrar o UUID para o jogador: " + targetName));
                return;
            }

            this.dataManager.rescue(uuid, targetLocation).thenAccept(success -> {
                if (success) {
                    sender.sendMessage(Component.text("§aJogador §f" + targetName + " §aresgatado com sucesso para §f"
                            + targetLocation.getWorld().getName() + " §7("
                            + targetLocation.getBlockX() + ", "
                            + targetLocation.getBlockY() + ", "
                            + targetLocation.getBlockZ() + ")§a."));
                } else {
                    sender.sendMessage(Component.text("§cErro ao resgatar jogador. Arquivo de dados nao encontrado ou corrompido."));
                }
            });
        });

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("mineskydatafixer.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    list.add(player.getName());
                }
            }
            return list;
        }

        if (args.length == 2) {
            List<String> worlds = new ArrayList<>();
            for (World world : Bukkit.getWorlds()) {
                if (world.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    worlds.add(world.getName());
                }
            }
            return worlds;
        }

        if (args.length == 3 || args.length == 4 || args.length == 5) {
            return List.of("~");
        }

        return Collections.emptyList();
    }
}
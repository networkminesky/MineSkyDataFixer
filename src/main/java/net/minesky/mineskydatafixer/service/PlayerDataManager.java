package net.minesky.mineskydatafixer.service;

import com.destroystokyo.paper.profile.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class PlayerDataManager {

    private final Plugin plugin;

    public PlayerDataManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<UUID> resolveUUID(String targetName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return UUID.fromString(targetName);
            } catch (IllegalArgumentException ignored) {
            }

            Player online = Bukkit.getPlayerExact(targetName);
            if (online != null) {
                return online.getUniqueId();
            }

            PlayerProfile profile = Bukkit.createProfile(targetName);
            if (profile.complete()) {
                UUID id = profile.getId();
                if (id != null) {
                    return id;
                }
            }

            return Bukkit.getOfflinePlayer(targetName).getUniqueId();
        });
    }

    public File findPlayerDataFile(UUID uuid) {
        for (World world : Bukkit.getWorlds()) {
            File worldDataFolder = new File(world.getWorldFolder(), "playerdata");
            File targetFile = new File(worldDataFolder, uuid.toString() + ".dat");
            if (targetFile.exists()) {
                return targetFile;
            }
        }

        World rootWorld = Bukkit.getWorlds().get(0);
        File defaultFolder = new File(rootWorld.getWorldFolder(), "playerdata");
        if (!defaultFolder.exists()) {
            defaultFolder.mkdirs();
        }
        return new File(defaultFolder, uuid.toString() + ".dat");
    }

    public CompletableFuture<Boolean> rescue(UUID uuid, Location targetLocation) {
        CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            player.getScheduler().run(plugin, task -> {
                player.kick(Component.text("§cSua posicao foi redefinida por um administrador para correcao de dados. Reconecte-se."));
            }, null);
        }

        plugin.getServer().getAsyncScheduler().runDelayed(plugin, scheduledTask -> {
            try {
                File dataFile = findPlayerDataFile(uuid);
                if (!dataFile.exists()) {
                    resultFuture.complete(false);
                    return;
                }

                Path path = dataFile.toPath();
                Path backup = path.resolveSibling(uuid + ".dat.rescue_bak");
                Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING);

                CompoundTag compound = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
                if (compound == null) {
                    resultFuture.complete(false);
                    return;
                }

                ListTag posList = new ListTag();
                posList.add(DoubleTag.valueOf(targetLocation.getX()));
                posList.add(DoubleTag.valueOf(targetLocation.getY()));
                posList.add(DoubleTag.valueOf(targetLocation.getZ()));
                compound.put("Pos", posList);

                ListTag rotList = new ListTag();
                rotList.add(FloatTag.valueOf(targetLocation.getYaw()));
                rotList.add(FloatTag.valueOf(targetLocation.getPitch()));
                compound.put("Rotation", rotList);

                ListTag motionList = new ListTag();
                motionList.add(DoubleTag.valueOf(0.0));
                motionList.add(DoubleTag.valueOf(0.0));
                motionList.add(DoubleTag.valueOf(0.0));
                compound.put("Motion", motionList);

                compound.putFloat("FallDistance", 0.0f);
                compound.putBoolean("OnGround", true);

                World destinationWorld = targetLocation.getWorld();
                if (destinationWorld != null) {
                    compound.putString("Dimension", destinationWorld.key().asString());
                }

                compound.remove("RootVehicle");
                compound.remove("Riding");
                compound.remove("Passengers");

                NbtIo.writeCompressed(compound, path);
                resultFuture.complete(true);
            } catch (Exception exception) {
                exception.printStackTrace();
                resultFuture.complete(false);
            }
        }, 600, TimeUnit.MILLISECONDS);

        return resultFuture;
    }
}
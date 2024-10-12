package me.caseload.knockbacksync;

import com.github.retrooper.packetevents.PacketEvents;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import dev.jorel.commandapi.*;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import me.caseload.knockbacksync.command.KnockbackSyncCommand;
import me.caseload.knockbacksync.command.MainCommand;
import me.caseload.knockbacksync.listener.*;
import me.caseload.knockbacksync.manager.ConfigManager;
import me.caseload.knockbacksync.scheduler.BukkitSchedulerAdapter;
import me.caseload.knockbacksync.scheduler.FabricSchedulerAdapter;
import me.caseload.knockbacksync.scheduler.FoliaSchedulerAdapter;
import me.caseload.knockbacksync.scheduler.SchedulerAdapter;
import me.caseload.knockbacksync.stats.StatsManager;
import me.lucko.commodore.Commodore;
import me.lucko.commodore.CommodoreProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.kohsuke.github.GitHub;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

public final class KnockbackSync extends JavaPlugin {

    public static Logger LOGGER;
    public static KnockbackSync INSTANCE;
    @Getter
    private SchedulerAdapter scheduler;
    public final Platform platform = getPlatform();

    private Platform getPlatform() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return Platform.FOLIA; // Paper (Folia) detected
        } catch (ClassNotFoundException ignored) {}

        try {
            Class.forName("net.fabricmc.loader.api.FabricLoader");
            return Platform.FABRIC; // Fabric detected
        } catch (ClassNotFoundException ignored) {}

        try {
            Class.forName("org.bukkit.Bukkit");
            return Platform.BUKKIT; // Bukkit (Spigot/Paper without Folia) detected
        } catch (ClassNotFoundException ignored) {}

        throw new IllegalStateException("Unknown platform!");
    }

    @Getter
    private final ConfigManager configManager = new ConfigManager();

    @Override
    public void onLoad() {
        CommandAPI.onLoad(new CommandAPIBukkitConfig(this));
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        LOGGER = getLogger();
        INSTANCE = this;
//        checkForUpdates();
        CommandDispatcher<CommandSourceStack> pluginCommandDispatcher = null;
        switch (platform) {
            case FOLIA:
                scheduler = new FoliaSchedulerAdapter(this);
//                pluginCommandDispatcher = new CommandDispatcher<>();
//                pluginCommandDispatcher.register(KnockbackSyncCommand.build());
                Brigadier.getCommandDispatcher().register(KnockbackSyncCommand.build());
                break;
            case BUKKIT:
                scheduler = new BukkitSchedulerAdapter(this);
                Brigadier.getCommandDispatcher().register(KnockbackSyncCommand.build());
                break;
            case FABRIC:
                scheduler = new FabricSchedulerAdapter();
                CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                        dispatcher.register(KnockbackSyncCommand.build()));
                break;
        }

        saveDefaultConfig();
        configManager.loadConfig(false);

//        CommandAPI.onEnable();
//        new MainCommand().register();

        registerListeners(
                new PlayerDamageListener(),
                new PlayerKnockbackListener(),
                new PlayerJoinQuitListener()
        );

        PacketEvents.getAPI().getEventManager().registerListeners(
                new AttributeChangeListener(),
                new PingReceiveListener()
        );

        PacketEvents.getAPI().getSettings()
                        .checkForUpdates(false)
                        .debug(false);

        PacketEvents.getAPI().load();
        PacketEvents.getAPI().init();

        StatsManager.init();

//        CommandMap commandMap = CommandAPIBukkit.get().getSimpleCommandMap();
//        commandMap.register("knockback", null);
    }

    @Override
    public void onDisable() {
//        CommandAPI.onDisable();
        PacketEvents.getAPI().terminate();
    }

    public static KnockbackSync getInstance() {
        return getPlugin(KnockbackSync.class);
    }

    private void registerListeners(Listener... listeners) {
        PluginManager pluginManager = getServer().getPluginManager();
        for (Listener listener : listeners)
            pluginManager.registerEvents(listener, this);
    }

    private void checkForUpdates() {
        getLogger().info("Checking for updates...");

        scheduler.runTaskAsynchronously(() -> {
            try {
                GitHub github = GitHub.connectAnonymously();
                String latestVersion = github.getRepository("CASELOAD7000/knockback-sync")
                        .getLatestRelease()
                        .getTagName();

                String currentVersion = getDescription().getVersion();
                boolean updateAvailable = !currentVersion.equalsIgnoreCase(latestVersion);

                if (updateAvailable) {
                    LOGGER.warning("A new update is available for download at: https://github.com/CASELOAD7000/knockback-sync/releases/latest");
                } else {
                    LOGGER.info("You are running the latest release.");
                }

                configManager.setUpdateAvailable(updateAvailable);
            } catch (Exception e) {
                LOGGER.severe("Failed to check for updates: " + e.getMessage());
            }
        });
    }
}
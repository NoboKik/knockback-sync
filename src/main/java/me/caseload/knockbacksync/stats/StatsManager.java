package me.caseload.knockbacksync.stats;

import me.caseload.knockbacksync.KnockbackSyncBase;
import me.caseload.knockbacksync.KnockbackSyncPlugin;
import org.bstats.bukkit.Metrics;

public class StatsManager {

    public static Metrics metrics;

    public static void init() {
        KnockbackSyncBase.INSTANCE.getScheduler().runTaskAsynchronously(() -> {
            BuildTypePie.determineBuildType(); // Function to calculate hash
            metrics = new Metrics(null, 23568);
            metrics.addCustomChart(new PlayerVersionsPie());
            metrics.addCustomChart(new BuildTypePie());
        });
    }
}

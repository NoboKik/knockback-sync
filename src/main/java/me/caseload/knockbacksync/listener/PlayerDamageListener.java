package me.caseload.knockbacksync.listener;

import me.caseload.knockbacksync.KnockbackSyncBase;
import me.caseload.knockbacksync.KnockbackSyncPlugin;
import me.caseload.knockbacksync.manager.PlayerData;
import me.caseload.knockbacksync.manager.PlayerDataManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class PlayerDamageListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!KnockbackSyncBase.INSTANCE.getConfigManager().isToggled())
            return;

        if (!(event.getEntity() instanceof Player victim) || !(event.getDamager() instanceof Player attacker))
            return;

        PlayerData playerData = PlayerDataManager.getPlayerData(victim.getUniqueId());
        if (playerData == null)
            return;

        playerData.setVerticalVelocity(playerData.calculateVerticalVelocity(attacker)); // do not move this calculation
        playerData.setLastDamageTicks(victim.getNoDamageTicks());
        playerData.updateCombat();

        if (!KnockbackSyncBase.INSTANCE.getConfigManager().isRunnableEnabled())
            playerData.sendPing();
    }
}
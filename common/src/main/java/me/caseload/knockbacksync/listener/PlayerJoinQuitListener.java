package me.caseload.knockbacksync.listener;

import me.caseload.knockbacksync.KnockbackSyncBase;
import me.caseload.knockbacksync.player.PlayerData;
import me.caseload.knockbacksync.manager.PlayerDataManager;
import me.caseload.knockbacksync.player.PlatformPlayer;
import me.caseload.knockbacksync.util.ChatUtil;

import java.util.UUID;

public abstract class PlayerJoinQuitListener {
    public void onPlayerJoin(PlayerData player) {
        PlayerDataManager.addPlayerData(player.getUuid(), player);
        PlatformPlayer platformPlayer = player.getPlatformPlayer();

        if (KnockbackSyncBase.INSTANCE.getConfigManager().isUpdateAvailable() && KnockbackSyncBase.INSTANCE.getConfigManager().isNotifyUpdate() && KnockbackSyncBase.INSTANCE.getPermissionChecker().hasPermission(platformPlayer,"knockbacksync.update"))
            platformPlayer.sendMessage(ChatUtil.translateAlternateColorCodes(
                    '&',
                    "&6An updated version of &eKnockbackSync &6is now available for download at: &bhttps://github.com/CASELOAD7000/knockback-sync/releases/latest"
            ));
    }

    public void onPlayerQuit(UUID uuid) {
        PlayerData playerData = PlayerDataManager.getPlayerData(uuid);
        if (playerData == null)
            return;

        if (playerData.isInCombat())
            playerData.quitCombat(true);

        PlayerDataManager.removePlayerData(uuid);
    }
}
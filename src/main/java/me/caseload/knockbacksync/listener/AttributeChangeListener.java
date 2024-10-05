package me.caseload.knockbacksync.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes;
import me.caseload.knockbacksync.manager.PlayerData;
import me.caseload.knockbacksync.manager.PlayerDataManager;
import me.caseload.knockbacksync.util.MathUtil;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class AttributeChangeListener extends PacketListenerAbstract {

    public static final UUID SPRINTING_MODIFIER_UUID =
            UUID.fromString("662A6B8D-DA3E-4C1C-8813-96EA6097278D");
    double minGravity = -1;
    double maxGravity = 1;
    double defaultGravity = 0.08;
    double currentGravity;

    public AttributeChangeListener() {
        currentGravity = defaultGravity;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        // Intercept the packet that updates entity properties (attributes)
        if (event.getPacketType() == PacketType.Play.Server.UPDATE_ATTRIBUTES) {
            WrapperPlayServerUpdateAttributes packet = new WrapperPlayServerUpdateAttributes(event);

            // Check if the entity is a player
            Player player = event.getPlayer();

            if (player != null) {
                // Get the attributes from the packet
                for (WrapperPlayServerUpdateAttributes.Property property : packet.getProperties()) {
                    // You can now check for specific attributes
                    if (property.getAttribute().equals(Attributes.GENERIC_GRAVITY)) {
                        onPlayerGravityChange(player, calculateValueWithModifiers(property));
                    }
                }
            }
        }
    }

    public double calculateValueWithModifiers(WrapperPlayServerUpdateAttributes.Property property) {
        double d0 = property.getValue();

        List<WrapperPlayServerUpdateAttributes.PropertyModifier> modifiers = property.getModifiers();
        // TODO, account for https://bugs.mojang.com/browse/MC-69459 ?
//        modifiers.removeIf(modifier -> modifier.getUUID().equals(SPRINTING_MODIFIER_UUID) ||
//                modifier.getName().getKey().equals("sprinting"));

        for (WrapperPlayServerUpdateAttributes.PropertyModifier attributemodifier : modifiers) {
            if (attributemodifier.getOperation() ==
                    WrapperPlayServerUpdateAttributes.PropertyModifier.Operation.ADDITION) {
                d0 += attributemodifier.getAmount();
            }
        }

        double d1 = d0;

        for (WrapperPlayServerUpdateAttributes.PropertyModifier attributemodifier : modifiers) {
            if (attributemodifier.getOperation() ==
                    WrapperPlayServerUpdateAttributes.PropertyModifier.Operation.MULTIPLY_BASE) {
                d1 += d0 * attributemodifier.getAmount();
            }
        }

        for (WrapperPlayServerUpdateAttributes.PropertyModifier attributemodifier : modifiers) {
            if (attributemodifier.getOperation() ==
                    WrapperPlayServerUpdateAttributes.PropertyModifier.Operation.MULTIPLY_TOTAL) {
                d1 *= 1.0D + attributemodifier.getAmount();
            }
        }

        // Note that gravity is effectively 0.07999999821186066 on 1.20.5+ instead of 0.08 by default
        double newValue = MathUtil.clampFloat((float) d1, (float) minGravity, (float) maxGravity);

        if (newValue < minGravity || newValue > maxGravity) {
            throw new IllegalArgumentException("New value must be between min and max!");
        }

        return this.currentGravity = newValue;
    }

    // Yes this is not properly latency compensated, that would require including a proper simulation engine
    // Laggy players will just have to deal with being on the wrong gravity for a few hundred ms, too bad!
    public void onPlayerGravityChange(Player player, double newGravity) {
        PlayerData playerData = PlayerDataManager.getPlayerData(player.getUniqueId());
        if (playerData.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_20_5)) {
            playerData.setGravity(newGravity);
        } else {
          currentGravity = defaultGravity;
        }
    }
}

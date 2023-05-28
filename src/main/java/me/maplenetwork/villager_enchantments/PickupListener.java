package me.maplenetwork.villager_enchantments;

import net.minecraft.network.protocol.game.PacketPlayOutAnimation;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.spigotmc.event.entity.EntityDismountEvent;

public class PickupListener implements Listener {
    private final JavaPlugin plugin;
    String CarryTag = "VillagerEnchantments:CarryVillager";

    public PickupListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getRightClicked().getType() == EntityType.VILLAGER) {
            Player player = event.getPlayer();
            if (player.isSneaking() && player.getInventory().getItemInMainHand().getType() == Material.AIR) {
                Villager villager = (Villager) event.getRightClicked();
                carryVillager(player, villager);
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();

        // Check if the player is shift right-clicking
        if (player.isSneaking() && (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) && (player.getInventory().getItemInMainHand().getType() == Material.AIR) && (isPlayerCarryingVillager(player))) {
            // Get the clicked block
            Block clickedBlock = event.getClickedBlock();

            if (clickedBlock != null) {
                // Get the block face that was clicked
                BlockFace clickedFace = event.getBlockFace();

                // Get the air block in front of the clicked face
                Block airBlock = clickedBlock.getRelative(clickedFace);

                // Check if the block in front of the clicked face is air
                if (airBlock.getType() == Material.AIR) {
                    // Get the location of the air block
                    Location airBlockLocation = airBlock.getLocation();
                    airBlockLocation.add(0.5, 0, 0.5);

                    AreaEffectCloud areaEffectCloud = (AreaEffectCloud) player.getPassengers().get(0);
                    Villager villager = (Villager) areaEffectCloud.getPassengers().get(0);

                    areaEffectCloud.remove();
                    villager.teleport(airBlockLocation);

                    if (VillagerEnchantments.minecraftVersionNumber == 19) {
                        try {
                            CraftPlayer craftPlayer = (CraftPlayer) player;
                            PacketPlayOutAnimation animationPacket = new PacketPlayOutAnimation(craftPlayer.getHandle(), 0);
                            craftPlayer.getHandle().b.a(animationPacket);

                        } catch (Exception ignored) {
                        }
                    }
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        // Check if the damaged entity is a villager
        if (event.getEntityType() == EntityType.VILLAGER) {
            Villager villager = (Villager) event.getEntity();

            // Check if the villager is riding an AreaEffectCloud and the damage cause is suffocation
            if (isVillagerBeingCarried(villager) && event.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION) {
                // Cancel the event to prevent damage
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof AreaEffectCloud && event.getDismounted() instanceof Player) {
            AreaEffectCloud areaEffectCloud = (AreaEffectCloud) event.getEntity();

            if (isAreaEffectCloudCarrier(areaEffectCloud)) {
                Player player = (Player) event.getDismounted();

                if (player.isInWater() || areaEffectCloud.isInWater()) {
                    event.setCancelled(true);
                } else {
                    areaEffectCloud.remove();
                }
            }
        } else if (event.getEntity() instanceof Villager && event.getDismounted() instanceof AreaEffectCloud) {
            AreaEffectCloud areaEffectCloud = (AreaEffectCloud) event.getDismounted();

            if (isAreaEffectCloudCarrier(areaEffectCloud)) {
                Villager villager = (Villager) event.getEntity();

                if (villager.isInWater() || areaEffectCloud.isInWater()) {
                    event.setCancelled(true);
                } else {
                    areaEffectCloud.remove();
                }
            }
        }
    }

    private boolean isAreaEffectCloudCarrier(AreaEffectCloud areaEffectCloud) {
        return areaEffectCloud.getMetadata(CarryTag) != null;
    }

    private boolean isPlayerCarryingVillager(Player player) {
        for (Entity passenger : player.getPassengers()) {
            if (passenger instanceof AreaEffectCloud && passenger.getMetadata(CarryTag) != null) {
                return true;
            }
        }
        return false;
    }

    private boolean isVillagerBeingCarried(Villager villager){
        Entity vehicle = villager.getVehicle();
        if (vehicle instanceof AreaEffectCloud && vehicle.getMetadata(CarryTag) != null) {
            return true;
        }
        return false;
    }

    private void carryVillager(Player player, Villager villager) {
        if (!isPlayerCarryingVillager(player)) {
            player.getWorld().spawn(player.getLocation(), AreaEffectCloud.class, areaEffectCloud -> {
                try { areaEffectCloud.setRadius(0); } catch (Exception ignored) { }
                try { areaEffectCloud.setGravity(false); } catch (Exception ignored) { }
                try { areaEffectCloud.setInvulnerable(true); } catch (Exception ignored) { }
                try { areaEffectCloud.setDuration(Integer.MAX_VALUE); } catch (Exception ignored) { }
                try { areaEffectCloud.setParticle(Particle.BLOCK_CRACK, Material.AIR.createBlockData()); } catch (Exception ignored) { }
                try { areaEffectCloud.setWaitTime(0); } catch (Exception ignored) { }

                areaEffectCloud.setMetadata(CarryTag, new FixedMetadataValue(plugin, Boolean.TRUE));

                player.addPassenger(areaEffectCloud);
                areaEffectCloud.addPassenger(villager);
            });
        }
    }
}

package me.maplenetwork.villager_enchantments;

import net.minecraft.network.protocol.game.PacketPlayOutAnimation;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.spigotmc.event.entity.EntityDismountEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class VillagerListener implements Listener {
    private final JavaPlugin plugin;
    private HashMap<UUID, List<MerchantRecipe>> storedTrades = new HashMap<>();
    String CarryTag = "Villager_Enchantments:CarryVillager";

    public VillagerListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }
        @EventHandler
        public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
            if (event.getRightClicked().getType() == EntityType.VILLAGER) {
                Villager villager = (Villager) event.getRightClicked();
                Player player = event.getPlayer();
                if (player.isSneaking()) {
                    if (player.getInventory().getItemInMainHand().getType() == Material.STICK) {
                        if (villager.getProfession() == Villager.Profession.NONE || villager.getProfession() == Villager.Profession.NITWIT) {
                            player.sendMessage("§cVillager needs a valid profession!");
                        } else if (villager.getVillagerExperience() != 0) {
                            player.sendMessage("§cVillager has already been traded with!");
                        } else {
                            rerollTrades(player, villager);
                        }
                        event.setCancelled(true);
                    } else if (player.getInventory().getItemInMainHand().getType() == Material.AIR) {
                        carryVillager(player, villager);
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

                        if (Villager_Enchantments.minecraftVersion.equals("1.19.4")){
                            CraftPlayer craftPlayer = (CraftPlayer) player;
                            PacketPlayOutAnimation animationPacket = new PacketPlayOutAnimation(craftPlayer.getHandle(), 0);
                            craftPlayer.getHandle().b.a(animationPacket);
                            event.setCancelled(true);
                        }
                    }
                }
            }
        }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();

            // Check if the player is left-clicking (attacking) the entity
            if (event.getEntity() instanceof Villager && player.isSneaking() && player.getInventory().getItemInMainHand().getType() == Material.STICK) {
                Villager villager = (Villager) event.getEntity();

                if (villager.getProfession() == Villager.Profession.NONE || villager.getProfession() == Villager.Profession.NITWIT) {
                    player.sendMessage("§cVillager needs a valid profession!");
                } else if (villager.getVillagerExperience() != 0) {
                    player.sendMessage("§cVillager has already been traded with!");
                } else {
                    restoreTrades(player, villager);
                }

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

    private void rerollTrades(Player player, Villager villager) {
        // Store the old trades
        List<MerchantRecipe> newTrades = villager.getRecipes();
        storedTrades.remove(villager.getUniqueId());
        storedTrades.put(villager.getUniqueId(), newTrades);

        final Villager.Profession previousProfession = villager.getProfession();
        villager.setProfession(Villager.Profession.NONE);
        villager.setProfession(previousProfession);

        player.sendMessage("§aVillager trades rerolled!");
        printVillagerTrades(player, villager);
    }

    public void restoreTrades(Player player, Villager villager) {
        UUID villagerUUID = villager.getUniqueId();
        List<MerchantRecipe> newTrades = villager.getRecipes();

        if (storedTrades.containsKey(villagerUUID)) {
            List<MerchantRecipe> oldTrades = storedTrades.get(villagerUUID);
            villager.setRecipes(oldTrades);

            // Remove the stored trades from the HashMap
            storedTrades.remove(villagerUUID);
            storedTrades.put(villager.getUniqueId(), newTrades);
            player.sendMessage("§aVillager trades restored!");
            printVillagerTrades(player, villager);
        } else {
            player.sendMessage("§cCan't find any trades to restore!");
        }
    }

    private void printVillagerTrades(Player player, Villager villager) {
        List<MerchantRecipe> recipes = villager.getRecipes();
        for (int i = 0; i < recipes.size(); i++) {
            MerchantRecipe recipe = recipes.get(i);
            List<ItemStack> ingredients = recipe.getIngredients();


            String Ingredients = "";

            for (int j = 0; j < ingredients.size(); j++) {
                ItemStack ingredient = ingredients.get(j);
                String materialName = ingredient.getType().toString();
                int amount = ingredient.getAmount();

                if (ingredient.getType() != Material.AIR) {
                    Ingredients += "§b" + materialName + "§f x §6" + amount + "§f";

                    if (j < ingredients.size() - 1 && ingredients.get(j+1).getType() != Material.AIR) {
                        Ingredients += ", ";
                    }
                }
            }


            ItemStack result = recipe.getResult();
            String materialName = result.getType().toString();
            int resultAmount = result.getAmount();

            String Results = "";

            Results += "§b" + materialName + "§f";

            if (result.getType() == Material.ENCHANTED_BOOK) {
                EnchantmentStorageMeta meta = (EnchantmentStorageMeta) result.getItemMeta();
                Map<Enchantment, Integer> enchantments = meta.getStoredEnchants();

                Results +=(" §d(");

                int enchantIndex = 0;
                for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                    Enchantment enchantment = entry.getKey();
                    int level = entry.getValue();
                    Results +=(enchantment.getKey().getKey() + " " + level);

                    if (enchantIndex < enchantments.size() - 1) {
                        Results +=(", ");
                    }

                    enchantIndex++;
                }

                Results +=(")§f");
            }

            Results += " x §6" + resultAmount + "§f";

            player.sendMessage("Trade " + (i + 1) + ":\n  - §cIngredients§f: " + Ingredients + "\n   - §aResult§f: " + Results);
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
                areaEffectCloud.setRadius(0);
                areaEffectCloud.setGravity(false);
                areaEffectCloud.setInvulnerable(true);
                areaEffectCloud.setDuration(Integer.MAX_VALUE);
                areaEffectCloud.setParticle(Particle.BLOCK_CRACK, Material.AIR.createBlockData());
                areaEffectCloud.setWaitTime(0);

                areaEffectCloud.setMetadata(CarryTag, new FixedMetadataValue(plugin, true));

                player.addPassenger(areaEffectCloud);
                areaEffectCloud.addPassenger(villager);
            });
        }
    }
}

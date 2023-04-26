package me.maplenetwork.villager_enchantments;

import net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.spigotmc.event.entity.EntityDismountEvent;
import org.spigotmc.event.entity.EntityMountEvent;

import java.util.List;
import java.util.Map;

public class VillagerListener implements Listener {
    private final JavaPlugin plugin;

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
                        rerollTrades(villager);
                        player.sendMessage("§aVillager trades rerolled!");
                        List<MerchantRecipe> recipes = villager.getRecipes();
                        for (int i = 0; i < recipes.size(); i++) {
                            MerchantRecipe recipe = recipes.get(i);
                            List<ItemStack> ingredients = recipe.getIngredients();


                            player.sendMessage("Trade " + (i + 1) + ":");

                            String Ingredients = "";

                            for (int j = 0; j < ingredients.size(); j++) {
                                ItemStack ingredient = ingredients.get(j);
                                String materialName = ingredient.getType().toString();
                                int amount = ingredient.getAmount();

                                Ingredients += materialName + " x " + amount;

                                if (j < ingredients.size() - 1) {
                                    Ingredients += ", ";
                                }
                            }


                            player.sendMessage("  - Ingredients: " + Ingredients);

                            for (int j = 0; j < ingredients.size(); j++) {
                                ItemStack ingredient = ingredients.get(j);
                                String materialName = ingredient.getType().toString();
                                int amount = ingredient.getAmount();

                                Ingredients += materialName + " x " + amount;

                                if (j < ingredients.size() - 1) {
                                    Ingredients += ", ";
                                }
                            }

                            ItemStack result = recipe.getResult();
                            String resultMaterialName = result.getType().toString();
                            int resultAmount = result.getAmount();

                            String Results = "";

                            Results += resultMaterialName;

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

                            Results += " x " + resultAmount;

                            player.sendMessage("  - Result: " + Results);
                        }
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
    public void onEntityMount(EntityMountEvent event) {
        System.out.println("EntityMountEvent " + event.getEntity().getType() + " " + event.getMount().getType());
        if (event.getEntity() instanceof Villager && event.getMount() instanceof ArmorStand) {
            Villager villager = (Villager) event.getEntity();

            villager.setInvulnerable(true);
        }
    }

    @EventHandler
    public void onEntityDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof ArmorStand && event.getDismounted() instanceof Player) {
            Player player = (Player) event.getDismounted();
            ArmorStand armorStand = (ArmorStand) event.getEntity();

            if (player.isInsideVehicle() && player.getLocation().getBlock().isLiquid()) {
                event.setCancelled(true);
            } else {
                armorStand.remove();
            }

        } else if (event.getEntity() instanceof Villager && event.getDismounted() instanceof ArmorStand) {
            Villager villager = (Villager) event.getEntity();

            if (villager.isInsideVehicle() && villager.getLocation().getBlock().isLiquid()) {
                event.setCancelled(true);
            } else {
                villager.setInvulnerable(false);
            }

        }

    }

    private void rerollTrades(Villager villager) {
        final Villager.Profession previousProfession = villager.getProfession();
        villager.setProfession(Villager.Profession.NONE);
        villager.setProfession(previousProfession);
    }

    private void carryVillager(Player player, Villager villager) {
        if (player.getPassengers().isEmpty()) {
            ArmorStand armorStand = (ArmorStand) player.getWorld().spawnEntity(player.getLocation(), EntityType.ARMOR_STAND);
            armorStand.setVisible(false);
            armorStand.setInvulnerable(true);
            armorStand.setGravity(false);
            armorStand.setSmall(true);
            armorStand.addPassenger(villager);
            player.addPassenger(armorStand);
        } else {
            for (Entity passenger : player.getPassengers()) {
                if (passenger instanceof ArmorStand && passenger.getPassengers().contains(villager)) {
                    passenger.eject();
                    villager.eject();
                    passenger.remove();
                }
            }
        }
    }
}

package me.maplenetwork.villager_enchantments;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class RerollListener implements Listener {
    private HashMap<UUID, List<MerchantRecipe>> storedTrades = new HashMap<>();
    private Material RerollItem;

    public RerollListener(JavaPlugin plugin) {
        String RerollItemString = plugin.getConfig().getString("item-used-to-reroll", "STICK");
        try {
            RerollItem = Material.valueOf(RerollItemString);
        } catch (IllegalArgumentException e) {
            RerollItem = Material.STICK;
            Bukkit.getLogger().log(Level.WARNING, "[VillagerEnchantments] " + (RerollItemString) + " is not a valid item. Defaulting to STICK.");
        }
    }

    @EventHandler
        public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
            if (event.getRightClicked().getType() == EntityType.VILLAGER) {
                Villager villager = (Villager) event.getRightClicked();
                Player player = event.getPlayer();
                if (player.isSneaking() && player.getInventory().getItemInMainHand().getType() == RerollItem) {
                    if (villager.getProfession() == Villager.Profession.NONE || villager.getProfession() == Villager.Profession.NITWIT) {
                        player.sendMessage("§cVillager needs a valid profession!");
                    } else if (villager.getVillagerExperience() != 0) {
                        player.sendMessage("§cVillager has already been traded with!");
                    } else {
                        rerollTrades(player, villager);
                    }
                    event.setCancelled(true);
                }
            }
        }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();

            // Check if the player is left-clicking (attacking) the entity
            if (event.getEntity() instanceof Villager && player.isSneaking() && player.getInventory().getItemInMainHand().getType() == RerollItem) {
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

    private void rerollTrades(Player player, Villager villager) {
        if (VillagerEnchantments.allowLock) {
            // Check if the player has a locked enchantment
            Boolean bypass = VillagerEnchantments.getPlugin(VillagerEnchantments.class).bypassLockedEnchantment.get(player.getUniqueId());
            if (bypass != null && bypass) {
                VillagerEnchantments.getPlugin(VillagerEnchantments.class).bypassLockedEnchantment.remove(player.getUniqueId());
                player.sendMessage(ChatColor.RED + "Bypass locked enchantment is now set to false.");
            } else {
                Enchantment lockedEnchantment = VillagerEnchantments.getPlugin(VillagerEnchantments.class).lockedEnchantments.get(player.getUniqueId());
                if (lockedEnchantment != null) {
                    for (MerchantRecipe recipe : villager.getRecipes()) {
                        ItemStack result = recipe.getResult();
                        if (result.getType() == Material.ENCHANTED_BOOK) {
                            EnchantmentStorageMeta meta = (EnchantmentStorageMeta) result.getItemMeta();
                            if (meta.hasStoredEnchant(lockedEnchantment)) {
                                Integer lockedEnchantmentLevel = VillagerEnchantments.getPlugin(VillagerEnchantments.class).lockedEnchantmentLevels.get(player.getUniqueId());
                                if (lockedEnchantmentLevel == null || meta.getStoredEnchantLevel(lockedEnchantment) == lockedEnchantmentLevel) {
                                    player.sendMessage(ChatColor.RED + "This villager has a trade with a locked enchantment. It will not be rerolled.");
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }

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

            player.sendMessage("Trade " + (i + 1) + ":\n  - §cIngredients§f: " + Ingredients + "\n  - §aResult§f: " + Results);
        }
    }
}

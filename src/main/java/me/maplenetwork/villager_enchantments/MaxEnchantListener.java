package me.maplenetwork.villager_enchantments;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class MaxEnchantListener implements Listener {
    @EventHandler
    public void onVillagerAcquireTrade(VillagerAcquireTradeEvent event) {
        MerchantRecipe recipe = event.getRecipe();
        ItemStack result = recipe.getResult();

        if (result.getType() == Material.ENCHANTED_BOOK) {
            EnchantmentStorageMeta meta = (EnchantmentStorageMeta) result.getItemMeta();
            boolean isMaxLevel = true;
            Map<Enchantment, Integer> maxEnchants = new HashMap<>();

            for (Enchantment enchantment : meta.getStoredEnchants().keySet()) {
                int currentLevel = meta.getStoredEnchantLevel(enchantment);
                int maxLevel = enchantment.getMaxLevel();

                if (currentLevel < maxLevel) {
                    isMaxLevel = false;
                    maxEnchants.put(enchantment, maxLevel);
                }
            }

            if (!isMaxLevel) {
                ItemStack newResult = new ItemStack(result.getType());
                EnchantmentStorageMeta newMeta = (EnchantmentStorageMeta) newResult.getItemMeta();

                for (Map.Entry<Enchantment, Integer> entry : maxEnchants.entrySet()) {
                    newMeta.addStoredEnchant(entry.getKey(), entry.getValue(), true);
                    // calculate cost based on max enchantment level
                    int costInEmeralds = calculateCost(entry.getKey(), entry.getValue());
                    ItemStack cost = new ItemStack(Material.EMERALD, costInEmeralds);
                    newResult.setItemMeta(newMeta);

                    MerchantRecipe newRecipe = new MerchantRecipe(newResult, recipe.getUses(), recipe.getMaxUses(), recipe.hasExperienceReward(), recipe.getVillagerExperience(), recipe.getPriceMultiplier());
                    newRecipe.addIngredient(cost);

                    event.setRecipe(newRecipe);
                }
            }
        }
    }

    private int calculateCost(Enchantment enchantment, int enchantmentLevel) {
        int cost;
        switch (enchantmentLevel) {
            case 1: cost = ThreadLocalRandom.current().nextInt(5, 19 + 1); break;
            case 2: cost = ThreadLocalRandom.current().nextInt(8, 32 + 1); break;
            case 3: cost = ThreadLocalRandom.current().nextInt(11, 45 + 1); break;
            case 4: cost = ThreadLocalRandom.current().nextInt(14, 58 + 1); break;
            case 5: cost = ThreadLocalRandom.current().nextInt(17, 64 + 1); break;
            default: cost = 64; // fallback value
        }

        // If it's a treasure enchantment, double the cost
        if (enchantment.isTreasure()) {
            cost = Math.min(cost * 2, 64); // Ensure the cost does not exceed 64
        }

        return cost;
    }


}

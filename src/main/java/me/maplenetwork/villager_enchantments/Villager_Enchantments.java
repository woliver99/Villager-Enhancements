package me.maplenetwork.villager_enchantments;

import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

public final class Villager_Enchantments extends JavaPlugin {
    static String minecraftVersion = Bukkit.getVersion().split("MC: ")[1].substring(0, Bukkit.getVersion().split("MC: ")[1].indexOf(')'));

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(new VillagerListener(this), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}

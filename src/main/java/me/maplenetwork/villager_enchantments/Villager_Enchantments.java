package me.maplenetwork.villager_enchantments;

import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

public final class Villager_Enchantments extends JavaPlugin {

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

package me.maplenetwork.villager_enchantments;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class VillagerEnchantments extends JavaPlugin {
    static String minecraftVersion = Bukkit.getVersion().split("MC: ")[1].substring(0, Bukkit.getVersion().split("MC: ")[1].indexOf(')'));
    static int minecraftVersionNumber = Integer.parseInt(minecraftVersion.split("\\.")[1]);

    static boolean allowLock = false;

    HashMap<UUID, Enchantment> lockedEnchantments = new HashMap<>();
    HashMap<UUID, Integer> lockedEnchantmentLevels = new HashMap<>();
    Map<UUID, Boolean> bypassLockedEnchantment = new HashMap<>();

    RerollListener rerollListener;
    PickupListener pickupListener;
    MaxEnchantListener maxEnchantListener;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        if (this.getConfig().getBoolean("allow-reroll", true)) {
            rerollListener = new RerollListener(this);
            getServer().getPluginManager().registerEvents(rerollListener, this);
        }

        if (this.getConfig().getBoolean("allow-villager-pickup", true)) {
            pickupListener = new PickupListener(this);
            getServer().getPluginManager().registerEvents(pickupListener, this);
        }

        if (this.getConfig().getBoolean("force-max-enchant", true)) {
            maxEnchantListener = new MaxEnchantListener();
            getServer().getPluginManager().registerEvents(maxEnchantListener, this);
        }

        allowLock = this.getConfig().getBoolean("allow-lock", true);
    }

    @Override
    public void onDisable() {
        if (rerollListener != null) {
            HandlerList.unregisterAll(rerollListener);
        }
        if (pickupListener != null) {
            HandlerList.unregisterAll(pickupListener);
        }
        if (maxEnchantListener != null) {
            HandlerList.unregisterAll(maxEnchantListener);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("villagerenchantments")) {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("reload")) {
                    if (sender.hasPermission("villagerenchantments.reload")) {
                        this.reloadConfig();
                        onDisable();
                        onEnable();
                        sender.sendMessage(ChatColor.GREEN + "Config reloaded.");
                    } else {
                        sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    }
                    return true;
                } else if (allowLock && args[0].equalsIgnoreCase("lock") && args.length > 1) {
                    if (args[1].equalsIgnoreCase("enchant")) {
                        if (!(sender instanceof Player)) {
                            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                            return true;
                        }

                        Player player = (Player) sender;

                        if (args.length > 2) {
                            String enchantKey = args[2].toLowerCase();
                            Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantKey));

                            if (enchantment == null) {
                                player.sendMessage(ChatColor.RED + "Invalid enchantment.");
                                return true;
                            }

                            lockedEnchantments.put(player.getUniqueId(), enchantment);
                            String ending = "";

                            if (args.length > 3) {
                                int level;
                                try {
                                    level = Integer.parseInt(args[3]);
                                } catch (NumberFormatException e) {
                                    player.sendMessage(ChatColor.RED + "Invalid level.");
                                    return true;
                                }
                                if (enchantment.getMaxLevel() < level || level < enchantment.getStartLevel()) {
                                    player.sendMessage(ChatColor.RED + "Invalid level.");
                                    return true;
                                }
                                lockedEnchantmentLevels.put(player.getUniqueId(), level);
                                ending = " " + level;
                            } else {
                                lockedEnchantmentLevels.remove(player.getUniqueId());
                            }

                            player.sendMessage(ChatColor.GREEN + "Locked enchantment set to " + enchantKey + ending + ".");
                        } else {
                            lockedEnchantments.remove(player.getUniqueId());
                            lockedEnchantmentLevels.remove(player.getUniqueId());
                            player.sendMessage(ChatColor.GREEN + "Locked enchantment removed.");
                        }
                        return true;
                    } else if (args[1].equalsIgnoreCase("bypass")) {
                        if (!(sender instanceof Player)) {
                            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                            return true;
                        }

                        Player player = (Player) sender;
                        boolean setBypassLockedEnchantment;
                        if (args.length > 2) {
                            String bypass = args[2].toLowerCase();
                            if (bypass.equals("true")) {
                                setBypassLockedEnchantment = true;
                            } else if (bypass.equals("false")) {
                                setBypassLockedEnchantment = false;
                            } else {
                                setBypassLockedEnchantment = !(bypassLockedEnchantment.getOrDefault(player.getUniqueId(), false));
                            }
                        } else {
                            setBypassLockedEnchantment = !(bypassLockedEnchantment.getOrDefault(player.getUniqueId(), false));
                        }
                        bypassLockedEnchantment.put(player.getUniqueId(), setBypassLockedEnchantment);
                        player.sendMessage(ChatColor.GREEN + "Bypass locked enchantment is now set to " + setBypassLockedEnchantment + ".");
                        return true;
                    }
                }
            }
            sender.sendMessage(ChatColor.RED + "Invalid Usage.");
            return false;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (cmd.getName().equalsIgnoreCase("villagerenchantments")) {
            if (args.length == 1) {
                ArrayList<String> completions = new ArrayList<>();
                if (sender.hasPermission("villagerenchantments.reload")) {
                    completions.add("reload");
                }
                if (allowLock) {
                    completions.add("lock");
                }
                return completions;
            } else if (allowLock) {
                if (args.length == 2) {
                    if (args[0].equalsIgnoreCase("lock")) {
                        return Arrays.asList("enchant", "bypass");
                    }
                } else if (args.length == 3 && args[1].equalsIgnoreCase("enchant")) {
                    List<String> completions = new ArrayList<>();
                    for (Enchantment enchantment : Enchantment.values()) {
                        completions.add(enchantment.getKey().getKey());
                    }
                    return completions;
                } else if (args.length == 3 && args[1].equalsIgnoreCase("bypass")) {
                    return Arrays.asList("true", "false");
                } else if (args.length == 4 && args[1].equalsIgnoreCase("enchant")) {
                    Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(args[2].toLowerCase()));
                    if (enchantment != null) {
                        List<String> levels = new ArrayList<>();
                        for (int i = enchantment.getStartLevel(); i <= enchantment.getMaxLevel(); i++) {
                            levels.add(String.valueOf(i));
                        }
                        return levels;
                    }
                }
            }
        }
        return Collections.emptyList();
    }

}

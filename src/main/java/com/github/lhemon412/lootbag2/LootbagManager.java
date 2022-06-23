package com.github.lhemon412.lootbag2;

import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;


import java.io.File;
import java.io.IOException;
import java.util.*;

public class LootbagManager implements Listener {
    private final Lootbag2Plugin plugin;

    private final LanguageManager langM;

    private final Map<String, Lootbag> idLootbagMap = new HashMap<>();
    private final Map<ItemStack, String> itemLootbagIDMap = new HashMap<>();
    private final List<String> sortedIds = new ArrayList<>();
    private final List<UUID> openingPlayers = new ArrayList<>();

    public LootbagManager(Lootbag2Plugin instance) {
        plugin = instance;
        langM = instance.getLangM();
        load();
    }

    public boolean reload() {
        unload();
        return load();
    }

    public void unload() {
        idLootbagMap.clear();
        itemLootbagIDMap.clear();
        sortedIds.clear();
    }

    public boolean load() {
        File lootbagSaveDir = new File(plugin.getDataFolder(), "lootbags");
        List<String> failList = new ArrayList<>();
        int successCount = 0;
        if (!lootbagSaveDir.exists()) return true;
        for (File lootbagFile : Objects.requireNonNull(lootbagSaveDir.listFiles())) {
            FileConfiguration lbConfig = new YamlConfiguration();
            String lbID = lootbagFile.getName().replaceFirst("[.][^.]+$", "");
            try {
                lbConfig.load(lootbagFile);
            } catch (IOException | InvalidConfigurationException e) {
                e.printStackTrace();
                failList.add(lbID);
                plugin.getLogger().severe("Error while loading saved lootbag files.");
                break;
            }
            Lootbag lb = new Lootbag();
            List<String> reqKeys = Arrays.asList("name", "size", "triggerItem", "contents");
            boolean valid = true;
            for (String key : reqKeys) {
                if (!lbConfig.contains(key)) {
                    failList.add(lbID);
                    valid = false;
                    break;
                }
            }
            if (!valid) continue;

            lb.setID(lbID);
            lb.setName(lbConfig.getString("name"));
            lb.setSize(lbConfig.getInt("size"));
            lb.setTriggerItem(ItemStack.deserialize(Objects.requireNonNull(lbConfig.getConfigurationSection("triggerItem")).getValues(false)));
            ConfigurationSection contents = Objects.requireNonNull(lbConfig.getConfigurationSection("contents"));
            for (String slot : contents.getKeys(false)) {
                lb.setItem(Integer.valueOf(slot), ItemStack.deserialize(Objects.requireNonNull(contents.getConfigurationSection(slot)).getValues(false)));
            }
            idLootbagMap.put(lbID, lb);
            itemLootbagIDMap.put(lb.getTriggerItem(), lbID);
            successCount++;
        }

        sortedIds.addAll(idLootbagMap.keySet());
        Collections.sort(sortedIds);

        // Print to console
        if (successCount > 0) {
            plugin.getLogger().info(langM.get("Message.LOAD_SUCCESS_REPORT").replaceAll("\\{count}", Integer.toString(successCount)));
        }
        if (!failList.isEmpty()) {
            plugin.getLogger().warning(langM.get("Message.LOAD_FAIL_REPORT").replaceAll("\\{count}", Integer.toString(failList.size())));
            plugin.getLogger().warning(StringUtils.join(failList, ","));
        }
        return true;
    }

    public List<String> getSortedIds() { return sortedIds; }

    public boolean idExists(String id) {
        return idLootbagMap.containsKey(id);
    }

    public boolean triggerItemExists(ItemStack itemStack) {
        return itemLootbagIDMap.containsKey(itemStack);
    }

    public void addLootbag(Lootbag lootbag) {
        idLootbagMap.put(lootbag.getID(), lootbag);
        itemLootbagIDMap.put(lootbag.getTriggerItem(), lootbag.getID());
        sortedIds.add(lootbag.getID());
        Collections.sort(sortedIds);
        saveLootbag(lootbag);
    }

    public boolean removeLootbag(String Id) {
        Lootbag lb = getLootbag(Id);
        itemLootbagIDMap.remove(lb.getTriggerItem());
        idLootbagMap.remove(Id);
        sortedIds.remove(Id);
        File lbFile = new File(plugin.getDataFolder(), "lootbags/" + Id + ".yml");
        return lbFile.delete();
    }

    public void renameLootbag(String Id, String newName) {
        idLootbagMap.get(Id).setName(newName);
        saveLootbag(idLootbagMap.get(Id));
    }

    public void saveLootbag(Lootbag lootbag) {
        FileConfiguration lbConfig = new YamlConfiguration();
        lbConfig.set("name", lootbag.getName());
        lbConfig.set("size", lootbag.getSize());
        lbConfig.set("triggerItem", lootbag.getTriggerItem().serialize());
        lootbag.getContents().forEach((slot, item) -> lbConfig.set("contents." + slot, item.serialize()));
        File dest = new File(plugin.getDataFolder(), "lootbags/" + lootbag.getID() + ".yml");
        try {
            lbConfig.save(dest);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<ItemStack, String> getItemLootbagIDMap() { return itemLootbagIDMap; }

    public Map<String, Lootbag> getIdLootbagMap() { return idLootbagMap; }

    public Lootbag getLootbag(String Id) { return idLootbagMap.get(Id); }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
            if (e.getItem() != null) {
                ItemStack handItem = e.getItem().clone();
                handItem.setAmount(1);
                if (itemLootbagIDMap.containsKey(handItem)) {
                    e.setCancelled(true);
                    String lbID = itemLootbagIDMap.get(handItem);
                    e.getPlayer().getInventory().getItemInMainHand().setAmount(e.getItem().getAmount() - 1);
                    idLootbagMap.get(lbID).openInventory(e.getPlayer());
                    openingPlayers.add(e.getPlayer().getUniqueId());
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (openingPlayers.contains(e.getPlayer().getUniqueId())) {
            openingPlayers.remove(e.getPlayer().getUniqueId());
            List<ItemStack> remainedItems = new ArrayList<>();
            for (ItemStack item : e.getInventory().getStorageContents()) {
                if (item != null) remainedItems.add(item);
            }
            if (remainedItems.size() > 0) {
                HashMap<Integer, ItemStack> dropItems = e.getPlayer().getInventory().addItem(remainedItems.toArray(new ItemStack[0]));
                if (dropItems.size() > 0) {
                    dropItems.forEach((index, item) -> e.getPlayer().getWorld().dropItem(e.getPlayer().getLocation(), item));
                }
            }
        }
    }
}

package com.github.lhemon412.lootbag2;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class Lootbag {
    private String id;
    private String name;
    private final Map<Integer, ItemStack> contents = new HashMap<>();
    private Integer size;
    private ItemStack triggerItem;

    public void setID(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setItem(Integer slot, ItemStack itemStack) {
        contents.put(slot, itemStack);
    }

    public void removeItem(Integer slot) { contents.remove(slot); }

    public void setSize(Integer size) {
        this.size = size;
    }

    public void setTriggerItem(ItemStack itemStack) {
        this.triggerItem = itemStack;
    }

    public String getID() { return id; }

    public String getName() { return name; }

    public Integer getSize() { return size; }

    public ItemStack getTriggerItem() { return triggerItem; }

    public Map<Integer, ItemStack> getContents() {
        return contents;
    }

    public void openInventory(Player player) {
        Inventory inv = Bukkit.createInventory(null, size, name);
        contents.forEach(inv::setItem);
        player.openInventory(inv);
    }
}

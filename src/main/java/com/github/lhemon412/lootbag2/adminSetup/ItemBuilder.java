package com.github.lhemon412.lootbag2.adminSetup;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ItemBuilder
{
    private final ItemStack itemStack;

    public ItemBuilder(Material material) {
        this.itemStack = new ItemStack(material);
    }

    public ItemBuilder(ItemStack itemStack) { this.itemStack = itemStack.clone();}

    private ItemMeta getItemMeta() {
        return itemStack.hasItemMeta() ? itemStack.getItemMeta() : Bukkit.getItemFactory().getItemMeta(itemStack.getType());
    }

    /**
     * Set the display name of the item
     * @param displayName display name of the item
     * @return ItemBuilder
     */
    public ItemBuilder setDisplayName(String displayName) {
        ItemMeta itemMeta = getItemMeta();
        itemMeta.setDisplayName(displayName);
        itemStack.setItemMeta(itemMeta);
        return this;
    }

    public ItemBuilder addLore(String... loreLines) {
        ItemMeta itemMeta = getItemMeta();
        List<String> loreList = itemMeta.hasLore() ? itemMeta.getLore() : new ArrayList<>();
        assert loreList != null;
        loreList.addAll(Arrays.asList(loreLines));
        itemMeta.setLore(loreList);
        itemStack.setItemMeta(itemMeta);
        return this;
    }

    public ItemBuilder addLore(List<String> loreLines) {
        ItemMeta itemMeta = getItemMeta();
        List<String> loreList = itemMeta.hasLore() ? itemMeta.getLore() : new ArrayList<>();
        assert loreList != null;
        loreList.addAll(loreLines);
        itemMeta.setLore(loreList);
        itemStack.setItemMeta(itemMeta);
        return this;
    }

    public ItemStack getItem() {
        return itemStack;
    }
}

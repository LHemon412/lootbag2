package com.github.lhemon412.lootbag2;

import com.github.lhemon412.lootbag2.adminSetup.GuiHandler;
import com.github.lhemon412.lootbag2.adminSetup.MainCommand;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Colorable;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Scanner;

public final class Lootbag2Plugin extends JavaPlugin {
    private LootbagManager lbM;
    private LanguageManager langM;
    private GuiHandler guiHandler;

    public ItemStack WHITE_STAINED_GLASS_PANE;
    public ItemStack BLACK_STAINED_GLASS_PANE;
    public ItemStack ENDER_EYE;

    @SuppressWarnings({"ConstantConditions", "deprecation"})
    @Override
    public void onEnable() {
        // Plugin startup logic
        int ver2 = Integer.parseInt(Bukkit.getBukkitVersion().split("-")[0].split("\\.")[1]);
        if (ver2 >= 13) {
            // 1.13+
            WHITE_STAINED_GLASS_PANE = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
            BLACK_STAINED_GLASS_PANE = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            ENDER_EYE = new ItemStack(Material.ENDER_EYE);
        } else {
            // 1.12 or below
            assert Material.getMaterial("STAINED_GLASS_PANE") != null;
            WHITE_STAINED_GLASS_PANE = new ItemStack(Material.getMaterial("STAINED_GLASS_PANE"), 1, (short) 0);
            BLACK_STAINED_GLASS_PANE = new ItemStack(Material.getMaterial("STAINED_GLASS_PANE"), 1, (short) 15);
            ENDER_EYE = new ItemStack(Material.getMaterial("EYE_OF_ENDER"));
        }

        saveDefaultConfig();

        langM = new LanguageManager(this, getConfig().getString("language"));
        lbM = new LootbagManager(this);
        MainCommand mc = new MainCommand(this);
        Objects.requireNonNull(this.getCommand("lootbag")).setExecutor(mc);
        Objects.requireNonNull(this.getCommand("lootbag")).setTabCompleter(mc);
        guiHandler = new GuiHandler(this);
        getServer().getPluginManager().registerEvents(guiHandler, this);
        getServer().getPluginManager().registerEvents(lbM, this);

        try {
            URL url = new URL("https://raw.githubusercontent.com/LHemon412/lootbag2/main/lastest_version.txt");
            Scanner scanner = new Scanner(url.openStream(), StandardCharsets.UTF_8.toString());
            scanner.useDelimiter("\\A");
            String version = scanner.hasNext() ? scanner.next().trim() : "";
            if (!version.equalsIgnoreCase(getVersion())) {
                getLogger().info(langM.get("Message.NEW_VER_AVAILABLE").replaceAll("\\{version}", version));
            }
        } catch (IOException e) {
            e.printStackTrace();
            getLogger().warning(langM.get("Message.CHECK_UPDATE_FAILED"));
        }
    }
    public LootbagManager getLbm() {
        return lbM;
    }

    public LanguageManager getLangM() {
        return langM;
    }

    public GuiHandler getGuiHandler() {
        return guiHandler;
    }

    public String getVersion() {
        return "2.0";
    }
}

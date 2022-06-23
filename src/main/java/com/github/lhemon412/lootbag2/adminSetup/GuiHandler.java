package com.github.lhemon412.lootbag2.adminSetup;

import com.github.lhemon412.lootbag2.LanguageManager;
import com.github.lhemon412.lootbag2.Lootbag;
import com.github.lhemon412.lootbag2.Lootbag2Plugin;
import com.github.lhemon412.lootbag2.LootbagManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GuiHandler implements Listener {
    private final Lootbag2Plugin plugin;
    private final LanguageManager langM;
    private final LootbagManager lbM;
    private final Map<UUID, Lootbag> setupLootbag = new HashMap<>();
    private final Map<UUID, String> setupState = new HashMap<>();
    private final Map<UUID, String> guiState = new HashMap<>();
    /*  main            Main Title (/lb setup)
    /   list            View List of Lootbags
    /   action          Action title of a lootbag
    /   view_contents   Viewing content of a lootbag
    /   rename:ID       Rename a lootbag with specific ID
    /   delete:ID       Delete a lootbag with specific ID
    */

    ItemStack frame;
    ItemStack blank;

    public GuiHandler(Lootbag2Plugin instance) {
        plugin = instance;
        langM = instance.getLangM();
        lbM = instance.getLbm();
        frame = new ItemBuilder(plugin.WHITE_STAINED_GLASS_PANE)
                .setDisplayName("§f")
                .getItem();
        blank = new ItemBuilder(plugin.BLACK_STAINED_GLASS_PANE)
                .setDisplayName("§f")
                .getItem();
    }

    public void showGui(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, langM.get("Gui.MAIN_TITLE"));
        gui.setItem(12, new ItemBuilder(Material.EMERALD_BLOCK)
                .setDisplayName(langM.get("Gui.CREATE_LOOTBAG"))
                .addLore(langM.getList("Gui.CREATE_LOOTBAG_LORE"))
                .getItem());
        gui.setItem(14, new ItemBuilder(Material.CHEST)
                .setDisplayName(langM.get("Gui.VIEW_LIST"))
                .getItem());
        guiState.put(player.getUniqueId(), "main");
        player.openInventory(gui);
    }

    private String colorize(String string) {
        return string.replaceAll("&", "§");
    }

    static List<Integer> blankSlots = Arrays.asList(10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43);
    static List<Integer> frameSlots = Arrays.asList(0,1,2,3,4,5,6,7,8,9,17,18,26,27,35,36,44,45,45,46,47,48,49,50,51,52,53);

    private void openLootbagList(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, langM.get("Gui.LIST_TITLE"));
        frameSlots.forEach(slot -> gui.setItem(slot, frame));
        blankSlots.forEach(slot -> gui.setItem(slot, blank));

        int maxPage = (int) Math.ceil((double) lbM.getIdLootbagMap().size() / 28);
        gui.setItem(49, new ItemBuilder(Material.COMPASS)
                .setDisplayName(langM.get("Gui.PAGE")
                        .replaceAll("\\{now/max}", "1/" + maxPage))
                .getItem());
        if (maxPage > 1) {
            gui.setItem(50, new ItemBuilder(Material.REDSTONE)
                    .setDisplayName(langM.get("Gui.NEXT_PAGE"))
                    .getItem());
        }

        int curSlot = 10;
        int idIndex = 0;
        List<String> sortedIds = lbM.getSortedIds();
        while (curSlot < 54 && idIndex < sortedIds.size()) {
            String lbID = sortedIds.get(idIndex);
            Lootbag lb = lbM.getIdLootbagMap().get(lbID);
            ItemStack item = new ItemBuilder(lb.getTriggerItem())
                    .setDisplayName(lb.getName() + " §8(" + lb.getID() + ")")
                    .addLore("", langM.get("Gui.MORE_ACTIONS"))
                    .getItem();
            gui.setItem(curSlot, item);
            curSlot++;
            while (frameSlots.contains(curSlot)) curSlot++;
            idIndex++;
        }
        player.openInventory(gui);
        guiState.put(player.getUniqueId(), "list");
    }

    @SuppressWarnings("ConstantConditions")
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();

        if (!guiState.containsKey(player.getUniqueId())) return;

        String guiTitle = e.getView().getTitle();
        String state = guiState.get(player.getUniqueId());

        if (!state.equals("edit_contents")) e.setCancelled(true);

        switch (state) {
            case "main":
                // Main Setting Gui
                if (e.getSlot() == 12) {
                    // Create lootbag
                    player.closeInventory();
                    player.sendMessage(langM.getPrefixed("Message.ENTER_ID"));
                    setupLootbag.put(player.getUniqueId(), new Lootbag());
                    setupState.put(player.getUniqueId(), "id");
                } else if (e.getSlot() == 14) {
                    // View lootbag list
                    player.closeInventory();
                    openLootbagList(player);
                }
                break;
            case "list":
                // List View GUI
                if (e.getCurrentItem() == null) return;
                // Clicked lootbag item
                if (blankSlots.contains(e.getSlot())) {
                    if (Objects.requireNonNull(e.getCurrentItem().getItemMeta()).getDisplayName().equals("§f")) return;
                    Pattern idPattern = Pattern.compile("\\([^)]*\\)$");
                    Matcher m = idPattern.matcher(e.getCurrentItem().getItemMeta().getDisplayName());
                    String lbID = "";
                    if (m.find()) {
                        lbID = m.group().replaceAll("\\(", "").replaceAll("\\)", "");
                    }

                    player.closeInventory();
                    Inventory gui = Bukkit.createInventory(null, 54, langM.get("Gui.ACTION_TITLE") + " §8(" + lbID + ")");
                    Lootbag lb = lbM.getIdLootbagMap().get(lbID);
                    gui.setItem(13, lb.getTriggerItem());
                    gui.setItem(29, new ItemBuilder(plugin.ENDER_EYE)
                            .setDisplayName(langM.get("Gui.VIEW_CONTENT"))
                            .getItem());
                    gui.setItem(30, new ItemBuilder(Material.CHEST)
                            .setDisplayName(langM.get("Gui.EDIT_CONTENT"))
                            .getItem());
                    gui.setItem(31, new ItemBuilder(Material.NAME_TAG)
                            .setDisplayName(langM.get("Gui.RENAME"))
                            .getItem());
                    gui.setItem(32, new ItemBuilder(Material.EMERALD_BLOCK)
                            .setDisplayName(langM.get("Gui.GET_ONE"))
                            .getItem());
                    gui.setItem(33, new ItemBuilder(Material.BARRIER)
                            .setDisplayName(langM.get("Gui.DELETE"))
                            .getItem());
                    gui.setItem(45, new ItemBuilder(Material.PAPER)
                            .setDisplayName(langM.get("Gui.BACK_TO_LIST"))
                            .getItem());
                    player.openInventory(gui);
                    guiState.put(player.getUniqueId(), "action");
                }
                // Clicked Next Page or Last Page
                else if (e.getSlot() == 50 || e.getSlot() == 48) {
                    if (Objects.requireNonNull(e.getCurrentItem().getItemMeta()).getDisplayName().equals("§f")) return;
                    Pattern pattern = Pattern.compile("\\d/\\d");
                    Matcher m = pattern.matcher(e.getInventory().getItem(49).getItemMeta().getDisplayName());
                    int curPage = 1;
                    if (m.find()) {
                        curPage = Integer.parseInt(m.group().split("/")[0]);
                    }
                    if (e.getSlot() == 50) curPage++;
                    else curPage--;

                    Inventory gui = Bukkit.createInventory(null, 54, langM.get("Gui.LIST_TITLE"));
                    frameSlots.forEach(slot -> gui.setItem(slot, frame));
                    blankSlots.forEach(slot -> gui.setItem(slot, blank));

                    int curSlot = 10;
                    for (int i=28*curPage-28; i<28*curPage; i++) {
                        if (lbM.getSortedIds().size() == i) break;
                        String lbID = lbM.getSortedIds().get(i);
                        Lootbag lb = lbM.getIdLootbagMap().get(lbID);
                        ItemStack item = new ItemBuilder(lb.getTriggerItem())
                                .setDisplayName(lb.getName() + " §8(" + lb.getID() + ")")
                                .addLore("", langM.get("Gui.MORE_ACTIONS"))
                                .getItem();
                        gui.setItem(curSlot, item);
                        curSlot++;
                        while (frameSlots.contains(curSlot)) curSlot++;
                    }

                    int maxPage = (int) Math.ceil((double) lbM.getIdLootbagMap().size() / 28);
                    gui.setItem(49, new ItemBuilder(Material.COMPASS)
                            .setDisplayName(langM.get("Gui.PAGE")
                                    .replaceAll("\\{now/max}", curPage + "/" + maxPage))
                            .getItem());
                    if (maxPage > curPage) {
                        gui.setItem(50, new ItemBuilder(Material.REDSTONE)
                                .setDisplayName(langM.get("Gui.NEXT_PAGE"))
                                .getItem());
                    }
                    if (curPage > 1) {
                        gui.setItem(48, new ItemBuilder(Material.GLOWSTONE_DUST)
                                .setDisplayName(langM.get("Gui.LAST_PAGE"))
                                .getItem());
                    }
                    player.closeInventory();
                    player.openInventory(gui);
                    guiState.put(player.getUniqueId(), "list");
                }
                break;
            case "action":
                // Action GUI
                List<Integer> functionSlots = Arrays.asList(29, 30, 31, 32, 33, 45);
                if (!functionSlots.contains(e.getSlot())) {
                    return;
                }
                Pattern idPattern = Pattern.compile("\\([^)]*\\)$");
                Matcher m = idPattern.matcher(guiTitle);
                String lbID = "";
                if (m.find()) {
                    lbID = m.group().replaceAll("\\(", "").replaceAll("\\)", "");
                }
                Lootbag lb = lbM.getLootbag(lbID);
                if (e.getSlot() == 29) {
                    // View Content
                    player.closeInventory();
                    Inventory gui = Bukkit.createInventory(null, lb.getSize(), langM.get("Gui.VIEW_TITLE") + " §8(" + lbID + ")");
                    lb.getContents().forEach(gui::setItem);
                    player.openInventory(gui);
                    guiState.put(player.getUniqueId(), "view_contents");
                } else if (e.getSlot() == 30) {
                    // Edit Content
                    player.closeInventory();
                    Inventory gui = Bukkit.createInventory(null, lb.getSize(), langM.get("Gui.EDIT_TITLE") + " §8(" + lbID + ")");
                    lb.getContents().forEach(gui::setItem);
                    player.openInventory(gui);
                    guiState.put(player.getUniqueId(), "edit_contents");
                } else if (e.getSlot() == 31) {
                    // Rename
                    player.closeInventory();
                    player.sendMessage(langM.getPrefixed("Message.RENAME_ENTER"));
                    guiState.put(player.getUniqueId(), "rename:" + lbID);
                } else if (e.getSlot() == 32) {
                    // Get one
                    if (player.getInventory().addItem(lb.getTriggerItem()).size() > 0) {
                        player.sendMessage(langM.getPrefixed("Message.BAG_FULL"));
                    } else {
                        player.sendMessage(langM.getPrefixed("Message.GET_ITEM")
                                .replaceAll("\\{name}", lb.getName())
                                .replaceAll("\\{id}", lbID));
                    }
                } else if (e.getSlot() == 33) {
                    // Delete
                    if (plugin.getConfig().getBoolean("delete_confirm")) {
                        player.sendMessage(langM.getPrefixed("Message.DELETE_CONFIRM")
                                .replaceAll("\\{msg}", "delete/" + lbID));
                    } else {
                        if (lbM.removeLootbag(lbID)) {
                            player.sendMessage(langM.getPrefixed("Message.DELETE_SUCCESSFUL")
                                    .replaceAll("\\{id}", lbID));
                        } else {
                            player.sendMessage(langM.getPrefixed("Message.DELETE_FAILED")
                                    .replaceAll("\\{id}", lbID));
                        }
                    }
                    player.closeInventory();
                    guiState.put(player.getUniqueId(), "delete:" + lbID);
                } else if (e.getSlot() == 45) {
                    player.closeInventory();
                    openLootbagList(player);
                }
                break;
        }
    }

    // Remove guiState on close inventory
    @EventHandler
    public void onCloseInventory(InventoryCloseEvent e) {
        if (guiState.containsKey(e.getPlayer().getUniqueId())) {
            if (guiState.get(e.getPlayer().getUniqueId()).equals("edit_contents")) {
                // Edit Content Finish
                Pattern idPattern = Pattern.compile("\\([^)]*\\)$");
                Matcher m = idPattern.matcher(e.getView().getTitle());
                String lbID = "";
                if (m.find()) {
                    lbID = m.group().replaceAll("\\(", "").replaceAll("\\)", "");
                }
                Lootbag lb = lbM.getLootbag(lbID);
                Inventory inv = e.getInventory();
                for (int i=0; i<inv.getSize(); i++) {
                    if (inv.getItem(i) != null) {
                        lb.setItem(i, inv.getItem(i));
                    } else {
                        lb.removeItem(i);
                    }
                }
                lbM.getIdLootbagMap().put(lbID, lb);
                lbM.getItemLootbagIDMap().put(lb.getTriggerItem(), lbID);
                lbM.saveLootbag(lb);
                e.getPlayer().sendMessage(langM.getPrefixed("Message.EDIT_DONE").replaceAll("\\{id}", lbID));
            }
            guiState.remove(e.getPlayer().getUniqueId());
        }
    }

    // Remove guiState & setupState on leaving server
    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        guiState.remove(e.getPlayer().getUniqueId());
        setupState.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler (priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        if (setupState.containsKey(player.getUniqueId())) {
            if (e.getMessage().equalsIgnoreCase("cancel")) {
                e.setCancelled(true);
                setupState.remove(player.getUniqueId());
                setupLootbag.remove(player.getUniqueId());
                player.sendMessage(langM.getPrefixed("Message.CREATE_CANCELLED"));
                return;
            }

            if (setupState.get(player.getUniqueId()).equals("id")) {
                e.setCancelled(true);
                String id = e.getMessage();

                if (lbM.idExists(id)) {
                    player.sendMessage(langM.getPrefixed("Message.ID_EXISTS_ALREADY"));
                    player.sendMessage(langM.getPrefixed("Message.ENTER_ID"));
                    return;
                }

                setupLootbag.get(player.getUniqueId()).setID(e.getMessage());
                player.sendMessage(langM.getPrefixed("Message.ENTER_NAME"));
                setupState.put(player.getUniqueId(), "name");
            } else if (setupState.get(player.getUniqueId()).equals("name")) {
                e.setCancelled(true);
                setupLootbag.get(player.getUniqueId()).setName(colorize(e.getMessage()));
                player.sendMessage(langM.getPrefixed("Message.SETUP_TRIGGER"));
                setupState.put(player.getUniqueId(), "trigger");
            }
        } else if (guiState.getOrDefault(player.getUniqueId(), "none").startsWith("rename:")) {
            e.setCancelled(true);
            Pattern idPattern = Pattern.compile(":.*$");
            Matcher m = idPattern.matcher(guiState.get(player.getUniqueId()));
            String lbID = "";
            if (m.find()) {
                lbID = m.group().replaceAll(":", "");
            }
            lbM.renameLootbag(lbID, colorize(e.getMessage()));
            player.sendMessage(langM.getPrefixed("Message.RENAME_SUCCESSFUL")
                    .replaceAll("\\{name}", colorize(e.getMessage())));
            guiState.remove(player.getUniqueId());
        } else if (guiState.getOrDefault(player.getUniqueId(), "none").startsWith("delete:")) {
            e.setCancelled(true);
            if (e.getMessage().equalsIgnoreCase("cancel")) {
                guiState.remove(player.getUniqueId());
                return;
            }
            Pattern idPattern = Pattern.compile(":.*$");
            Matcher m = idPattern.matcher(guiState.get(player.getUniqueId()));
            String lbID = "";
            if (m.find()) {
                lbID = m.group().replaceAll(":", "");
            }

            if (!e.getMessage().equals("delete/" + lbID)) {
                player.sendMessage(langM.getPrefixed("Message.DELETE_CONFIRM_FAILED"));
                return;
            }

            if (lbM.removeLootbag(lbID)) {
                player.sendMessage(langM.getPrefixed("Message.DELETE_SUCCESSFUL")
                        .replaceAll("\\{id}", lbID));
            } else {
                player.sendMessage(langM.getPrefixed("Message.DELETE_FAILED")
                        .replaceAll("\\{id}", lbID));
            }
            guiState.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (setupState.containsKey(e.getPlayer().getUniqueId())) {
            Player player = e.getPlayer();
            if (setupState.get(player.getUniqueId()).equals("trigger")) {
                e.setCancelled(true);
                ItemStack triggerItem = e.getItemDrop().getItemStack();
                triggerItem.setAmount(1);

                if (lbM.triggerItemExists(triggerItem)) {
                    player.sendMessage(langM.getPrefixed("Message.TRIGGER_EXISTS_ALREADY"));
                    player.sendMessage(langM.getPrefixed("Message.SETUP_TRIGGER"));
                    return;
                }

                setupLootbag.get(player.getUniqueId()).setTriggerItem(triggerItem);
                player.sendMessage(langM.getPrefixed("Message.RIGHT_CLICK_CHEST"));
                setupState.put(player.getUniqueId(), "contents");
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (setupState.containsKey(e.getPlayer().getUniqueId())) {
            if (setupState.get(e.getPlayer().getUniqueId()).equals("contents")) {
                if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    assert e.getClickedBlock() != null;
                    if (e.getClickedBlock().getType() == Material.CHEST) {
                        Player player = e.getPlayer();
                        e.setCancelled(true);
                        Chest chest = (Chest) e.getClickedBlock().getState();
                        Inventory inv = chest.getInventory();
                        Lootbag lootbag = setupLootbag.get(player.getUniqueId());
                        lootbag.setSize(inv.getSize());
                        for (int i=0; i<inv.getSize(); i++) {
                            if (inv.getItem(i) != null) {
                                lootbag.setItem(i, inv.getItem(i));
                            }
                        }
                        lbM.addLootbag(lootbag);
                        player.sendMessage(langM.getPrefixed("Message.CREATE_SUCCESSFUL")
                                .replaceAll("\\{name}", lootbag.getName())
                                .replaceAll("\\{id}", lootbag.getID()));
                        setupState.remove(player.getUniqueId());
                        setupLootbag.remove(player.getUniqueId());
                    }
                }
            }
        }

    }
}

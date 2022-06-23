package com.github.lhemon412.lootbag2.adminSetup;

import com.github.lhemon412.lootbag2.LanguageManager;
import com.github.lhemon412.lootbag2.Lootbag;
import com.github.lhemon412.lootbag2.Lootbag2Plugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainCommand implements CommandExecutor, TabCompleter {
    private final Lootbag2Plugin plugin;
    private final LanguageManager langM;

    public MainCommand(Lootbag2Plugin instance) {
        plugin = instance;
        langM = instance.getLangM();
    }

    @Override
    public List<String> onTabComplete(
            @NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String[] args) {
        final List<String>  ava = new ArrayList<>();
        if (args.length == 1) {
            ava.addAll(Arrays.asList("setup", "give", "reload"));
        } else if (args.length == 2) {
            if (args[0].equals("give")) {
                plugin.getServer().getOnlinePlayers().forEach(player -> ava.add(player.getName()));
            }
        } else if (args.length == 3) {
            if (args[0].equals("give")) {
                ava.addAll(plugin.getLbm().getIdLootbagMap().keySet());
            }
        }

        List<String> ret = new ArrayList<>();
        for (String tc : ava) {
            if (tc.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) ret.add(tc);
        }

        return ret;
    }

    @Override
    public boolean onCommand(
            @NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
        } else {
            switch(args[0]) {
                case "help":
                    showHelp(sender);
                    break;
                case "setup":
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(langM.getPrefixed("Message.CONSOLE_UNUSABLE"));
                        return false;
                    }
                    plugin.getGuiHandler().showGui((Player) sender);
                    break;
                case "give":
                    // args[0]      give
                    // args[1]      <player>
                    // args[2]      <id>
                    // args[3]      [amount]
                    if (args.length == 1) {
                        sender.sendMessage(langM.getPrefixed("Message.GIVE_ARGS_1"));
                        return false;
                    } else if (args.length == 2) {
                        sender.sendMessage(langM.getPrefixed("Message.GIVE_ARGS_2"));
                        return false;
                    }
                    Player target = plugin.getServer().getPlayerExact(args[1]);
                    if (target == null) {
                        sender.sendMessage(langM.getPrefixed("Message.GIVE_PLAYER_OFFLINE"));
                        return false;
                    }
                    Lootbag lb = plugin.getLbm().getLootbag(args[2]);
                    if (lb == null) {
                        sender.sendMessage(langM.getPrefixed("Message.GIVE_ID_NOT_EXIST"));
                        return false;
                    }

                    Integer amount = 1;
                    if (args.length == 4) {
                        amount = Integer.valueOf(args[3]);
                    }

                    ItemStack item = lb.getTriggerItem().clone();
                    item.setAmount(amount);

                    int space = 0;
                    for (ItemStack i : target.getInventory().getStorageContents()) {
                        if (i == null) space += 64;
                        else if (i.isSimilar(item)) space += (64 - i.getAmount());
                    }

                    if (space < amount) {
                        sender.sendMessage(langM.getPrefixed("Message.GIVE_TARGET_FULL"));
                    } else {
                        target.getInventory().addItem(item);
                        sender.sendMessage(langM.getPrefixed("Message.GIVE_SUCCESSFUL")
                                .replaceAll("\\{amount}", String.valueOf(amount))
                                .replaceAll("\\{name}", lb.getName())
                                .replaceAll("\\{target}", target.getName()));
                    }
                    break;
                case "reload":
                    plugin.reloadConfig();
                    plugin.getLangM().setLang(plugin.getConfig().getString("language"));
                    if (!plugin.getLangM().reload()) {
                        sender.sendMessage(langM.getPrefixed("Message.RELOAD_LANG_FAILED"));
                        return false;
                    }
                    if (!plugin.getLbm().reload()) {
                        sender.sendMessage(langM.getPrefixed("Message.RELOAD_LOOTBAG_FAILED"));
                        return false;
                    }
                    sender.sendMessage(langM.getPrefixed("Message.RELOAD_SUCCESSFUL"));
                    break;
                default:
                    sender.sendMessage(langM.getPrefixed("Message.MISSING_CMD"));
            }
        }
        return false;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(langM.getPrefix() + "V" + plugin.getVersion());
        langM.getPrefixedList("Message.COMMAND_HELP").forEach(sender::sendMessage);
    }
}

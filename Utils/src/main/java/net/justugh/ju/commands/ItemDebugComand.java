package net.justugh.ju.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Subcommand;
import net.justugh.ju.message.Messages;
import net.justugh.ju.util.PersistentDataUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

@CommandAlias("itemdebug")
@CommandPermission("justutils.command.itemdebug")
public class ItemDebugComand extends BaseCommand {

    @Default
    public void onDefault(Player sender, @Default("1") int page) {
        List<String> data = new ArrayList<>();

        ItemStack item = sender.getInventory().getItemInMainHand();

        if (item.getType() == Material.AIR) {
            sender.sendMessage(Messages.error("Item Debug", "You must hold a valid item."));
            return;
        }

        data.add(Messages.dottedMessage("Material", item.getType().name()));
        data.add(Messages.dottedMessage("Amount", item.getAmount() + ""));

        ItemMeta meta = item.getItemMeta();

        if (meta.hasDisplayName()) {
            data.add(Messages.dottedMessage("Name", meta.getDisplayName()));
        }

        if (meta.hasLore()) {
            meta.getLore().forEach(s -> data.add(Messages.dottedMessage("Lore", s)));
        }

        if (!item.getEnchantments().isEmpty()) {
            item.getEnchantments().forEach((enchantment, integer) -> data.add(Messages.dottedMessage("Enchantment", enchantment.getName() + ", " + integer)));
        }

        if (!item.getItemFlags().isEmpty()) {
            item.getItemFlags().forEach(flag -> data.add(Messages.dottedMessage("Flag", flag.name())));
        }

        if (meta.hasCustomModelData()) {
            data.add(Messages.dottedMessage("Model Data", meta.getCustomModelData() + ""));
        }

        if (meta.hasAttributeModifiers()) {
            meta.getAttributeModifiers().forEach((attribute, attributeModifier) -> data.add(Messages.dottedMessage("Attribute", attribute.name() + ", " + attributeModifier.getName() + ", " + attributeModifier.getAmount())));
        }

        if (!meta.getPersistentDataContainer().getKeys().isEmpty()) {
            meta.getPersistentDataContainer().getKeys().forEach(key -> {
                PersistentDataUtil.getValues(meta.getPersistentDataContainer(), key).forEach(o -> {
                    data.add(Messages.dottedMessage("Persistent Data", key.getNamespace() + ", " + key.getKey() + ", " + o));
                });
            });
        }

        sender.sendMessage(Messages.getPagedList("Item Debug", data, page));
    }

    @Subcommand("hat")
    public void onHatCommand(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.AIR) {
            return;
        }

        player.getInventory().setItem(EquipmentSlot.HEAD, item);
    }

    @Subcommand("back")
    public void onBackCommand(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.AIR) {
            return;
        }

        player.getInventory().setItem(EquipmentSlot.CHEST, item);
    }

}

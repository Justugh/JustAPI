package net.justugh.japi.menu.action;

import org.bukkit.event.inventory.InventoryClickEvent;

public interface ClickTypeAction {

    void onClick(InventoryClickEvent event, String args);

}

package net.justugh.japi.menu.task;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.justugh.japi.JustAPIPlugin;
import net.justugh.japi.menu.Menu;
import net.justugh.japi.menu.placeholder.PlaceholderProvider;
import net.justugh.japi.util.Format;
import net.justugh.japi.util.InventoryUpdate;
import net.justugh.japi.util.Placeholder;
import net.justugh.japi.util.StringModifier;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class TitleUpdateTask {

    private final Menu menu;

    private int taskId;

    @Getter
    private boolean active;

    public void startTask() {
        active = true;

        taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(JustAPIPlugin.getInstance(), this::run, 5, 1).getTaskId();
    }

    public void cancelTask() {
        if (!active) {
            return;
        }

        Bukkit.getScheduler().cancelTask(taskId);
        active = false;
    }

    private void run() {
        List<StringModifier> modifiers = new ArrayList<>();
        modifiers.addAll(menu.getData().getModifiers());
        modifiers.addAll(menu.getData().getPlaceholderProviders().stream().map(PlaceholderProvider::asPlaceholder).collect(Collectors.toList()));

        Placeholder pagesPlaceholder = new Placeholder("%pages%", menu.getInventories().size() + "");
        modifiers.add(pagesPlaceholder);

        menu.getInventories().forEach(menuInventory -> {
            Inventory inventory = menuInventory.getInventory();

            List<StringModifier> finalModifiers = new ArrayList<>(modifiers);

            Placeholder pagePlaceholder = new Placeholder("%page%", (menu.getInventories().indexOf(menuInventory) + 1) + "");
            finalModifiers.add(pagePlaceholder);

            new ArrayList<>(inventory.getViewers()).forEach(viewer -> {
                InventoryUpdate.updateInventory(JustAPIPlugin.getInstance(), (Player) viewer, Format.format(menuInventory.getTitle(),
                        finalModifiers.toArray(new StringModifier[]{})));
            });
        });

        menu.getUserMenus().values().forEach(menuInventoryList -> {
            menuInventoryList.forEach(menuInventory -> {
                Inventory inventory = menuInventory.getInventory();

                List<StringModifier> finalModifiers = new ArrayList<>(modifiers);

                Placeholder pagePlaceholder = new Placeholder("%page%", (menu.getInventories().indexOf(menuInventory) + 1) + "");
                finalModifiers.add(pagePlaceholder);

                new ArrayList<>(inventory.getViewers()).forEach(viewer -> {
                    InventoryUpdate.updateInventory(JustAPIPlugin.getInstance(), (Player) viewer, Format.format(menuInventory.getTitle(),
                            finalModifiers.toArray(new StringModifier[]{})));
                });
            });
        });
    }

}

package win.baldzika.panes;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.Inventory;
import org.jspecify.annotations.Nullable;

final class MenuListener implements Listener {

    // cancel first so no other plugin can move items out of a menu
    @EventHandler(priority = EventPriority.LOWEST)
    void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        Session session = session(top);
        if (session == null) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() == top) {
            session.click(event.getSlot(), event.getClick());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    void onDrag(InventoryDragEvent event) {
        if (session(event.getView().getTopInventory()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    void onClose(InventoryCloseEvent event) {
        Session session = session(event.getInventory());
        if (session != null) {
            session.closed();
        }
    }

    @EventHandler
    void onDisable(PluginDisableEvent event) {
        if (event.getPlugin() != Panes.plugin()) {
            return;
        }
        for (Player player : event.getPlugin().getServer().getOnlinePlayers()) {
            Session session = session(player.getOpenInventory().getTopInventory());
            if (session == null) {
                continue;
            }
            if (Bukkit.isOwnedByCurrentRegion(player)) {
                player.closeInventory();
            } else {
                session.closed();
            }
        }
    }

    private static @Nullable Session session(Inventory inventory) {
        return inventory.getHolder(false) instanceof Session session ? session : null;
    }
}

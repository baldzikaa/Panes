package win.baldzika.panes;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

public final class Panes {

    private static Plugin plugin;

    private Panes() {
    }

    /**
     * hooks panes into your plugin, call once in onEnable.
     */
    public static void init(Plugin owner) {
        Objects.requireNonNull(owner, "owner");
        if (plugin != null && plugin.isEnabled()) {
            throw new IllegalStateException("panes is already initialized by " + plugin.getName());
        }
        plugin = owner;
        Bukkit.getPluginManager().registerEvents(new MenuListener(), owner);
    }

    static Plugin plugin() {
        if (plugin == null) {
            throw new IllegalStateException("call Panes.init(plugin) before opening menus");
        }
        return plugin;
    }

    static boolean now(Player player, Runnable task) {
        if (Bukkit.isOwnedByCurrentRegion(player)) {
            task.run();
            return true;
        }
        return later(player, task);
    }

    // next tick on the player's thread, needed when swapping inventories inside a click
    static boolean later(Player player, Runnable task) {
        return player.getScheduler().run(plugin(), scheduled -> task.run(), null) != null;
    }
}

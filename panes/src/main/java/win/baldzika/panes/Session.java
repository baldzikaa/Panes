package win.baldzika.panes;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * one open menu for one player. everything in here runs on that player's thread.
 */
public final class Session implements InventoryHolder {

    private final Menu menu;
    private final Player player;
    private final Inventory inventory;
    private final @Nullable Element[] drawn;
    private final Map<String, Object> data = new HashMap<>();
    private @Nullable ScheduledTask refreshTask;
    private int page;
    private int pages = 1;
    private long lastClick;
    private boolean open;

    Session(Menu menu, Player player) {
        this.menu = menu;
        this.player = player;
        this.drawn = new Element[menu.size()];
        this.inventory = menu.type() == InventoryType.CHEST
            ? Bukkit.createInventory(this, menu.size(), menu.title(player))
            : Bukkit.createInventory(this, menu.type(), menu.title(player));
    }

    public Menu menu() {
        return menu;
    }

    public Player player() {
        return player;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public boolean isOpen() {
        return open;
    }

    public int page() {
        return page;
    }

    public int pages() {
        return pages;
    }

    public boolean hasNext() {
        return page < pages - 1;
    }

    public boolean hasPrevious() {
        return page > 0;
    }

    public void page(int page) {
        Panes.now(player, () -> {
            this.page = page;
            draw();
        });
    }

    public void next() {
        Panes.now(player, () -> {
            if (hasNext()) {
                page++;
                draw();
            }
        });
    }

    public void previous() {
        Panes.now(player, () -> {
            if (hasPrevious()) {
                page--;
                draw();
            }
        });
    }

    /**
     * redraws every slot. safe to call from any thread.
     */
    public void refresh() {
        Panes.now(player, this::draw);
    }

    public void close() {
        Panes.later(player, () -> {
            if (open) {
                player.closeInventory();
            }
        });
    }

    /**
     * swaps this menu for another one on the next tick.
     */
    public void open(Menu other) {
        Panes.later(player, () -> other.open(player));
    }

    @SuppressWarnings("unchecked")
    public <T> @Nullable T get(String key) {
        return (T) data.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, T fallback) {
        return (T) data.getOrDefault(key, fallback);
    }

    public void set(String key, @Nullable Object value) {
        if (value == null) {
            data.remove(key);
        } else {
            data.put(key, value);
        }
    }

    void show() {
        draw();
        player.openInventory(inventory);
        open = true;
        long ticks = menu.refreshTicks();
        if (ticks > 0) {
            refreshTask = player.getScheduler().runAtFixedRate(Panes.plugin(), task -> draw(), null, ticks, ticks);
        }
        menu.opened(this);
    }

    void closed() {
        if (!open) {
            return;
        }
        open = false;
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
        menu.closed(this);
    }

    void click(int slot, ClickType type) {
        if (slot < 0 || slot >= drawn.length) {
            return;
        }
        Element element = drawn[slot];
        if (element == null) {
            return;
        }
        long now = System.nanoTime();
        if (lastClick != 0 && now - lastClick < menu.clickCooldownNanos()) {
            return;
        }
        lastClick = now;
        element.click(new Click(this, slot, type));
    }

    private void draw() {
        int[] slots = menu.contentSlots();
        List<? extends Element> content = slots.length == 0 ? List.of() : menu.content(this);
        pages = Math.max(1, (content.size() + slots.length - 1) / Math.max(1, slots.length));
        page = Math.clamp(page, 0, pages - 1);

        for (int slot = 0; slot < drawn.length; slot++) {
            drawn[slot] = menu.element(slot);
        }
        int offset = page * slots.length;
        for (int i = 0; i < slots.length; i++) {
            int index = offset + i;
            if (index < content.size()) {
                drawn[slots[i]] = content.get(index);
            }
        }
        for (int slot = 0; slot < drawn.length; slot++) {
            Element element = drawn[slot];
            inventory.setItem(slot, element == null ? null : element.render(this));
        }
    }
}

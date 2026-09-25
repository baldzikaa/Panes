package win.baldzika.panes;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * a reusable menu template. every viewer gets their own {@link Session} so nothing is shared between regions.
 */
public final class Menu {

    private final InventoryType type;
    private final int size;
    private final Function<Player, Component> title;
    private final @Nullable Element[] elements;
    private final int[] contentSlots;
    private final Function<Session, List<? extends Element>> content;
    private final long refreshTicks;
    private final long clickCooldownNanos;
    private final Consumer<Session> onOpen;
    private final Consumer<Session> onClose;

    private Menu(Builder builder) {
        this.type = builder.type;
        this.size = builder.size;
        this.title = builder.title;
        this.refreshTicks = builder.refreshTicks;
        this.clickCooldownNanos = builder.clickCooldown.toNanos();
        this.onOpen = builder.onOpen;
        this.onClose = builder.onClose;
        this.content = builder.content;

        this.elements = new Element[size];
        int[] content = new int[size];
        int contentCount = 0;
        char[] layout = builder.layout;
        for (int slot = 0; slot < size; slot++) {
            if (layout != null) {
                char key = layout[slot];
                if (key == builder.contentKey) {
                    content[contentCount++] = slot;
                } else if (key != '.' && key != '_') {
                    Element element = builder.keys.get(key);
                    if (element == null) {
                        throw new IllegalArgumentException("layout uses '" + key + "' but nothing is bound to it");
                    }
                    elements[slot] = element;
                }
            }
            Element override = builder.slots.get(slot);
            if (override != null) {
                elements[slot] = override;
            }
        }
        this.contentSlots = Arrays.copyOf(content, contentCount);
        if (builder.content != null && contentCount == 0) {
            throw new IllegalArgumentException("content was set but the layout has no '" + builder.contentKey + "' slots");
        }
    }

    public static Builder chest(int rows) {
        if (rows < 1 || rows > 6) {
            throw new IllegalArgumentException("rows must be between 1 and 6");
        }
        return new Builder(InventoryType.CHEST, rows * 9);
    }

    public static Builder of(InventoryType type) {
        if (type == InventoryType.CHEST) {
            return chest(3);
        }
        return new Builder(type, type.getDefaultSize());
    }

    /**
     * opens the menu for the player. safe to call from any thread.
     */
    public CompletableFuture<Session> open(Player player) {
        CompletableFuture<Session> future = new CompletableFuture<>();
        boolean scheduled = Panes.now(player, () -> {
            try {
                Session session = new Session(this, player);
                session.show();
                future.complete(session);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        if (!scheduled) {
            future.completeExceptionally(new IllegalStateException(player.getName() + " is no longer online"));
        }
        return future;
    }

    InventoryType type() {
        return type;
    }

    int size() {
        return size;
    }

    Component title(Player player) {
        return title.apply(player);
    }

    @Nullable Element element(int slot) {
        return elements[slot];
    }

    int[] contentSlots() {
        return contentSlots;
    }

    List<? extends Element> content(Session session) {
        return content == null ? List.of() : content.apply(session);
    }

    long refreshTicks() {
        return refreshTicks;
    }

    long clickCooldownNanos() {
        return clickCooldownNanos;
    }

    void opened(Session session) {
        onOpen.accept(session);
    }

    void closed(Session session) {
        onClose.accept(session);
    }

    public static final class Builder {

        private final InventoryType type;
        private final int size;
        private Function<Player, Component> title;
        private char @Nullable [] layout;
        private final Map<Character, Element> keys = new HashMap<>();
        private final Map<Integer, Element> slots = new HashMap<>();
        private char contentKey;
        private @Nullable Function<Session, List<? extends Element>> content;
        private long refreshTicks;
        private Duration clickCooldown = Duration.ofMillis(100);
        private Consumer<Session> onOpen = session -> {
        };
        private Consumer<Session> onClose = session -> {
        };

        private Builder(InventoryType type, int size) {
            this.type = type;
            this.size = size;
            this.title = player -> type.defaultTitle();
            Layout.width(type);
        }

        public Builder title(Component title) {
            Objects.requireNonNull(title, "title");
            this.title = player -> title;
            return this;
        }

        public Builder title(Function<Player, Component> title) {
            this.title = Objects.requireNonNull(title, "title");
            return this;
        }

        /**
         * one string per row, one char per slot. spaces are ignored, '.' and '_' mean empty.
         */
        public Builder layout(String... rows) {
            this.layout = Layout.parse(rows, Layout.width(type), size);
            return this;
        }

        public Builder bind(char key, Element element) {
            keys.put(key, Objects.requireNonNull(element, "element"));
            return this;
        }

        public Builder slot(int slot, Element element) {
            if (slot < 0 || slot >= size) {
                throw new IndexOutOfBoundsException("slot " + slot + " is outside a menu of size " + size);
            }
            slots.put(slot, Objects.requireNonNull(element, "element"));
            return this;
        }

        /**
         * fills every slot marked with the key, split into pages. the function runs on each redraw.
         */
        public Builder content(char key, Function<Session, List<? extends Element>> content) {
            this.contentKey = key;
            this.content = Objects.requireNonNull(content, "content");
            return this;
        }

        /**
         * redraws the menu every n ticks while it is open.
         */
        public Builder refreshEvery(long ticks) {
            if (ticks < 0) {
                throw new IllegalArgumentException("ticks can't be negative");
            }
            this.refreshTicks = ticks;
            return this;
        }

        public Builder clickCooldown(Duration cooldown) {
            this.clickCooldown = Objects.requireNonNull(cooldown, "cooldown");
            return this;
        }

        public Builder onOpen(Consumer<Session> onOpen) {
            this.onOpen = Objects.requireNonNull(onOpen, "onOpen");
            return this;
        }

        public Builder onClose(Consumer<Session> onClose) {
            this.onClose = Objects.requireNonNull(onClose, "onClose");
            return this;
        }

        public Menu build() {
            return new Menu(this);
        }
    }
}

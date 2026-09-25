package win.baldzika.panes;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuTest {

    private ServerMock server;
    private PlayerMock player;
    private ItemStack glass;
    private ItemStack arrow;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        glass = ItemStack.of(Material.GRAY_STAINED_GLASS_PANE);
        arrow = ItemStack.of(Material.ARROW);
        Panes.init(MockBukkit.createMockPlugin());
        player = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private InventoryClickEvent click(PlayerMock who, int rawSlot, ClickType type) {
        InventoryView view = who.getOpenInventory();
        InventoryClickEvent event = new InventoryClickEvent(view, view.getSlotType(rawSlot), rawSlot, type, InventoryAction.PICKUP_ALL);
        server.getPluginManager().callEvent(event);
        return event;
    }

    @Test
    void opensWithLayout() {
        Menu menu = Menu.chest(3)
            .layout(
                "#########",
                "#...a...#",
                "#########")
            .bind('#', Button.of(glass))
            .bind('a', Button.of(arrow))
            .build();

        Session session = menu.open(player).join();

        Inventory top = player.getOpenInventory().getTopInventory();
        assertInstanceOf(Session.class, top.getHolder(false));
        assertTrue(session.isOpen());
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, top.getItem(0).getType());
        assertEquals(Material.ARROW, top.getItem(13).getType());
        assertNull(top.getItem(10));
    }

    @Test
    void clickRunsButtonAndIsCancelled() {
        AtomicInteger clicks = new AtomicInteger();
        Menu menu = Menu.chest(1)
            .slot(4, Button.of(arrow, click -> clicks.incrementAndGet()))
            .build();
        menu.open(player).join();

        InventoryClickEvent event = click(player, 4, ClickType.LEFT);

        assertTrue(event.isCancelled());
        assertEquals(1, clicks.get());
    }

    @Test
    void clicksInPlayerInventoryAreCancelledButIgnored() {
        AtomicInteger clicks = new AtomicInteger();
        Menu menu = Menu.chest(1)
            .slot(4, Button.of(arrow, click -> clicks.incrementAndGet()))
            .build();
        menu.open(player).join();
        InventoryView view = player.getOpenInventory();

        InventoryClickEvent event = click(player, view.getTopInventory().getSize() + 4, ClickType.SHIFT_LEFT);

        assertTrue(event.isCancelled());
        assertEquals(0, clicks.get());
    }

    @Test
    void cooldownDropsSpamClicks() {
        AtomicInteger clicks = new AtomicInteger();
        Menu menu = Menu.chest(1)
            .clickCooldown(Duration.ofSeconds(10))
            .slot(0, Button.of(arrow, click -> clicks.incrementAndGet()))
            .build();
        menu.open(player).join();

        for (int i = 0; i < 5; i++) {
            click(player, 0, ClickType.LEFT);
        }

        assertEquals(1, clicks.get());
    }

    @Test
    void pagesThroughContent() {
        List<Element> items = new ArrayList<>();
        for (int i = 1; i <= 16; i++) {
            items.add(Button.of(ItemStack.of(Material.DIAMOND, i)));
        }
        Menu menu = Menu.chest(3)
            .layout(
                "xxxxxxxxx",
                "xxxxxxxxx",
                "<.......>")
            .content('x', session -> items)
            .bind('<', Button.previousPage(arrow))
            .bind('>', Button.nextPage(arrow))
            .clickCooldown(Duration.ZERO)
            .build();

        Session session = menu.open(player).join();
        Inventory top = session.getInventory();

        assertEquals(1, session.pages());
        assertNull(top.getItem(18));
        assertNull(top.getItem(26));
        assertEquals(16, top.getItem(15).getAmount());
        assertNull(top.getItem(16));
    }

    @Test
    void nextAndPreviousButtonsMovePages() {
        List<Element> items = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            items.add(Button.of(ItemStack.of(Material.DIAMOND, i)));
        }
        Menu menu = Menu.chest(2)
            .layout(
                "xxxxxxxxx",
                "<.......>")
            .content('x', session -> items)
            .bind('<', Button.previousPage(arrow))
            .bind('>', Button.nextPage(arrow))
            .clickCooldown(Duration.ZERO)
            .build();
        Session session = menu.open(player).join();
        Inventory top = session.getInventory();

        assertEquals(3, session.pages());
        assertNull(top.getItem(9));
        assertEquals(Material.ARROW, top.getItem(17).getType());

        click(player, 17, ClickType.LEFT);
        click(player, 17, ClickType.LEFT);

        assertEquals(2, session.page());
        assertEquals(19, top.getItem(0).getAmount());
        assertEquals(20, top.getItem(1).getAmount());
        assertNull(top.getItem(2));
        assertNull(top.getItem(17));

        click(player, 9, ClickType.LEFT);
        assertEquals(1, session.page());
        assertEquals(10, top.getItem(0).getAmount());
    }

    @Test
    void pageClampsWhenContentShrinks() {
        List<Element> items = new ArrayList<>();
        for (int i = 0; i < 18; i++) {
            items.add(Button.of(glass));
        }
        Menu menu = Menu.chest(1)
            .layout("xxxxxxxxx")
            .content('x', session -> items)
            .build();
        Session session = menu.open(player).join();
        session.page(1);
        assertEquals(1, session.page());

        items.subList(9, 18).clear();
        session.refresh();

        assertEquals(0, session.page());
        assertEquals(1, session.pages());
    }

    @Test
    void refreshesOnTimer() {
        AtomicInteger ticks = new AtomicInteger();
        Menu menu = Menu.chest(1)
            .refreshEvery(5)
            .slot(0, Button.dynamic(session -> ItemStack.of(Material.CLOCK, ticks.incrementAndGet())))
            .build();
        Session session = menu.open(player).join();
        assertEquals(1, session.getInventory().getItem(0).getAmount());

        server.getScheduler().performTicks(10);

        assertEquals(3, session.getInventory().getItem(0).getAmount());
    }

    @Test
    void closeStopsRefreshAndRunsCallback() {
        AtomicInteger draws = new AtomicInteger();
        AtomicInteger closed = new AtomicInteger();
        Menu menu = Menu.chest(1)
            .refreshEvery(1)
            .slot(0, Button.dynamic(session -> {
                draws.incrementAndGet();
                return glass;
            }))
            .onClose(session -> closed.incrementAndGet())
            .build();
        Session session = menu.open(player).join();

        session.close();
        server.getScheduler().performTicks(1);
        int drawsAtClose = draws.get();
        server.getScheduler().performTicks(10);

        assertFalse(session.isOpen());
        assertEquals(1, closed.get());
        assertEquals(drawsAtClose, draws.get());
        assertEquals(InventoryType.CRAFTING, player.getOpenInventory().getType());
    }

    @Test
    void openingAnotherMenuFromAClick() {
        Menu second = Menu.of(InventoryType.HOPPER).slot(2, Button.of(arrow)).build();
        Menu first = Menu.chest(1)
            .slot(0, Button.of(glass, click -> click.session().open(second)))
            .build();
        Session session = first.open(player).join();

        click(player, 0, ClickType.LEFT);
        server.getScheduler().performTicks(1);

        assertFalse(session.isOpen());
        Inventory top = player.getOpenInventory().getTopInventory();
        assertEquals(InventoryType.HOPPER, top.getType());
        assertEquals(Material.ARROW, top.getItem(2).getType());
    }

    @Test
    void sessionDataIsPerViewer() {
        Menu menu = Menu.chest(1)
            .clickCooldown(Duration.ZERO)
            .slot(0, Button.dynamic(
                session -> ItemStack.of(Material.EMERALD, session.get("count", 1)),
                click -> {
                    click.session().set("count", click.session().get("count", 1) + 1);
                    click.session().refresh();
                }))
            .build();
        PlayerMock other = server.addPlayer();
        Session mine = menu.open(player).join();
        Session theirs = menu.open(other).join();

        click(player, 0, ClickType.LEFT);
        click(player, 0, ClickType.LEFT);

        assertEquals(3, mine.getInventory().getItem(0).getAmount());
        assertEquals(1, theirs.getInventory().getItem(0).getAmount());
    }

    @Test
    void unboundLayoutKeyFailsFast() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
            () -> Menu.chest(1).layout("####z####").bind('#', Button.of(glass)).build());
        assertEquals("layout uses 'z' but nothing is bound to it", error.getMessage());
    }

    @Test
    void contentWithoutSlotsFailsFast() {
        assertThrows(IllegalArgumentException.class,
            () -> Menu.chest(1).layout(".........").content('x', session -> List.of()).build());
    }

    @Test
    void titleCanDependOnViewer() {
        Menu menu = Menu.chest(1)
            .title(viewer -> Component.text(viewer.getName() + "'s stash"))
            .build();
        menu.open(player).join();

        assertEquals(Component.text(player.getName() + "'s stash"), player.getOpenInventory().title());
    }

    @Test
    void openFromAnotherThreadWaitsForPlayerThread() throws Exception {
        Menu menu = Menu.chest(1).slot(0, Button.of(arrow)).build();

        CompletableFuture<Session> opened = CompletableFuture.supplyAsync(() -> menu.open(player)).get().toCompletableFuture();
        assertFalse(opened.isDone());
        assertEquals(InventoryType.CRAFTING, player.getOpenInventory().getType());

        server.getScheduler().performTicks(1);

        assertTrue(opened.isDone());
        assertInstanceOf(Session.class, player.getOpenInventory().getTopInventory().getHolder(false));
    }

    @Test
    void emptyContentSlotsFallBackToTheirBinding() {
        List<Element> items = List.of(Button.of(arrow), Button.of(arrow));
        Menu menu = Menu.chest(1)
            .layout("xxxxxxxxx")
            .bind('x', Button.of(glass))
            .content('x', session -> items)
            .build();

        Session session = menu.open(player).join();

        assertEquals(Material.ARROW, session.getInventory().getItem(1).getType());
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, session.getInventory().getItem(2).getType());
    }
}

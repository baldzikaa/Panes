package win.baldzika.panes.example;

import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import win.baldzika.panes.Button;
import win.baldzika.panes.Element;
import win.baldzika.panes.Menu;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static win.baldzika.panes.example.Icons.icon;
import static win.baldzika.panes.example.Icons.text;

final class DemoMenus {

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Menu hub;
    private final Menu shop;

    DemoMenus() {
        Button border = Button.of(icon(Material.BLACK_STAINED_GLASS_PANE, " "));
        Button back = Button.of(icon(Material.ARROW, "<gray>Back"), click -> click.session().open(hub()));

        List<Element> products = Registry.ITEM.stream()
            .filter(type -> {
                String key = type.getKey().getKey();
                return key.endsWith("_wool") || key.endsWith("_concrete") || key.endsWith("_terracotta");
            })
            .map(DemoMenus::product)
            .toList();

        this.shop = Menu.chest(6)
            .title(text("<dark_gray>Block shop"))
            .layout(
                "#########",
                "#xxxxxxx#",
                "#xxxxxxx#",
                "#xxxxxxx#",
                "#########",
                "b..<p>...")
            .bind('#', border)
            .bind('x', border)
            .content('x', session -> products)
            .bind('b', back)
            .bind('<', Button.previousPage(icon(Material.SPECTRAL_ARROW, "<yellow>Previous page")))
            .bind('>', Button.nextPage(icon(Material.SPECTRAL_ARROW, "<yellow>Next page")))
            .bind('p', Button.dynamic(session -> icon(Material.PAPER,
                "<white>Page " + (session.page() + 1) + " of " + session.pages())))
            .build();

        this.hub = Menu.chest(3)
            .title(text("<dark_gray>Panes demo"))
            .layout(
                "#########",
                "#.s.t.c.#",
                "#######x#")
            .bind('#', border)
            .bind('s', Button.of(
                icon(Material.EMERALD, "<green>Shop", "<gray>a paged menu built from the item registry"),
                click -> click.session().open(this.shop)))
            .bind('t', Button.dynamic(
                session -> session.get("on", false)
                    ? icon(Material.LIME_DYE, "<green>Toggle: on", "<gray>state lives in your session")
                    : icon(Material.GRAY_DYE, "<red>Toggle: off", "<gray>state lives in your session"),
                click -> {
                    click.session().set("on", !click.session().get("on", false));
                    click.session().refresh();
                    click.player().playSound(click.player(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                }))
            .bind('c', Button.dynamic(session -> icon(Material.CLOCK,
                "<aqua>" + LocalTime.now().format(CLOCK),
                "<gray>redrawn every second on your own thread",
                "<gray>your ping: <white>" + session.player().getPing() + "ms")))
            .bind('x', Button.close(icon(Material.BARRIER, "<red>Close")))
            .refreshEvery(20)
            .build();
    }

    Menu hub() {
        return hub;
    }

    private static Element product(ItemType type) {
        ItemStack stack = type.createItemStack();
        stack.editMeta(meta -> meta.lore(List.of(text("<gray>left click to buy 1"), text("<gray>shift click to buy 16"))));
        return Button.of(stack, click -> {
            int amount = click.shift() ? 16 : 1;
            click.player().getInventory().addItem(type.createItemStack(amount));
            click.player().sendActionBar(text("<green>+" + amount + " " + type.getKey().getKey()));
        });
    }
}

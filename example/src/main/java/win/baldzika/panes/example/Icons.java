package win.baldzika.panes.example;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

final class Icons {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private Icons() {
    }

    static ItemStack icon(Material material, String name, String... lore) {
        ItemStack item = ItemStack.of(material);
        item.editMeta(meta -> {
            meta.displayName(text(name));
            meta.lore(Arrays.stream(lore).map(Icons::text).toList());
        });
        return item;
    }

    static Component text(String mini) {
        return MINI.deserialize(mini).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }
}

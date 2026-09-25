package win.baldzika.panes;

import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * anything that can sit in a menu slot.
 */
public interface Element {

    /**
     * called on the viewer's thread every time the slot is drawn, null leaves it empty.
     */
    @Nullable ItemStack render(Session session);

    default void click(Click click) {
    }
}

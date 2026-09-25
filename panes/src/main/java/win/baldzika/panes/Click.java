package win.baldzika.panes;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

public record Click(Session session, int slot, ClickType type) {

    public Player player() {
        return session.player();
    }

    public boolean left() {
        return type.isLeftClick();
    }

    public boolean right() {
        return type.isRightClick();
    }

    public boolean shift() {
        return type.isShiftClick();
    }
}

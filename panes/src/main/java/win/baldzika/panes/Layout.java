package win.baldzika.panes;

import org.bukkit.event.inventory.InventoryType;

final class Layout {

    private Layout() {
    }

    static int width(InventoryType type) {
        return switch (type) {
            case CHEST -> 9;
            case HOPPER -> 5;
            case DISPENSER, DROPPER, CRAFTER -> 3;
            default -> throw new IllegalArgumentException("unsupported inventory type " + type);
        };
    }

    /**
     * turns the row strings into one char per slot, spaces are dropped.
     */
    static char[] parse(String[] rows, int width, int size) {
        if (rows.length * width != size) {
            throw new IllegalArgumentException("layout has " + rows.length + " rows but the menu needs " + size / width);
        }
        char[] slots = new char[size];
        for (int row = 0; row < rows.length; row++) {
            String line = rows[row].replace(" ", "");
            if (line.length() != width) {
                throw new IllegalArgumentException("layout row " + (row + 1) + " must be " + width + " chars, got \"" + rows[row] + "\"");
            }
            line.getChars(0, width, slots, row * width);
        }
        return slots;
    }
}

package win.baldzika.panes;

import org.bukkit.event.inventory.InventoryType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LayoutTest {

    @Test
    void parsesRowsAndIgnoresSpaces() {
        char[] slots = Layout.parse(new String[]{"# # # # # # # # #", "#.......#"}, 9, 18);
        assertEquals('#', slots[0]);
        assertEquals('.', slots[10]);
        assertEquals('#', slots[17]);
    }

    @Test
    void rejectsWrongRowCount() {
        assertThrows(IllegalArgumentException.class, () -> Layout.parse(new String[]{"#########"}, 9, 27));
    }

    @Test
    void rejectsWrongRowWidth() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
            () -> Layout.parse(new String[]{"#####"}, 9, 9));
        assertEquals("layout row 1 must be 9 chars, got \"#####\"", error.getMessage());
    }

    @Test
    void knowsSmallInventoryWidths() {
        assertEquals(5, Layout.width(InventoryType.HOPPER));
        assertEquals(3, Layout.width(InventoryType.DISPENSER));
        assertThrows(IllegalArgumentException.class, () -> Layout.width(InventoryType.ANVIL));
    }

    @Test
    void hopperLayout() {
        assertArrayEquals("ab.cd".toCharArray(), Layout.parse(new String[]{"a b . c d"}, 5, 5));
    }
}

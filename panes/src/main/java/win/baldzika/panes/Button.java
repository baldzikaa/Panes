package win.baldzika.panes;

import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public final class Button implements Element {

    private static final Consumer<Click> NOTHING = click -> {
    };

    private final Function<Session, @Nullable ItemStack> icon;
    private final Consumer<Click> action;

    private Button(Function<Session, @Nullable ItemStack> icon, Consumer<Click> action) {
        this.icon = Objects.requireNonNull(icon, "icon");
        this.action = Objects.requireNonNull(action, "action");
    }

    public static Button of(ItemStack icon) {
        return of(icon, NOTHING);
    }

    public static Button of(ItemStack icon, Consumer<Click> action) {
        Objects.requireNonNull(icon, "icon");
        return new Button(session -> icon, action);
    }

    public static Button dynamic(Function<Session, @Nullable ItemStack> icon) {
        return new Button(icon, NOTHING);
    }

    public static Button dynamic(Function<Session, @Nullable ItemStack> icon, Consumer<Click> action) {
        return new Button(icon, action);
    }

    /**
     * only shows while there is a next page.
     */
    public static Button nextPage(ItemStack icon) {
        return new Button(session -> session.hasNext() ? icon : null, click -> click.session().next());
    }

    /**
     * only shows while there is a previous page.
     */
    public static Button previousPage(ItemStack icon) {
        return new Button(session -> session.hasPrevious() ? icon : null, click -> click.session().previous());
    }

    public static Button close(ItemStack icon) {
        return new Button(session -> icon, click -> click.session().close());
    }

    @Override
    public @Nullable ItemStack render(Session session) {
        return icon.apply(session);
    }

    @Override
    public void click(Click click) {
        action.accept(click);
    }
}

package win.baldzika.panes.example;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import win.baldzika.panes.Panes;

public final class ExamplePlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        Panes.init(this);
        DemoMenus menus = new DemoMenus();

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> event.registrar().register(
            Commands.literal("panes")
                .requires(source -> source.getExecutor() instanceof Player)
                .executes(context -> {
                    menus.hub().open((Player) context.getSource().getExecutor());
                    return 1;
                })
                .build(),
            "open the panes demo"));
    }
}

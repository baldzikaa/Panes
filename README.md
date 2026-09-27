# Panes

Inventory menus for Paper and Folia.

On Folia there is no main thread. Every player is ticked by whichever region they are standing in, so a menu that several players share is touched by several threads at once. That is where most GUI dupes and crashes on Folia come from.

Panes avoids the problem instead of locking around it: a `Menu` is only a template, and every viewer gets their own `Session` with their own inventory. All drawing and click handling for a session happens on that player's thread. Calls from anywhere else are handed to the player's scheduler.

It runs the same on regular Paper.

## Features

- Layouts drawn as text, one char per slot
- Paged content with next and previous buttons that hide themselves on the first and last page
- Dynamic buttons that redraw from session state
- Timed refresh (`refreshEvery(20)` redraws once a second)
- Per viewer state with `session.get` and `session.set`
- Click cooldown to stop macro spam, 100ms by default
- Chests of 1 to 6 rows, hoppers, dispensers, droppers and crafters
- Every click and drag inside a menu is cancelled at `LOWEST`, so items can't be pulled out or shift-clicked in

## Example

```java
public final class MyPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        Panes.init(this);
    }
}
```

```java
Menu menu = Menu.chest(3)
    .title(Component.text("Warps"))
    .layout(
        "#########",
        "#.a.b.c.#",
        "#########")
    .bind('#', Button.of(ItemStack.of(Material.BLACK_STAINED_GLASS_PANE)))
    .bind('a', Button.of(spawnIcon, click -> click.player().teleportAsync(spawn)))
    .bind('b', Button.of(shopIcon, click -> click.session().open(shopMenu)))
    .bind('c', Button.close(ItemStack.of(Material.BARRIER)))
    .build();

menu.open(player);
```

`open` works from any thread and returns a `CompletableFuture<Session>` that completes once the menu is showing. It fails instead if another plugin cancels the open, so your `onOpen` code never runs for a menu the player can't see.

### Pages

Mark the slots that hold content with a char, then give Panes the list. The function runs on every redraw, so it can read live data.

```java
Menu shop = Menu.chest(6)
    .layout(
        "#########",
        "#xxxxxxx#",
        "#xxxxxxx#",
        "#xxxxxxx#",
        "#########",
        "...<p>...")
    .bind('#', border)
    .bind('x', border)
    .content('x', session -> products)
    .bind('<', Button.previousPage(prevIcon))
    .bind('>', Button.nextPage(nextIcon))
    .bind('p', Button.dynamic(session -> pageIcon(session.page() + 1, session.pages())))
    .build();
```

Slots with the content char that have nothing on the current page fall back to whatever is bound to that char, so the last page keeps its border.

### State and refresh

```java
Button.dynamic(
    session -> session.get("on", false) ? onIcon : offIcon,
    click -> {
        click.session().set("on", !click.session().get("on", false));
        click.session().refresh();
    });
```

`refresh()`, `page(int)`, `next()` and `previous()` can be called from any thread. `close()` and `open(Menu)` always wait for the next tick, since Bukkit doesn't like inventories being swapped in the middle of a click event.

## Install

Build and publish to your local Maven repository:

```
./gradlew publishToMavenLocal
```

Then shade it into your plugin and relocate it, so two plugins using different versions don't clash:

```kotlin
repositories {
    mavenLocal()
}

dependencies {
    implementation("win.baldzika:panes:1.0.0")
}

tasks.shadowJar {
    relocate("win.baldzika.panes", "your.plugin.libs.panes")
}
```

Needs Java 25 and Paper or Folia 26.1.2 or newer.

## Demo plugin

The `example` module is a small plugin with a hub, a paged block shop, a toggle and a live clock. Run `/panes` in game.

```
./gradlew :example:runServer    # paper
./gradlew :example:runFolia     # folia
```

## Building

```
./gradlew build
```

Runs the tests (MockBukkit) and builds both jars.

## License

MIT

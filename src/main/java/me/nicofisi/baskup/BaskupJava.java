package me.nicofisi.baskup;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class BaskupJava extends JavaPlugin implements Listener {
    private static BaskupJava instance;

    public BaskupJava() {
        if (instance == null) {
            instance = this;
        } else {
            throw new IllegalStateException("You cannot create another instance of the main class");
        }
    }

    public static BaskupJava get() {
        return instance;
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);

        Baskup.onEnable();
    }

    public void setEnabledPublic(boolean enabled) {
        super.setEnabled(enabled);
    }

    @Override
    public void onDisable() {
        Baskup.onDisable();
    }

    // Event Handlers
    //
    // Every method below needs to be annotated with @EventHandler and call Baskup.onEventName(event).
    // We need this to be in Java because Spigot uses Java's reflection to call events, and Scala
    // would require a different approach to reflection, which Spigot doesn't support
}

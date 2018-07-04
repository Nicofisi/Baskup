package me.nicofisi.baskup;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
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

    public static BaskupJava instance() {
        return instance;
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(Baskup$.MODULE$, this);

        Baskup.onEnable();
    }

    public void setEnabledPublic(boolean enabled) {
        super.setEnabled(enabled);
    }

    @Override
    public void onDisable() {
        Baskup.onDisable();
    }
}

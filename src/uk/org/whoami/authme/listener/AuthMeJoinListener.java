package uk.org.whoami.authme.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import uk.org.whoami.authme.AuthMe;
import uk.org.whoami.authme.datasource.DataSource;
import uk.org.whoami.authme.settings.Settings;

public class AuthMeJoinListener implements Listener {
	
    @SuppressWarnings("unused")
	private JavaPlugin plugin;
    private DataSource database;

    public AuthMeJoinListener(AuthMe plugin) {
    	this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {

        final Player player = event.getPlayer();
        String name = player.getName();
        
        if (!Settings.playersWhiteList.contains(name) && database == null) {
        	event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "You can't join this server for the moment");
        	return;
        }
    }
}

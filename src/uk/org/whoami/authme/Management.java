package uk.org.whoami.authme;

import java.security.NoSuchAlgorithmException;
import java.util.Date;


import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import uk.org.whoami.authme.api.API;
import uk.org.whoami.authme.cache.auth.PlayerAuth;
import uk.org.whoami.authme.cache.auth.PlayerCache;
import uk.org.whoami.authme.cache.backup.FileCache;
import uk.org.whoami.authme.cache.limbo.LimboCache;
import uk.org.whoami.authme.cache.limbo.LimboPlayer;
import uk.org.whoami.authme.datasource.DataSource;
import uk.org.whoami.authme.events.RestoreInventoryEvent;
import uk.org.whoami.authme.listener.AuthMePlayerListener;
import uk.org.whoami.authme.security.PasswordSecurity;
import uk.org.whoami.authme.settings.Messages;
import uk.org.whoami.authme.settings.PlayersLogs;
import uk.org.whoami.authme.settings.Settings;

public class Management {

    private Messages m = Messages.getInstance();
    private PlayersLogs pllog = PlayersLogs.getInstance();
    private Utils utils = Utils.getInstance();
    private FileCache playerCache = new FileCache();
    private DataSource database;
    private boolean passpartu = false;
    
    public Management(DataSource database) {
        this.database = database;
    }

    public Management(DataSource database, boolean passpartu) {
        this.database = database;
        this.passpartu = passpartu;
    }   
    
    public String performLogin(Player player, String password) {
            
        String name = player.getName().toLowerCase();
        String ip = player.getAddress().getAddress().getHostAddress();
        
        if (PlayerCache.getInstance().isAuthenticated(name)) {
            return m._("logged_in");
           
        }

        if (!database.isAuthAvailable(player.getName().toLowerCase())) {
            return m._("user_unknown");
        }
        
        PlayerAuth pAuth = database.getAuth(name);
            // if Mysql is unavaible
            if(pAuth == null)
                return m._("user_unknown");
            
        String hash = pAuth.getHash();
        

        try {
            if(!passpartu) {
            if (PasswordSecurity.comparePasswordWithHash(password, hash)) {
                PlayerAuth auth = new PlayerAuth(name, hash, ip, new Date().getTime());
            
                database.updateSession(auth);
                PlayerCache.getInstance().addPlayer(auth);
                LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(name);
                if (limbo != null) {
                    if (Settings.protectInventoryBeforeLogInEnabled.booleanValue()) {
                    	RestoreInventoryEvent event = new RestoreInventoryEvent(player, limbo.getInventory(), limbo.getArmour());
                    	Bukkit.getServer().getPluginManager().callEvent(event);
                    	if (!event.isCancelled()) {
                    		API.setPlayerInventory(player, limbo.getInventory(), limbo.getArmour());
                    	}
                                }
                      player.setGameMode(GameMode.getByValue(limbo.getGameMode()));
                      player.setOp(limbo.getOperator());
                    
                      this.utils.addNormal(player, limbo.getGroup());
                    
                      World world = player.getWorld();
                    
                      if ((Settings.isTeleportToSpawnEnabled.booleanValue()) && (!Settings.isForceSpawnLocOnJoinEnabled.booleanValue()))
                                {
                        if ((Settings.isSaveQuitLocationEnabled.booleanValue()) && (this.database.getAuth(name).getQuitLocY() != 0))
                                  {
                          this.utils.packCoords(player.getWorld(), this.database.getAuth(name).getQuitLocX(), this.database.getAuth(name).getQuitLocY(), this.database.getAuth(name).getQuitLocZ(), player);
                                  }
                                  else {
                          if (!world.getChunkAt(limbo.getLoc()).isLoaded()) {
                            world.getChunkAt(limbo.getLoc()).load();
                                    }
                         player.teleport(limbo.getLoc());
                                  }
                    
                                }
                      else if (Settings.isForceSpawnLocOnJoinEnabled.booleanValue()) {
                        player.teleport(player.getWorld().getSpawnLocation());
                                }
                      else if ((Settings.isSaveQuitLocationEnabled.booleanValue()) && (this.database.getAuth(name).getQuitLocY() != 0))
                                {
                        this.utils.packCoords(player.getWorld(), this.database.getAuth(name).getQuitLocX(), this.database.getAuth(name).getQuitLocY(), this.database.getAuth(name).getQuitLocZ(), player);
                                }
                                else {
                        if (!world.getChunkAt(limbo.getLoc()).isLoaded())
                                  {
                          world.getChunkAt(limbo.getLoc()).load();
                                  }
                        player.teleport(limbo.getLoc());
                                }
                    
                      player.getServer().getScheduler().cancelTask(limbo.getTimeoutTaskId());
                      LimboCache.getInstance().deleteLimboPlayer(name);
                      if (this.playerCache.doesCacheExist(name)) {
                        this.playerCache.removeCache(name);
                                }
                    
                              }
                
               /*
                *  Little Work Around under Registration Group Switching for admins that
                *  add Registration thru a web Scripts.
                */
                if ( Settings.isPermissionCheckEnabled && AuthMe.permission.playerInGroup(player, Settings.unRegisteredGroup) && !Settings.unRegisteredGroup.isEmpty() ) {
                    AuthMe.permission.playerRemoveGroup(player.getWorld(), player.getName(), Settings.unRegisteredGroup);
                    AuthMe.permission.playerAddGroup(player.getWorld(), player.getName(), Settings.getRegisteredGroup);
                }
                
                if (!PlayersLogs.players.contains(player.getName()))
                	PlayersLogs.players.add(player.getName());
                pllog.save();
                player.sendMessage(m._("login"));
                if (!Settings.noConsoleSpam)
                ConsoleLogger.info(player.getDisplayName() + " logged in!");
                player.saveData();
                
            } else {
            	if (!Settings.noConsoleSpam)
                ConsoleLogger.info(player.getDisplayName() + " used the wrong password");
                if (Settings.isKickOnWrongPasswordEnabled) {
                    int gm = AuthMePlayerListener.gameMode.get(name);
                	player.setGameMode(GameMode.getByValue(gm));
                    player.kickPlayer(m._("wrong_pwd"));
                } else {
                    return (m._("wrong_pwd"));
                }
            }
         } else {
            // need for bypass password check if passpartu command is enabled
                PlayerAuth auth = new PlayerAuth(name, hash, ip, new Date().getTime());
                database.updateSession(auth);
                PlayerCache.getInstance().addPlayer(auth);
                LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(name);
                if (limbo != null) {
                    if (Settings.protectInventoryBeforeLogInEnabled.booleanValue()) {
                    	RestoreInventoryEvent event = new RestoreInventoryEvent(player, limbo.getInventory(), limbo.getArmour());
                    	Bukkit.getServer().getPluginManager().callEvent(event);
                    	if (!event.isCancelled()) {
                    		API.setPlayerInventory(player, limbo.getInventory(), limbo.getArmour());
                    	}
                                }
                      player.setGameMode(GameMode.getByValue(limbo.getGameMode()));
                      player.setOp(limbo.getOperator());
                      
                      this.utils.addNormal(player, limbo.getGroup());
                      
                      World world = player.getWorld();
                      if ((Settings.isTeleportToSpawnEnabled.booleanValue()) && (!Settings.isForceSpawnLocOnJoinEnabled.booleanValue()))
                                {
                        if ((Settings.isSaveQuitLocationEnabled.booleanValue()) && (this.database.getAuth(name).getQuitLocY() != 0)) {
                          Location quitLoc = new Location(player.getWorld(), this.database.getAuth(name).getQuitLocX() + 0.5D, this.database.getAuth(name).getQuitLocY() + 0.5D, this.database.getAuth(name).getQuitLocZ() + 0.5D);
                      
                          if (!world.getChunkAt(quitLoc).isLoaded())
                                    {
                            world.getChunkAt(quitLoc).load();
                                    }
                          player.teleport(quitLoc);
                                  }
                                  else
                                  {
                          if (!world.getChunkAt(limbo.getLoc()).isLoaded())
                            world.getChunkAt(limbo.getLoc()).load();
                          player.teleport(limbo.getLoc());
                                  }
                      
                                }
                      else if (Settings.isForceSpawnLocOnJoinEnabled.booleanValue()) {
                        player.teleport(player.getWorld().getSpawnLocation());
                                }
                      else if ((Settings.isSaveQuitLocationEnabled.booleanValue()) && (this.database.getAuth(name).getQuitLocY() != 0)) {
                        Location quitLoc = new Location(player.getWorld(), this.database.getAuth(name).getQuitLocX() + 0.5D, this.database.getAuth(name).getQuitLocY() + 0.5D, this.database.getAuth(name).getQuitLocZ() + 0.5D);
                      
                        if (!world.getChunkAt(quitLoc).isLoaded())
                                  {
                          world.getChunkAt(quitLoc).load();
                                  }
                        player.teleport(quitLoc);
                                }
                                else
                                {
                        if (!world.getChunkAt(limbo.getLoc()).isLoaded())
                                  {
                          world.getChunkAt(limbo.getLoc()).load();
                                  }
                        player.teleport(limbo.getLoc());
                                }
                      
                      player.getServer().getScheduler().cancelTask(limbo.getTimeoutTaskId());
                      LimboCache.getInstance().deleteLimboPlayer(name);
                      if (this.playerCache.doesCacheExist(name)) {
                        this.playerCache.removeCache(name);
                                }
                      
                              }
                
               /*
                *  Little Work Around under Registration Group Switching for admins that
                *  add Registration thru a web Scripts.
                */
                if ( Settings.isPermissionCheckEnabled && AuthMe.permission.playerInGroup(player, Settings.unRegisteredGroup) && !Settings.unRegisteredGroup.isEmpty() ) {
                    AuthMe.permission.playerRemoveGroup(player.getWorld(), player.getName(), Settings.unRegisteredGroup);
                    AuthMe.permission.playerAddGroup(player.getWorld(), player.getName(), Settings.getRegisteredGroup);
                }
                    
                if (!PlayersLogs.players.contains(player.getName()))
                	PlayersLogs.players.add(player.getName());
                pllog.save();
                player.sendMessage(m._("login"));
                if(!Settings.noConsoleSpam)
                ConsoleLogger.info(player.getDisplayName() + " logged in!");
                player.saveData(); 
                this.passpartu = false;
            }                
          
        } catch (NoSuchAlgorithmException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return (m._("error"));
        }
        return "";
	}
    
    
}

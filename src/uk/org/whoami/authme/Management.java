package uk.org.whoami.authme;

import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import uk.org.whoami.authme.api.API;
import uk.org.whoami.authme.cache.auth.PlayerAuth;
import uk.org.whoami.authme.cache.auth.PlayerCache;
import uk.org.whoami.authme.cache.backup.FileCache;
import uk.org.whoami.authme.cache.limbo.LimboCache;
import uk.org.whoami.authme.cache.limbo.LimboPlayer;
import uk.org.whoami.authme.datasource.DataSource;
import uk.org.whoami.authme.events.LoginEvent;
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
        World world = player.getWorld();
        Location spawnLoc = world.getSpawnLocation();
        while(world.getBlockAt(spawnLoc).getType() != Material.AIR) {
        	spawnLoc.setY(spawnLoc.getY() + 1);
        }
        
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
            
            //if columnGroup is set
            if(!Settings.getMySQLColumnGroup.isEmpty() && pAuth.getGroupId() == Settings.getNonActivatedGroup) {
            	return m._("vb_nonActiv");
            }
            
        String hash = pAuth.getHash();
        String email = pAuth.getEmail();
        

        try {
            if(!passpartu) {
            if (PasswordSecurity.comparePasswordWithHash(password, hash, name)) {
                PlayerAuth auth = new PlayerAuth(name, hash, ip, new Date().getTime(), email);
            
                database.updateSession(auth);
                PlayerCache.getInstance().addPlayer(auth);
                LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(name);
                if (limbo != null) {

                	
                      player.setOp(limbo.getOperator());
                    
                      this.utils.addNormal(player, limbo.getGroup());
                    
                    
                      if ((Settings.isTeleportToSpawnEnabled.booleanValue()) && (!Settings.isForceSpawnLocOnJoinEnabled.booleanValue()))
                                {
                        if ((Settings.isSaveQuitLocationEnabled.booleanValue()) && (this.database.getAuth(name).getQuitLocY() != 0))
                                  {
                          this.utils.packCoords(this.database.getAuth(name).getQuitLocX(), this.database.getAuth(name).getQuitLocY(), this.database.getAuth(name).getQuitLocZ(), player);
                                  }
                                  else {
                          if (!world.getChunkAt(limbo.getLoc()).isLoaded()) {
                            world.getChunkAt(limbo.getLoc()).load();
                                    }
                          while(world.getBlockAt(limbo.getLoc()).getType() != Material.AIR) {
                        	  limbo.getLoc().setY(limbo.getLoc().getY() + 1);
                          }
                         player.teleport(limbo.getLoc());
                                  }
                    
                                }
                      else if (Settings.isForceSpawnLocOnJoinEnabled.booleanValue()) {
                          if (!world.getChunkAt(spawnLoc).isLoaded()) {
                              world.getChunkAt(spawnLoc).load();
                                      }
                        player.teleport(spawnLoc);
                                }
                      else if ((Settings.isSaveQuitLocationEnabled.booleanValue()) && (this.database.getAuth(name).getQuitLocY() != 0))
                                {
                        this.utils.packCoords(this.database.getAuth(name).getQuitLocX(), this.database.getAuth(name).getQuitLocY(), this.database.getAuth(name).getQuitLocZ(), player);
                                }
                                else {
                        if (!world.getChunkAt(limbo.getLoc()).isLoaded())
                                  {
                          world.getChunkAt(limbo.getLoc()).load();
                                  }
                        while(world.getBlockAt(limbo.getLoc()).getType() != Material.AIR) {
                      	  limbo.getLoc().setY(limbo.getLoc().getY() + 1);
                        }
                        player.teleport(limbo.getLoc());
                                }
                      
                      
                      player.setGameMode(GameMode.getByValue(limbo.getGameMode()));
                      
                      if (Settings.protectInventoryBeforeLogInEnabled.booleanValue() && player.hasPlayedBefore()) {
                      		RestoreInventoryEvent event = new RestoreInventoryEvent(player, limbo.getInventory(), limbo.getArmour());
                      		Bukkit.getServer().getPluginManager().callEvent(event);
                      		if (!event.isCancelled()) {
                      			API.setPlayerInventory(player, limbo.getInventory(), limbo.getArmour());
                      		}
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
                
                try {
                    if (!PlayersLogs.players.contains(player.getName()))
                    	PlayersLogs.players.add(player.getName());
                    pllog.save();
                } catch (NullPointerException ex) {
                	
                }
                
                Bukkit.getServer().getPluginManager().callEvent(new LoginEvent(player, true));
                player.sendMessage(m._("login"));
                displayOtherAccounts(auth);
                if(!Settings.noConsoleSpam)
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
                PlayerAuth auth = new PlayerAuth(name, hash, ip, new Date().getTime(), email);
                database.updateSession(auth);
                PlayerCache.getInstance().addPlayer(auth);
                LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(name);
                if (limbo != null) {

                      
                      player.setOp(limbo.getOperator());
                      
                      this.utils.addNormal(player, limbo.getGroup());
                      

                      if ((Settings.isTeleportToSpawnEnabled.booleanValue()) && (!Settings.isForceSpawnLocOnJoinEnabled.booleanValue()))
                                {
                        if ((Settings.isSaveQuitLocationEnabled.booleanValue()) && (this.database.getAuth(name).getQuitLocY() != 0)) {
                          Location quitLoc = new Location(player.getWorld(), this.database.getAuth(name).getQuitLocX() + 0.5D, this.database.getAuth(name).getQuitLocY() + 0.5D, this.database.getAuth(name).getQuitLocZ() + 0.5D);
                      
                          if (!world.getChunkAt(quitLoc).isLoaded())
                                    {
                            world.getChunkAt(quitLoc).load();
                                    }
                          while(world.getBlockAt(quitLoc).getType() != Material.AIR) {
                        	  quitLoc.setY(quitLoc.getY() + 1);
                          }
                          player.teleport(quitLoc);
                                  }
                                  else
                                  {
                          if (!world.getChunkAt(limbo.getLoc()).isLoaded())
                            world.getChunkAt(limbo.getLoc()).load();
                          while(world.getBlockAt(limbo.getLoc()).getType() != Material.AIR) {
                        	  limbo.getLoc().setY(limbo.getLoc().getY() + 1);
                          }
                          player.teleport(limbo.getLoc());
                                  }
                      
                                }
                      else if (Settings.isForceSpawnLocOnJoinEnabled.booleanValue()) {
                          if (!world.getChunkAt(spawnLoc).isLoaded()) {
                              world.getChunkAt(spawnLoc).load();
                                      }
                        player.teleport(spawnLoc);
                                }
                      else if ((Settings.isSaveQuitLocationEnabled.booleanValue()) && (this.database.getAuth(name).getQuitLocY() != 0)) {
                        Location quitLoc = new Location(player.getWorld(), this.database.getAuth(name).getQuitLocX() + 0.5D, this.database.getAuth(name).getQuitLocY() + 0.5D, this.database.getAuth(name).getQuitLocZ() + 0.5D);
                      
                        if (!world.getChunkAt(quitLoc).isLoaded())
                                  {
                          world.getChunkAt(quitLoc).load();
                                  }
                        while(world.getBlockAt(quitLoc).getType() != Material.AIR) {
                      	  quitLoc.setY(quitLoc.getY() + 1);
                        }
                        player.teleport(quitLoc);
                                }
                                else
                                {
                        if (!world.getChunkAt(limbo.getLoc()).isLoaded())
                                  {
                          world.getChunkAt(limbo.getLoc()).load();
                                  }
                        while(world.getBlockAt(limbo.getLoc()).getType() != Material.AIR) {
                      	  limbo.getLoc().setY(limbo.getLoc().getY() + 1);
                        }
                        player.teleport(limbo.getLoc());
                                }
                      
                      
                      player.setGameMode(GameMode.getByValue(limbo.getGameMode()));
                      
                      if (Settings.protectInventoryBeforeLogInEnabled.booleanValue() && player.hasPlayedBefore()) {
                      	RestoreInventoryEvent event = new RestoreInventoryEvent(player, limbo.getInventory(), limbo.getArmour());
                      	Bukkit.getServer().getPluginManager().callEvent(event);
                      	if (!event.isCancelled()) {
                      		API.setPlayerInventory(player, limbo.getInventory(), limbo.getArmour());
                      	}
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
                
                try {
                    if (!PlayersLogs.players.contains(player.getName()))
                    	PlayersLogs.players.add(player.getName());
                    pllog.save();
                } catch (NullPointerException ex) { }
                
                Bukkit.getServer().getPluginManager().callEvent(new LoginEvent(player, true));
                player.sendMessage(m._("login"));
                displayOtherAccounts(auth);
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
    
    private void displayOtherAccounts(PlayerAuth auth) {
    	if (!Settings.displayOtherAccounts) {
    		return;
    	}
    	if (auth == null) {
    		return;
    	}
    	if (this.database.getAllAuthsByName(auth).isEmpty() || this.database.getAllAuthsByName(auth) == null) {
    		return;
    	}
    	if(this.database.getAllAuthsByName(auth).size() == 1) {
    		return;
    	}
    	List<String> accountList = this.database.getAllAuthsByName(auth);
    	String message = "[AuthMe] ";
    	int i = 0;
    	for (String account : accountList) {
    		i++;
    		message = message + account;
    		if (i != accountList.size()) {
    			message = message + ", ";
    		} else {
    			message = message + ".";
    		}
    		
    	}
    	for (Player player : AuthMe.getInstance().getServer().getOnlinePlayers()) {
    		if (player.hasPermission("authme.seeOtherAccounts")) {
    			player.sendMessage("[AuthMe] The player " + auth.getNickname() + " has " + String.valueOf(accountList.size()) + " accounts");
    			player.sendMessage(message);
    		}
    	}
    }
    
    
}

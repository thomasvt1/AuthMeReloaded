/*
 * Copyright 2011 Sebastian Köhler <sebkoehler@whoami.org.uk>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.org.whoami.authme.cache.limbo;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import uk.org.whoami.authme.cache.backup.FileCache;
import uk.org.whoami.authme.events.ResetInventoryEvent;
import uk.org.whoami.authme.events.StoreInventoryEvent;
import uk.org.whoami.authme.settings.Settings;

public class LimboCache {

    private static LimboCache singleton = null;
    private HashMap<String, LimboPlayer> cache;
    private FileCache playerData = new FileCache();
    
    private LimboCache() {
        this.cache = new HashMap<String, LimboPlayer>();
    }

    public void addLimboPlayer(Player player) {
        String name = player.getName().toLowerCase();
        Location loc = player.getLocation();
        int gameMode = player.getGameMode().getValue();
        ItemStack[] arm = null;
        ItemStack[] inv = null;
        boolean operator;
        String playerGroup = "";
        
        if (playerData.doesCacheExist(name)) {
        	StoreInventoryEvent event = new StoreInventoryEvent(player, playerData);
        	Bukkit.getServer().getPluginManager().callEvent(event);
        	if (!event.isCancelled()) {
                inv =  playerData.readCache(name).getInventory();
                arm =  playerData.readCache(name).getArmour();
        	}
             playerGroup = playerData.readCache(name).getGroup();
             operator = playerData.readCache(name).getOperator();
        } else {
        	StoreInventoryEvent event = new StoreInventoryEvent(player);
        	Bukkit.getServer().getPluginManager().callEvent(event);
        	if (!event.isCancelled()) {
        		inv =  player.getInventory().getContents();
        		arm =  player.getInventory().getArmorContents();
        	}
   
            if(player.isOp() ) {
                operator = true;
                }
                   else operator = false;      
        }

       
        
        if(Settings.isForceSurvivalModeEnabled) {
            if(Settings.isResetInventoryIfCreative && gameMode != 0 ) {
            	ResetInventoryEvent event = new ResetInventoryEvent(player);
            	Bukkit.getServer().getPluginManager().callEvent(event);
            	if (!event.isCancelled()) {
            		player.sendMessage("Your inventory has been cleaned!");
            	}
            }
            gameMode = 0;
        } 
        if(player.isDead()) {
        	loc = player.getWorld().getSpawnLocation();
        }
        
        if(cache.containsKey(name) && playerGroup.isEmpty()) {
            LimboPlayer groupLimbo = cache.get(name);
            playerGroup = groupLimbo.getGroup();
        }
        
        if (inv != null && arm != null)
        cache.put(player.getName().toLowerCase(), new LimboPlayer(name, loc, inv, arm, gameMode, operator, playerGroup));
        else cache.put(player.getName().toLowerCase(), new LimboPlayer(name, loc, gameMode, operator, playerGroup));
    }
    
    public void addLimboPlayer(Player player, String group) {
        
        cache.put(player.getName().toLowerCase(), new LimboPlayer(player.getName().toLowerCase(), group));
    }
    
    public void deleteLimboPlayer(String name) {
        cache.remove(name);
    }

    public LimboPlayer getLimboPlayer(String name) {
        return cache.get(name);
    }

    public boolean hasLimboPlayer(String name) {
        return cache.containsKey(name);
    }
    
    
    public static LimboCache getInstance() {
        if (singleton == null) {
            singleton = new LimboCache();
        }
        return singleton;
    }
}

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

package uk.org.whoami.authme.plugin.manager;

import net.citizensnpcs.Citizens;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.CitizensManager;

import org.bukkit.entity.Entity;
import org.bukkit.plugin.PluginManager;

import uk.org.whoami.authme.AuthMe;

public class CitizensCommunicator {
	
    static AuthMe instance;
    
    public CitizensCommunicator(AuthMe instance) {
    	CitizensCommunicator.instance = instance;
    }

    public static boolean isNPC(Entity player) {
        PluginManager pm = instance.getServer().getPluginManager();
        if (pm.getPlugin("Citizens") != null) {
        	try {
            	Citizens plugin = (Citizens) pm.getPlugin("Citizens");

                String ver = plugin.getDescription().getVersion();
                String[] args = ver.split("\\.");
                
                if(args[0].contains("1")) 
                    return CitizensManager.isNPC(player);
                else return CitizensAPI.getNPCRegistry().isNPC(player);
        	} catch (NullPointerException npe) {
        		return false;
        	} catch (ClassCastException cce) {
        		return false;
        	} catch (IllegalStateException ise) {
        		return false;
        	}

        }
        return false;
    }
}

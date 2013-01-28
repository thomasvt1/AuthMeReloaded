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
	
    public AuthMe instance;
	private boolean isnpc;
    
    public CitizensCommunicator(AuthMe instance) {
    	this.instance = instance;
    }

    public boolean isNPC(final Entity player, AuthMe instance) {
		        PluginManager pm = instance.getServer().getPluginManager();
		        if (pm.getPlugin("Citizens") != null) {
		        	try {
		            	Citizens plugin = (Citizens) pm.getPlugin("Citizens");
		            	if (plugin != null) {
		                    String ver = plugin.getDescription().getVersion();
		                    String[] args = ver.split("\\.");
		                    
		                    	if(args[0].contains("1")) {
		                    		try {
		                    			isnpc = CitizensManager.isNPC(player);
		                    		} catch (NullPointerException NPE) {
		                    			isnpc = false;
		                    		}
		                    	}
		                        else {
		                        	try {
		                        		isnpc = CitizensAPI.getNPCRegistry().isNPC(player);
		                        	} catch (NullPointerException NPE) {
		                        		isnpc = false;
		                        	}
		                        }
		            	}
		            	
		        	} catch (NullPointerException npe) {
		        		isnpc = false;
		        	} catch (ClassCastException cce) {
		        		isnpc = false;
		        	} catch (IllegalStateException ise) {
		        		isnpc = false;
		        	} catch (NoClassDefFoundError ncdfe) {
		        		isnpc = false;
		        	} catch (Exception ex) {
		        		isnpc = false;
		        	}

		        } else {
		        	isnpc = false;
		        }
		return isnpc;
    }
}

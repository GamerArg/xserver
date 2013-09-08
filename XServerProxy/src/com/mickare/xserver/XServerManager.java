package com.mickare.xserver;

import java.io.IOException;

import net.md_5.bungee.api.plugin.Plugin;

import com.mickare.xserver.exceptions.InvalidConfigurationException;
import com.mickare.xserver.exceptions.NotInitializedException;
import com.mickare.xserver.util.MySQL;

public class XServerManager extends AbstractXServerManager<Plugin> {

	// In Milliseconds
		private static final long AUTORECONNECT = 10000;
		public static final XType HOMETYPE = XType.BungeeCord;

		private static XServerManager instance = null;

		public static XServerManager getInstance() throws NotInitializedException {
			if (instance == null) {
				throw new NotInitializedException();
			}
			return instance;
		}
	
	protected XServerManager(String servername,
			XServerPlugin<Plugin> plugin, MySQL connection,
			EventHandler<Plugin> eventhandler)
			throws InvalidConfigurationException, IOException {
		super(servername, plugin, connection, eventhandler);
		
	}

	@Override
	public long getAutoReconnectTime() {
		return AUTORECONNECT;
	}

	@Override
	public XType getHomeType() {
		return HOMETYPE;
	}

}
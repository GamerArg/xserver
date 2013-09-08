package com.mickare.xserver.net;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.mickare.xserver.AbstractXServerManager;
import com.mickare.xserver.Message;
import com.mickare.xserver.XType;
import com.mickare.xserver.events.XServerMessageOutgoingEvent;
import com.mickare.xserver.exceptions.NotConnectedException;
import com.mickare.xserver.exceptions.NotInitializedException;
import com.mickare.xserver.util.CacheList;
import com.mickare.xserver.util.Encryption;

public class XServer<T> {

	private final static int MESSAGE_CACHE_SIZE = 256;
	
	private final String name;
	private final String host;
	private final int port;
	private final String password;

	private Connection<T> connection = null;
	private Connection<T> connection2 = null;	// Fix for HomeServer that is not connectable.
	private Lock conLock = new ReentrantLock();
	
	private Lock typeLock = new ReentrantLock();
	private XType type = XType.Other;

	private CacheList<Packet> pendingPackets = new CacheList<Packet>(MESSAGE_CACHE_SIZE);
	
	private final AbstractXServerManager<T> manager;
	
	public XServer(String name, String host, int port, String password, AbstractXServerManager<T> manager) {
		this.name = name;
		this.host = host;
		this.port = port;
		this.password = Encryption.MD5(password);
		this.manager = manager;
	}
	
	public XServer(String name, String host, int port, String password, XType type, AbstractXServerManager<T> manager) {
		this.name = name;
		this.host = host;
		this.port = port;
		this.password = Encryption.MD5(password);
		this.type = type;
		this.manager = manager;
	}

	public void connect() throws UnknownHostException, IOException,
			InterruptedException, NotInitializedException {
		conLock.lock();
		try {
			if(this.connection != null ? !this.connection.isLoggingIn() : true) {
				if (isConnected()) {
					this.disconnect();
				}
				connection = new Connection<T>(manager
						.getSocketFactory(), host, port, manager);
			}
		} finally {
			conLock.unlock();
		}
	}

	protected void setConnection(Connection<T> con) {
		conLock.lock();
		try {
			if (this.connection != con && isConnected()) {
				for(Packet p : this.connection.getPendingPackets()) {
					if(p.getPacketID() == PacketType.Message.packetID) {
						this.pendingPackets.push(p);
					}
				}
				this.disconnect();
			}
			this.connection = con;
		} finally {
			conLock.unlock();
		}
	}

	public void setReloginConnection(Connection<T> con) {
		conLock.lock();
		try {
			if(manager.getHomeServer() == this) {
				if (this.connection2 != con && (this.connection2 != null ? this.connection2.isConnected() : false)) {
					this.disconnect();
				}
				this.connection2 = con;
			} else {
				setConnection(con);
			}
		} finally {
			conLock.unlock();
		}
	}
	
	public boolean isConnected() {
		conLock.lock();
		try {
			return connection != null ? connection.isLoggedIn() : false;
		} finally {
			conLock.unlock();
		}
	}

	public void disconnect() {
		conLock.lock();
		try {
			if(connection != null) {
				connection.disconnect();
				connection = null;
				connection2 = null;
			}
		} finally {
			conLock.unlock();
		}
	}

	public String getName() {
		return name;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public String getPassword() {
		return password;
	}

	public void sendMessage(Message message) throws NotConnectedException, IOException {
		conLock.lock();
		try {
			if (!isConnected() || (connection != null ? !connection.isLoggedIn() : false)) {
				pendingPackets.push(new Packet(PacketType.Message, message.getData()));
				throw new NotConnectedException("Not Connected to this server!");
			}
			connection
					.send(new Packet(PacketType.Message, message.getData()));
		} finally {
			conLock.unlock();
		}
		
		manager.getEventHandler().callEvent(new XServerMessageOutgoingEvent(this, message));
		
	}
	
	public void ping(Ping<?> ping) throws InterruptedException, IOException {
		conLock.lock();
		if(isConnected()) {
			connection.ping(ping);
		}
		conLock.unlock();
	}
	
	public void flushCache() {
		conLock.lock();
		try {
			Packet p = pendingPackets.pollLast();
			while(p != null) {
				connection.send(p);
				p = pendingPackets.pollLast();
			}
		} finally {
			conLock.unlock();
		}
	}

	public XType getType()
	{
		typeLock.lock();
		try {
			return type;
		} finally {
			typeLock.unlock();
		}
	}

	protected void setType(XType type)
	{
		typeLock.lock();
		try {
			this.type = type;
		} finally {
			typeLock.unlock();
		}
	}

	public AbstractXServerManager<T> getManager()
	{
		return manager;
	}

	
	
}
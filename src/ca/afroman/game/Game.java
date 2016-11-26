package ca.afroman.game;

import java.util.ArrayList;
import java.util.List;

import ca.afroman.client.ClientGame;
import ca.afroman.client.ExitGameReason;
import ca.afroman.entity.PlayerEntity;
import ca.afroman.interfaces.IPacketParser;
import ca.afroman.level.MainLevel;
import ca.afroman.level.api.Level;
import ca.afroman.level.api.LevelType;
import ca.afroman.network.IncomingPacketWrapper;
import ca.afroman.packet.PacketStopServer;
import ca.afroman.resource.IDCounter;
import ca.afroman.server.ServerGame;
import ca.afroman.thread.DynamicTickRenderThread;

public abstract class Game extends DynamicTickRenderThread implements IPacketParser
{
	public static final int MAX_PLAYERS = 8;
	public static final int DEFAULT_PORT = 2143;
	public static final String IPv4_LOCALHOST = "127.0.0.1";
	
	public static Game instance(boolean serverSide)
	{
		return serverSide ? ServerGame.instance() : ClientGame.instance();
	}
	
	private boolean isPaused;
	private boolean isInGame;
	protected List<Level> levels;
	
	protected List<PlayerEntity> players;
	private SocketManager socketManager;
	
	private List<IncomingPacketWrapper> toProcess;
	
	public Game(boolean isServerSide, ThreadGroup threadGroup, String name, int ticks)
	{
		super(isServerSide, threadGroup, name, ticks);
		isPaused = false;
		isInGame = false;
		
		levels = new ArrayList<Level>();
		players = new ArrayList<PlayerEntity>(2);
		
		toProcess = new ArrayList<IncomingPacketWrapper>();
	}
	
	@Override
	public void addPacketToParse(IncomingPacketWrapper pack)
	{
		synchronized (toProcess)
		{
			toProcess.add(pack);
		}
	}
	
	public Level getLevel(LevelType type)
	{
		for (Level level : getLevels())
		{
			if (level.getLevelType() == type) return level;
		}
		return null;
	}
	
	public List<Level> getLevels()
	{
		return levels;
	}
	
	/**
	 * Gets the player with the given role.
	 * 
	 * @param role whether it's player 1 or 2
	 * @return the player.
	 */
	public PlayerEntity getPlayer(Role role)
	{
		for (PlayerEntity entity : getPlayers())
		{
			if (entity.getRole() == role) return entity;
		}
		return null;
	}
	
	public List<PlayerEntity> getPlayers()
	{
		return players;
	}
	
	public boolean isInGame()
	{
		return isInGame;
	}
	
	public boolean isPaused()
	{
		return isPaused;
	}
	
	public void loadLevels()
	{
		levels.clear();
		levels.add(new MainLevel(isServerSide()));
		levels.add(new Level(isServerSide(), LevelType.SECOND));
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
		isPaused = false;
	}
	
	@Override
	public void onUnpause()
	{
		super.onUnpause();
		isPaused = true;
	}
	
	public void setIsInGame(boolean isInGame)
	{
		this.isInGame = isInGame;
	}
	
	public SocketManager sockets()
	{
		return socketManager;
	}
	
	public boolean startSocket(String serverIpAddress, int port)
	{
		stopSocket();
		
		socketManager = new SocketManager(this);
		boolean successful = socketManager.setServerConnection(serverIpAddress, SocketManager.validatedPort(port));
		
		if (!successful) socketManager = null;
		
		return successful;
	}
	
	public void stopSocket()
	{
		if (socketManager != null) socketManager.stopThis();
		socketManager = null;
	}
	
	@Override
	public void stopThis()
	{
		super.stopThis();
		
		if (isServerSide())
		{
			sockets().sender().sendPacketToAllClients(new PacketStopServer());
			
			// TODO make a more surefire way to ensure that all clients got the message
			try
			{
				Thread.sleep(500);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			ClientGame.instance().exitFromGame(ExitGameReason.DISCONNECT);
		}
		
		stopSocket();
		
		synchronized (toProcess)
		{
			toProcess.clear();
		}
		
		if (getLevels() != null) getLevels().clear();
		IDCounter.resetAll();
	}
	
	@Override
	public void tick()
	{
		synchronized (toProcess)
		{
			for (IncomingPacketWrapper pack : toProcess)
			{
				parsePacket(pack);
			}
			
			toProcess.clear();
		}
	}
}

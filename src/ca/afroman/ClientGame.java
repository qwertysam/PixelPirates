package ca.afroman;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import ca.afroman.asset.AssetType;
import ca.afroman.assets.Assets;
import ca.afroman.assets.Texture;
import ca.afroman.console.ConsoleOutput;
import ca.afroman.gui.GuiConnectToServer;
import ca.afroman.gui.GuiMainMenu;
import ca.afroman.gui.GuiScreen;
import ca.afroman.input.InputHandler;
import ca.afroman.level.ClientLevel;
import ca.afroman.level.LevelType;
import ca.afroman.packet.PacketRequestConnection;
import ca.afroman.server.ServerGame;

public class ClientGame extends Canvas implements Runnable
{
	private static final long serialVersionUID = 1L;
	
	private static ClientGame game;
	
	public static ClientGame instance()
	{
		return game;
	}
	
	public static final int WIDTH = 240;
	public static final int HEIGHT = WIDTH / 16 * 9;
	public static final int SCALE = 3;
	public static final String NAME = "Cancer: The Adventures of Afro Man";
	
	private JFrame frame;
	
	private Texture screen;
	
	private boolean fullscreen = false;
	private boolean hudDebug = false; // Shows debug information on the hud
	private boolean hitboxDebug = false; // Shows all hitboxes
	private boolean lightingDebug = false; // Turns off the lighting engine
	private boolean buildMode = false; // Turns off the lighting engine
	private boolean consoleDebug = false; // Shows a console window
	
	public boolean isHosting = false;
	public int updatePlayerList = 0; // Tells if the player list has been updated within the last tick
	
	public boolean running = false;
	public int tickCount = 0;
	public int tps = 0;
	public int fps = 0;
	
	public InputHandler input;
	
	public List<ClientLevel> levels;
	private ClientLevel currentLevel = null;
	// public Level blankLevel; TODO make level loading
	// public PlayerEntity player;
	
	private String username = "";
	private String password = "";
	private String typedIP = "";
	
	public ClientSocket socketClient = null;
	public ServerGame server = null;
	
	private GuiScreen currentScreen = null;
	
	public ClientGame()
	{
		setMinimumSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));
		setMaximumSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));
		setPreferredSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));
		
		frame = new JFrame(NAME);
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		
		frame.getContentPane().setBackground(Color.black);
		
		frame.add(this, BorderLayout.CENTER);
		frame.pack();
		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		
		// Loading screen
		long startTime = System.currentTimeMillis();
		this.repaint();
		final Texture loading = Texture.fromResource("/loading.png");
		StoppableThread renderLoading = new StoppableThread()
		{
			@Override
			public void run()
			{
				while (!isStopped)
				{
					getGraphics().drawImage(loading.getImage(), 0, 0, getWidth(), getHeight(), null);
					
					try
					{
						Thread.sleep(200);
					}
					catch (InterruptedException e)
					{
						e.printStackTrace();
					}
				}
			}
		};
		renderLoading.start();
		
		// DO THE LOADING
		
		// Allows key listens for TAB and such
		this.setFocusTraversalKeysEnabled(false);
		
		ConsoleOutput.createGui();
		ConsoleOutput.showGui();
		ConsoleOutput.hideGui();
		
		screen = new Texture(new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB));
		input = new InputHandler(this);
		levels = new ArrayList<ClientLevel>();
		
		// WHEN FINISHED LOADING
		
		long loadTime = System.currentTimeMillis() - startTime;
		
		int forcedDisplayTime = 500;
		
		// Makes you see the loading screen for at least a half second
		if (loadTime < forcedDisplayTime)
		{
			try
			{
				Thread.sleep(forcedDisplayTime - loadTime);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		
		// End the loading screen
		frame.setResizable(true);
		this.repaint();
		renderLoading.stopThread();
	}
	
	public void init()
	{
		socketClient = new ClientSocket();
		socketClient.start();
		
		setCurrentScreen(new GuiMainMenu());
		
		/*
		 * TODO add player to level
		 * player = new PlayerMPEntity(100, 120, 1, input, null, -1);
		 * player.addToLevel(blankLevel);
		 * // player = new PlayerMPEntity(blankLevel, 100, 100, 1, input, null, -1);
		 * // player.setCameraToFollow(true);
		 * // blankLevel.putPlayer();
		 */
	}
	
	@Override
	public void validate()
	{
		super.validate();
		
		resizeGame(this.getWidth(), this.getHeight());
	}
	
	/**
	 * Operation to perform to resize the game, keeping the aspect ratio.
	 * 
	 * @param windowWidth the new desired width.
	 * @param windowHeight the new desired height.
	 */
	public void resizeGame(int windowWidth, int windowHeight)
	{
		int newWidth = 0;
		int newHeight = 0;
		
		// If what the drawn height should be based on the width goes off screen
		if (windowWidth / 16 * 9 > windowHeight)
		{
			newWidth = windowHeight / 9 * 16;
			newHeight = windowHeight;
		}
		else // Else do the height based on the width
		{
			newWidth = windowWidth;
			newHeight = windowWidth / 16 * 9;
		}
		
		// Resizes the canvas to match the new window size, keeping it centred.
		setBounds((windowWidth - newWidth) / 2, (windowHeight - newHeight) / 2, newWidth, newHeight);
	}
	
	public synchronized void start()
	{
		init();
		running = true;
		new Thread(this).start();
	}
	
	public synchronized void stop()
	{
		running = false;
	}
	
	@Override
	public void run()
	{
		long lastTime = System.nanoTime();
		double ticksPerSecond = 60D;
		double nsPerTick = 1000000000D / ticksPerSecond;
		
		int ticks = 0;
		int frames = 0;
		
		long lastTimer = System.currentTimeMillis();
		double delta = 0;
		
		while (running)
		{
			long now = System.nanoTime();
			delta += (now - lastTime) / nsPerTick;
			lastTime = now;
			boolean shouldRender = true; // true for unlimited frames, false for limited to tick rate
			
			while (delta >= 1)
			{
				ticks++;
				tick();
				delta--;
				shouldRender = true;
			}
			
			// Stops system from overloading the CPU. Gives other threads a chance to run.
			try
			{
				// If the ticks per second is less than desired, allow this thread to run with less sleep-time
				Thread.sleep((tps < ticksPerSecond ? 1 : 3));
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			
			// Only render when something has been updated
			if (shouldRender)
			{
				frames++;
				render();
			}
			
			// If current time - the last time we updated is >= 1 second
			if (System.currentTimeMillis() - lastTimer >= 1000)
			{
				tps = ticks;
				fps = frames;
				lastTimer += 1000;
				frames = 0;
				ticks = 0;
			}
		}
	}
	
	public void tick()
	{
		tickCount++;
		
		if (input.consoleDebug.isReleasedFiltered())
		{
			consoleDebug = !consoleDebug;
			
			ConsoleOutput.setGuiVisible(consoleDebug);
			
			System.out.println("Show Console: " + consoleDebug);
			
			// this.requestFocus();
		}
		
		if (input.full_screen.isPressedFiltered())
		{
			// Toggles Fullscreen Mode
			setFullScreen(!fullscreen);
		}
		
		if (input.hudDebug.isPressedFiltered())
		{
			hudDebug = !hudDebug;
			
			System.out.println("Debug Hud: " + hudDebug);
		}
		
		if (input.hitboxDebug.isPressedFiltered())
		{
			hitboxDebug = !hitboxDebug;
			
			System.out.println("Show Hitboxes: " + hitboxDebug);
		}
		
		// TODO
		//
		// if (input.lightingDebug.isPressedFiltered())
		// {
		// lightingDebug = !lightingDebug;
		//
		// System.out.println("Show Lighting: " + !lightingDebug);
		// }
		
		if (input.saveLevel.isPressedFiltered())
		{
			if (currentLevel != null) currentLevel.toSaveFile();
			System.out.println("Copied current level save data to clipboard");
		}
		
		if (input.levelBuilder.isPressedFiltered())
		{
			buildMode = !buildMode;
			
			System.out.println("Build Mode: " + buildMode);
			
			// TODO player shiz
			// this.player.setCameraToFollow(!buildMode);
			// this.player.getLevel().toSaveFile();
		}
		
		// TODO blankLevel.tick();
		
		// Don't update the player list after the first tick that it has been updated.
		if (updatePlayerList > 0) updatePlayerList--;
		
		if (currentLevel != null)
		{
			currentLevel.tick();
		}
		
		if (currentScreen != null)
		{
			currentScreen.tick();
		}
	}
	
	public void render()
	{
		// System.out.println("RENDERING");
		
		// Clears the canvas
		screen.getGraphics().setColor(Color.WHITE);
		screen.getGraphics().fillRect(0, 0, screen.getWidth(), screen.getHeight());
		
		if (currentLevel != null)
		{
			currentLevel.render(screen);
		}
		
		/*
		 * TODO add back once working with server-side
		 * blankLevel.render(screen);
		 * if (this.tickCount < 240)
		 * {
		 * int xPos = (WIDTH / 2);
		 * int yPos = 120;
		 * Assets.getFont(Assets.FONT_WHITE).renderCentered(screen, WIDTH - xPos, HEIGHT - yPos, "CANCER:");
		 * Assets.getFont(Assets.FONT_WHITE).renderCentered(screen, WIDTH - xPos, HEIGHT + 15 - yPos, "The Adventures of");
		 * Assets.getFont(Assets.FONT_WHITE).renderCentered(screen, WIDTH - xPos, HEIGHT + 25 - yPos, "Afro Man");
		 * }
		 */
		if (hudDebug)
		{
			Assets.getFont(AssetType.FONT_BLACK).render(screen, 1, 0, "TPS: " + tps);
			Assets.getFont(AssetType.FONT_BLACK).render(screen, 1, 10, "FPS: " + fps);
			// Assets.getFont(Assets.FONT_BLACK).render(screen, 1, 20, "x: " + player.getX() );
			// Assets.getFont(Assets.FONT_BLACK).render(screen, 1, 30, "y: " + player.getY());
		}
		
		if (currentScreen != null)
		{
			currentScreen.render(screen);
		}
		
		// Renders everything that was just drawn
		BufferStrategy bs = getBufferStrategy();
		if (bs == null)
		{
			createBufferStrategy(2);
			return;
		}
		Graphics2D g = ((Graphics2D) bs.getDrawGraphics());
		// g.rotate(Math.toRadians(1), WIDTH /2, HEIGHT/2);
		g.drawImage(screen.getImage(), 0, 0, getWidth(), getHeight(), null);
		g.dispose();
		bs.show();
	}
	
	public void setFullScreen(boolean isFullScreen)
	{
		fullscreen = isFullScreen;
		
		GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		
		System.out.println("Setting Fullscreen: " + isFullScreen);
		
		/*
		 * This StackOverFlow thread was EXTREMELY helpful in getting this to work properly
		 * http://stackoverflow.com/questions/13064607/fullscreen-swing-components-fail-to-receive-keyboard-input-on-java-7-on-mac-os-x
		 */
		if (isFullScreen)
		{
			frame.dispose();// Restarts the JFrame
			frame.setResizable(false);// Disables resizing else causes bugs
			frame.setUndecorated(true);
			frame.setVisible(true);
			frame.revalidate();
			this.setSize(Toolkit.getDefaultToolkit().getScreenSize());
			try
			{
				gd.setFullScreenWindow(frame);// Makes it full screen
				
				// if (System.getProperty("os.name").indexOf("Mac OS X") >= 0)
				// {
				// this.setVisible(false);
				// this.setVisible(true);
				// }
				
				this.repaint();
				this.revalidate();
			}
			catch (Exception e)
			{
				setFullScreen(false);
				System.err.println("Fullscreen Mode not supported.");
				e.printStackTrace();
			}
		}
		else
		{
			frame.dispose();// Restarts the JFrame
			frame.setVisible(false);
			frame.setResizable(true);
			frame.setUndecorated(false);
			frame.setVisible(true);// Shows restarted JFrame
			frame.pack();
			frame.setExtendedState(frame.getExtendedState() | JFrame.NORMAL);// Returns to normal state
		}
		
		this.requestFocus();
	}
	
	public boolean isFullScreen()
	{
		return fullscreen;
	}
	
	public boolean isHudDebugging()
	{
		return hudDebug;
	}
	
	public boolean isHitboxDebugging()
	{
		return hitboxDebug;
	}
	
	public boolean isLightingDebugging()
	{
		return lightingDebug;
	}
	
	public boolean isBuildMode()
	{
		return buildMode;
	}
	
	public boolean isHostingServer()
	{
		return isHosting;
	}
	
	public void setCurrentScreen(GuiScreen screen)
	{
		this.currentScreen = screen;
	}
	
	public GuiScreen getCurrentScreen()
	{
		return currentScreen;
	}
	
	public void setUsername(String newUsername)
	{
		this.username = newUsername;
	}
	
	public String getUsername()
	{
		return username;
	}
	
	public void setPassword(String newPassword)
	{
		this.password = newPassword;
	}
	
	public String getPassword()
	{
		return password;
	}
	
	public void setServerIP(String newIP)
	{
		this.typedIP = newIP;
	}
	
	public String getServerIP()
	{
		return typedIP;
	}
	
	public boolean hasServerListBeenUpdated()
	{
		// TODO make it only run one tick, but throughout the entirety of the tick/
		// Currently it just runs throughout a random portion of the tick, so I make it
		// Run through twice just in case. THIS IS AN ISSUE
		return updatePlayerList > 0;
	}
	
	public void exitFromGame()
	{
		// TODO Stop the game
		this.levels.clear();
		this.isHosting = false;
		setCurrentScreen(new GuiMainMenu());
		this.socketClient.getPlayers().clear();
	}
	
	public void joinServer()
	{
		setCurrentScreen(new GuiConnectToServer(getCurrentScreen()));
		render();
		
		socketClient.setServerIP(getServerIP());
		socketClient.sendPacket(new PacketRequestConnection(getUsername(), getPassword()));
	}
	
	public ClientLevel getCurrentLevel()
	{
		return currentLevel;
	}
	
	public void setCurrentLevel(ClientLevel newLevel)
	{
		currentLevel = newLevel;
	}
	
	public ClientLevel getLevelByType(LevelType type)
	{
		for (ClientLevel level : levels)
		{
			if (level.getType() == type) return level;
		}
		return null;
	}
	
	public static void main(String[] args)
	{
		game = new ClientGame();
		game.start();
	}
}
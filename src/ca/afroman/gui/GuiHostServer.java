package ca.afroman.gui;

import ca.afroman.assets.AssetType;
import ca.afroman.assets.Assets;
import ca.afroman.assets.SpriteAnimation;
import ca.afroman.assets.Texture;
import ca.afroman.client.ClientGame;
import ca.afroman.game.Game;
import ca.afroman.gfx.FlickeringLight;
import ca.afroman.gfx.LightMap;
import ca.afroman.input.TypingMode;
import ca.afroman.option.ClientOptions;
import ca.afroman.option.ServerOptions;
import ca.afroman.resource.Vector2DDouble;
import ca.afroman.resource.Vector2DInt;
import ca.afroman.server.ServerGame;

public class GuiHostServer extends GuiScreen
{
	private SpriteAnimation afroMan;
	private SpriteAnimation player2;
	private LightMap lightmap;
	private FlickeringLight light;
	
	private GuiTextField username;
	private GuiTextField password;
	private GuiTextField port;
	
	private GuiTextButton hostButton;
	
	public GuiHostServer(GuiScreen parent)
	{
		super(parent);
		
		afroMan = Assets.getSpriteAnimation(AssetType.PLAYER_ONE_IDLE_DOWN);
		player2 = Assets.getSpriteAnimation(AssetType.PLAYER_TWO_IDLE_DOWN);
		
		lightmap = new LightMap(ClientGame.WIDTH, ClientGame.HEIGHT, LightMap.DEFAULT_AMBIENT);
		light = new FlickeringLight(false, -1, new Vector2DDouble(ClientGame.WIDTH / 2, 38), 60, 62, 5);
		
		username = new GuiTextField(this, (ClientGame.WIDTH / 2) - (112 / 2) - 57, 62, 112);
		username.setText(ClientOptions.instance().serverUsername);
		username.setMaxLength(11);
		username.setTypingMode(TypingMode.ONLY_NUMBERS_AND_LETTERS);
		username.setFocussed();
		
		password = new GuiTextField(this, (ClientGame.WIDTH / 2) - (112 / 2) - 57, 90, 72);
		password.setText(ServerOptions.instance().serverPassword);
		password.setMaxLength(11);
		password.setTypingMode(TypingMode.ONLY_NUMBERS_AND_LETTERS);
		
		port = new GuiTextField(this, (ClientGame.WIDTH / 2) - 37, 90, 36);
		port.setText(ServerOptions.instance().serverPort);
		port.setMaxLength(5);
		port.setTypingMode(TypingMode.ONLY_NUMBERS);
		
		addButton(username);
		addButton(password);
		addButton(port);
		
		hostButton = new GuiTextButton(this, 1, 144, 62, 72, Assets.getFont(AssetType.FONT_BLACK), "Host Server");
		hostButton.setEnabled(!this.username.getText().isEmpty());
		
		addButton(hostButton);
		addButton(new GuiTextButton(this, 200, 144, 90, 72, Assets.getFont(AssetType.FONT_BLACK), "Back"));
	}
	
	private boolean canContinue()
	{
		try
		{
			int port = Integer.parseInt(this.port.getText());
			
			if (port < 0 || port > 0xFFFF) return false;
		}
		catch (NumberFormatException e)
		{
			
		}
		
		return !this.username.getText().isEmpty();
	}
	
	@Override
	public void drawScreen(Texture renderTo)
	{
		renderTo.draw(afroMan.getCurrentFrame(), new Vector2DInt((ClientGame.WIDTH / 2) - 20, 30));
		renderTo.draw(player2.getCurrentFrame(), new Vector2DInt((ClientGame.WIDTH / 2) + 4, 30));
		
		if (ClientOptions.instance().isLightingOn())
		{
			lightmap.clear();
			light.renderCentered(lightmap);
			lightmap.patch();
			
			renderTo.draw(lightmap, LightMap.PATCH_POSITION);
		}
		
		nobleFont.renderCentered(renderTo, new Vector2DInt(ClientGame.WIDTH / 2, 15), "Host A Server");
		
		blackFont.renderCentered(renderTo, new Vector2DInt(ClientGame.WIDTH / 2 - 57, 62 - 10), "Username");
		blackFont.renderCentered(renderTo, new Vector2DInt(ClientGame.WIDTH / 2 - 78, 90 - 10), "Pass");
		blackFont.renderCentered(renderTo, new Vector2DInt(ClientGame.WIDTH / 2 - 19, 90 - 10), "Port");
	}
	
	@Override
	public void goToParentScreen()
	{
		onLeaving();
		
		super.goToParentScreen();
	}
	
	private void hostServer()
	{
		onLeaving();
		
		// If not already hosting
		if (!ClientGame.instance().isHostingServer())
		{
			if (ServerGame.instance() == null)
			{
				new ServerGame(ServerOptions.instance().serverPassword, ServerOptions.instance().serverPort);
			}
			else
			{
				// Start that server thread
				ServerGame.instance().startThis();
			}
		}
		
		ClientGame.instance().joinServer(ClientOptions.instance().serverUsername, ServerOptions.instance().serverPassword);
	}
	
	@Override
	public void keyTyped()
	{
		this.hostButton.setEnabled(canContinue());
	}
	
	private void onLeaving()
	{
		ClientOptions.instance().serverUsername = username.getText();
		ServerOptions.instance().serverPassword = password.getText();
		ServerOptions.instance().serverPort = port.getText();
		ServerOptions.instance().serverIP = Game.IPv4_LOCALHOST; // TODO allow manual setting of the IP from the options file
		
		ClientOptions.instance().save();
	}
	
	@Override
	public void pressAction(int buttonID)
	{
		switch (buttonID)
		{
			
		}
	}
	
	@Override
	public void releaseAction(int buttonID)
	{
		switch (buttonID)
		{
			case 1: // Host Server
				hostServer();
				break;
			case 200:
				goToParentScreen();
				break;
		}
	}
	
	@Override
	public void tick()
	{
		super.tick();
		
		if (ClientOptions.instance().isLightingOn())
		{
			light.tick();
			afroMan.tick();
			player2.tick();
		}
		
		if (ClientGame.instance().input().tab.isPressedFiltered())
		{
			if (username.isFocussed())
			{
				password.setFocussed();
			}
			else if (password.isFocussed())
			{
				port.setFocussed();
			}
			else
			{
				username.setFocussed();
			}
		}
		
		if (ClientGame.instance().input().enter.isPressedFiltered() && canContinue())
		{
			hostServer();
		}
		
		if (ClientGame.instance().input().escape.isPressedFiltered())
		{
			goToParentScreen();
		}
	}
}

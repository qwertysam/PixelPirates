package ca.afroman.gui;

import java.awt.Color;

import ca.afroman.Game;
import ca.afroman.assets.Assets;
import ca.afroman.assets.Font;
import ca.afroman.assets.SpriteAnimation;
import ca.afroman.assets.Texture;
import ca.afroman.gfx.FlickeringLight;
import ca.afroman.gfx.LightMap;

public class GuiJoinServer extends GuiScreen
{
	public static String ipText = "";
	public static String passwordText = "";
	
	private Font font;
	private SpriteAnimation afroMan;
	private SpriteAnimation player2;
	private LightMap lightmap;
	private FlickeringLight light;
	
	private GuiTextField username;
	private GuiTextField serverIP;
	private GuiTextField password;
	
	private GuiTextButton joinButton;
	
	public GuiJoinServer(Game game, GuiScreen parent)
	{
		super(game, parent);
	}
	
	@Override
	public void init()
	{
		font = Assets.getFont(Assets.FONT_NORMAL);
		afroMan = Assets.getSpriteAnimation(Assets.PLAYER_ONE_IDLE_DOWN);
		player2 = Assets.getSpriteAnimation(Assets.PLAYER_TWO_IDLE_DOWN);
		
		lightmap = new LightMap(Game.WIDTH, Game.HEIGHT, new Color(0F, 0F, 0F, 0.3F));
		light = new FlickeringLight(Game.WIDTH / 2, 38, 60, 62, 5);
		
		username = new GuiTextField(this, (Game.WIDTH / 2) - (112 / 2) - 57, 60 - 4);
		username.setText(game.getUsername());
		serverIP = new GuiTextField(this, (Game.WIDTH / 2) - (112 / 2) - 57, 90 - 6);
		serverIP.setMaxLength(64);
		serverIP.setText(ipText);
		password = new GuiTextField(this, (Game.WIDTH / 2) - (112 / 2) - 57, 120 - 8);
		password.setText(passwordText);
		
		buttons.add(username);
		buttons.add(serverIP);
		buttons.add(password);
		
		joinButton = new GuiTextButton(this, 1, 150, 62, Assets.getFont(Assets.FONT_NORMAL), "Join Server");
		
		keyTyped();
		
		buttons.add(joinButton);
		buttons.add(new GuiTextButton(this, 200, 150, 90, Assets.getFont(Assets.FONT_NORMAL), "Back"));
	}
	
	@Override
	public void drawScreen(Texture renderTo)
	{
		lightmap.clear();
		light.renderCentered(lightmap);
		lightmap.patch();
		
		renderTo.draw(lightmap, 0, 0);
		
		font.renderCentered(renderTo, Game.WIDTH / 2, 15, "Join a Server");
		
		font.renderCentered(renderTo, Game.WIDTH / 2 - 57, 50 - 4, "Username");
		font.renderCentered(renderTo, Game.WIDTH / 2 - 57, 80 - 6, "Server IP");
		font.renderCentered(renderTo, Game.WIDTH / 2 - 57, 110 - 8, "Server Pass");
		
		renderTo.draw(afroMan.getCurrentFrame(), (Game.WIDTH / 2) - 20, 30);
		renderTo.draw(player2.getCurrentFrame(), (Game.WIDTH / 2) + 4, 30);
	}
	
	@Override
	public void tick()
	{
		super.tick();
		
		light.tick();
		afroMan.tick();
		player2.tick();
		
		if (Game.instance().input.tab.isPressedFiltered())
		{
			if (username.isFocussed())
			{
				serverIP.setFocussed();
			}
			else if (serverIP.isFocussed())
			{
				password.setFocussed();
			}
			else
			{
				password.setFocussed(false);
			}
		}
	}
	
	@Override
	public void pressAction(int buttonID)
	{
		switch (buttonID)
		{
			case 200:
				Game.instance().setCurrentScreen(this.parentScreen);
				break;
		}
	}
	
	@Override
	public void releaseAction(int buttonID)
	{
		switch (buttonID)
		{
			case 1: // Join Server
				game.setUsername(this.username.getText());
				Game.instance().setCurrentScreen(new GuiConnectToServer(game, this));
				break;
		}
	}
	
	@Override
	public void keyTyped()
	{
		if (!this.username.getText().isEmpty() && !this.serverIP.getText().isEmpty())
		{
			this.joinButton.setEnabled(true);
		}
		else
		{
			this.joinButton.setEnabled(false);
		}
		
		game.setUsername(this.username.getText());
		ipText = this.serverIP.getText();
		passwordText = this.password.getText();
	}
}

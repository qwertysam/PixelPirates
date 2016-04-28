package ca.afroman.gui;

import java.awt.Color;

import ca.afroman.Game;
import ca.afroman.assets.Assets;
import ca.afroman.assets.Font;
import ca.afroman.assets.SpriteAnimation;
import ca.afroman.assets.Texture;
import ca.afroman.gfx.FlickeringLight;
import ca.afroman.gfx.LightMap;
import ca.afroman.packet.PacketRequestConnection;
import ca.afroman.server.GameServer;

public class GuiHostServer extends GuiScreen
{
	private Font font;
	private SpriteAnimation afroMan;
	private SpriteAnimation player2;
	private LightMap lightmap;
	private FlickeringLight light;
	
	private GuiTextField username;
	private GuiTextField password;
	
	private GuiTextButton joinButton;
	
	public GuiHostServer(Game game, GuiScreen parent)
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
		
		username = new GuiTextField(this, (Game.WIDTH / 2) - (112 / 2) - 57, 62);
		username.setText(game.getUsername());
		password = new GuiTextField(this, (Game.WIDTH / 2) - (112 / 2) - 57, 90);

		buttons.add(username);
		buttons.add(password);
		
		joinButton = new GuiTextButton(this, 1, 150, 62, Assets.getFont(Assets.FONT_NORMAL), "Host Server");
		joinButton.setEnabled(false);
		
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
		
		font.renderCentered(renderTo, Game.WIDTH / 2, 15, "Host A Server");
		
		font.renderCentered(renderTo, Game.WIDTH / 2 - 57, 62 - 10, "Username");
		font.renderCentered(renderTo, Game.WIDTH / 2 - 57, 90 - 10, "Server Pass");
		
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
			case 1: // Host Server
				game.setUsername(this.username.getText());
				
				if (!game.isHosting)
				{
					game.socketServer = new GameServer(this.password.getText());
					game.socketServer.start();
					game.isHosting = true;
				}
				
				game.socketClient.setServerIP(GameServer.IPv4_LOCALHOST);
				game.socketClient.sendPacket(new PacketRequestConnection(this.password.getText()));
				break;
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
			
		}
	}
	
	@Override
	public void keyTyped()
	{
		if (!this.username.getText().isEmpty())
		{
			this.joinButton.setEnabled(true);
		}
		else
		{
			this.joinButton.setEnabled(false);
		}
	}
}

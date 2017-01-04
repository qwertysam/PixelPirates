package ca.afroman.battle;

import ca.afroman.assets.AssetType;
import ca.afroman.assets.Assets;
import ca.afroman.assets.Texture;
import ca.afroman.client.ClientGame;
import ca.afroman.interfaces.ITickable;
import ca.afroman.resource.ServerClientObject;

public class BattleScene extends ServerClientObject implements ITickable
{
	private static final Texture bg = Assets.getTexture(AssetType.BATTLE_RUINS_BG);
	
	private BattlingEntityWrapper entity;
	private BattlingEntityWrapper player1;
	private BattlingEntityWrapper player2;
	
	public BattleScene(BattlingEntityWrapper entity, BattlingEntityWrapper player1, BattlingEntityWrapper player2)
	{
		super(entity.getFightingEnemy().isServerSide());
		
		this.entity = entity;
		this.player1 = player1;
		this.player2 = player2;
		
		if (!isServerSide())
		{
			ClientGame.instance().playMusic(Assets.getAudioClip(AssetType.AUDIO_BATTLE_MUSIC), true);
		}
	}
	
	public void render(Texture renderTo)
	{
		bg.render(renderTo, 0, 0);
		
		entity.render(renderTo);
		if (player1 != null) player1.render(renderTo);
		if (player2 != null) player2.render(renderTo);
	}
	
	@Override
	public void tick()
	{
		// TODO controls and shite
		// Jump
		if (ClientGame.instance().input().up.isPressed())
		{
			
		}
		// Duck
		if (ClientGame.instance().input().down.isPressed())
		{
			
		}
		
		entity.tick();
		if (player1 != null) player1.tick();
		if (player2 != null) player2.tick();
	}
}

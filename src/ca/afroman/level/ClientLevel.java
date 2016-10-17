package ca.afroman.level;

import java.awt.Color;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import ca.afroman.assets.Asset;
import ca.afroman.assets.AssetType;
import ca.afroman.assets.Assets;
import ca.afroman.assets.Font;
import ca.afroman.assets.Texture;
import ca.afroman.client.ClientGame;
import ca.afroman.entity.PlayerEntity;
import ca.afroman.entity.YComparator;
import ca.afroman.entity.api.ClientAssetEntity;
import ca.afroman.entity.api.Entity;
import ca.afroman.entity.api.Hitbox;
import ca.afroman.events.Event;
import ca.afroman.events.HitboxToggle;
import ca.afroman.events.HitboxTrigger;
import ca.afroman.events.TPTrigger;
import ca.afroman.game.Role;
import ca.afroman.gui.build.GuiFlickeringLightEditor;
import ca.afroman.gui.build.GuiGrid;
import ca.afroman.gui.build.GuiHitboxToggleEditor;
import ca.afroman.gui.build.GuiHitboxTriggerEditor;
import ca.afroman.gui.build.GuiTPTriggerEditor;
import ca.afroman.gui.build.GuiTileEditor;
import ca.afroman.interfaces.IRenderable;
import ca.afroman.interfaces.ITickable;
import ca.afroman.light.FlickeringLight;
import ca.afroman.light.LightMap;
import ca.afroman.light.PointLight;
import ca.afroman.log.ALogType;
import ca.afroman.option.Options;
import ca.afroman.packet.PacketAddFlickeringLight;
import ca.afroman.packet.PacketAddHitbox;
import ca.afroman.packet.PacketAddHitboxToggle;
import ca.afroman.packet.PacketAddPointLight;
import ca.afroman.packet.PacketAddTPTrigger;
import ca.afroman.packet.PacketAddTile;
import ca.afroman.packet.PacketAddTrigger;
import ca.afroman.packet.PacketRemoveLevelObject;
import ca.afroman.resource.Vector2DDouble;
import ca.afroman.resource.Vector2DInt;
import ca.afroman.util.ListUtil;
import ca.afroman.util.ShapeUtil;

public class ClientLevel extends Level
{
	private static final int MAX_TOOLTIP_TIME = (60 * 3); // Time in ticks
	private LightMap lightmap;
	private Vector2DDouble offset;
	
	private Font lightDebug;
	
	private BuildMode buildMode = BuildMode.TILE;
	
	private int timeOnTool = 0;
	
	// Tiles
	private Asset cursorAsset = null;
	public boolean showLayer0 = true;
	public boolean showLayer1 = true;
	public boolean showLayer2 = true;
	public boolean showLayer3 = true;
	public boolean showLayer4 = true;
	public boolean showLayer5 = true;
	public boolean showLayer6 = true;
	public GridSize grid = GridSize.MEDIUM;
	public byte editLayer = 0;
	
	// PointLights
	private int currentBuildLightRadius = 10;
	
	// FlickeringLights
	private int currentFlickerLightFlicker = 4;
	private double currentFlickerLightRadius = 4;
	private double lastFlickerLightRadius = currentFlickerLightRadius;
	public FlickeringLight flickerCursor = null;
	
	// HitBoxes and Triggers
	private Vector2DDouble hitbox1 = new Vector2DDouble(0, 0);
	private Vector2DDouble hitbox2 = new Vector2DDouble(0, 0);
	private int hitboxClickCount = 0;
	
	// Used for doing cleanup and setup of build modes
	private boolean lastIsBuildMode = false;
	
	public ClientLevel(LevelType type)
	{
		super(false, type);
		
		lightDebug = Assets.getFont(AssetType.FONT_NOBLE);
		offset = new Vector2DDouble(0, 0);
		lightmap = new LightMap(ClientGame.WIDTH, ClientGame.HEIGHT, LightMap.DEFAULT_AMBIENT);
	}
	
	private void cleanupBuildMode(BuildMode mode)
	{
		// Cleanup
		switch (mode)
		{
			case TILE:
				ClientGame.instance().setCurrentScreen(null);
				break;
			case LIGHT:
				ClientGame.instance().setCurrentScreen(null);
				break;
			case HITBOX:
				ClientGame.instance().setCurrentScreen(null);
				hitboxClickCount = 0;
				break;
			case TRIGGER:
				ClientGame.instance().setCurrentScreen(null);
				hitboxClickCount = 0;
				break;
			case HITBOX_TOGGLE:
				ClientGame.instance().setCurrentScreen(null);
				hitboxClickCount = 0;
				break;
			case FLICKERING_LIGHT:
				ClientGame.instance().setCurrentScreen(null);
				hitboxClickCount = 0;
				flickerCursor.removeFromLevel();
				break;
			case TP_TRIGGER:
				hitboxClickCount = 0;
				ClientGame.instance().setCurrentScreen(null);
				break;
		}
	}
	
	public Vector2DDouble getCameraOffset()
	{
		return offset;
	}
	
	// Only used for right clicks in Build Mode
	private HitboxToggle getHitboxToggle(Vector2DDouble pos)
	{
		for (Event event : getScriptedEvents())
		{
			if (event instanceof HitboxToggle)
			{
				if (event.getHitbox().contains(pos.getX(), pos.getY())) return (HitboxToggle) event;
			}
		}
		return null;
	}
	
	// Only used for right clicks in Build Mode
	private HitboxTrigger getHitboxTrigger(Vector2DDouble pos)
	{
		for (Event event : getScriptedEvents())
		{
			if (event instanceof HitboxTrigger)
			{
				if (event.getHitbox().contains(pos.getX(), pos.getY())) return (HitboxTrigger) event;
			}
		}
		return null;
	}
	
	public LightMap getLightMap()
	{
		return lightmap;
	}
	
	// Only used for right clicks in Build Mode
	private TPTrigger getTPTrigger(Vector2DDouble pos)
	{
		for (Event event : getScriptedEvents())
		{
			if (event instanceof TPTrigger)
			{
				if (event.getHitbox().contains(pos.getX(), pos.getY())) return (TPTrigger) event;
			}
		}
		return null;
	}
	
	// Deals with generic hitbox behaviour for build modes that use it
	private void hitboxBehaviour(BuildMode mode, boolean leftClick)
	{
		switch (mode)
		{
			default:
				break;
			case HITBOX:
				if (leftClick)
				{
					Rectangle2D box = ShapeUtil.pointsToRectangle(hitbox1, hitbox2);
					
					PacketAddHitbox pack = new PacketAddHitbox(this.getType(), new Hitbox(box.getX(), box.getY(), box.getWidth(), box.getHeight()));
					ClientGame.instance().sockets().sender().sendPacket(pack);
				}
				else
				{
					Hitbox box = this.getHitbox(screenToWorld(ClientGame.instance().input().getMousePos()));
					
					if (box != null)
					{
						PacketRemoveLevelObject pack = new PacketRemoveLevelObject(box.getID(), this.getType(), LevelObjectType.HITBOX);
						ClientGame.instance().sockets().sender().sendPacket(pack);
					}
				}
				break;
			case TRIGGER:
				if (leftClick)
				{
					Rectangle2D box = ShapeUtil.pointsToRectangle(hitbox1, hitbox2);
					
					PacketAddTrigger pack = new PacketAddTrigger(this.getType(), -1, (int) box.getX(), (int) box.getY(), (int) box.getWidth(), (int) box.getHeight());
					ClientGame.instance().sockets().sender().sendPacket(pack);
				}
				else
				{
					HitboxTrigger event = getHitboxTrigger(screenToWorld(ClientGame.instance().input().getMousePos()));
					
					if (event != null)
					{
						if (!(ClientGame.instance().getCurrentScreen() instanceof GuiHitboxTriggerEditor))
						{
							ClientGame.instance().setCurrentScreen(new GuiHitboxTriggerEditor(this, event.getID()));
						}
					}
				}
				break;
			case HITBOX_TOGGLE:
				if (leftClick)
				{
					Rectangle2D box = ShapeUtil.pointsToRectangle(hitbox1, hitbox2);
					
					ClientGame.instance().sockets().sender().sendPacket(new PacketAddHitboxToggle(this.getType(), -1, box.getX(), box.getY(), box.getWidth(), box.getHeight()));
				}
				else
				{
					HitboxToggle event = getHitboxToggle(screenToWorld(ClientGame.instance().input().getMousePos()));
					
					if (event != null)
					{
						if (!(ClientGame.instance().getCurrentScreen() instanceof GuiHitboxToggleEditor))
						{
							ClientGame.instance().setCurrentScreen(new GuiHitboxToggleEditor(this, event.getID()));
						}
					}
				}
				break;
			case FLICKERING_LIGHT:
				if (leftClick)
				{
					PacketAddFlickeringLight pack = new PacketAddFlickeringLight(this.getType(), flickerCursor);
					ClientGame.instance().sockets().sender().sendPacket(pack);
					
					if (buildMode == BuildMode.FLICKERING_LIGHT)
					{
						flickerCursor.setPosition(new Vector2DDouble(Double.MAX_VALUE / 2, Double.MAX_VALUE / 2));
						flickerCursor.removeFromLevel();
					}
				}
				else
				{
					FlickeringLight light = getFlickeringLight(screenToWorld(ClientGame.instance().input().getMousePos()));
					
					if (light != null)
					{
						PacketRemoveLevelObject pack = new PacketRemoveLevelObject(light.getID(), this.getType(), LevelObjectType.FLICKERING_LIGHT);
						ClientGame.instance().sockets().sender().sendPacket(pack);
					}
				}
				break;
			case TP_TRIGGER:
				if (leftClick)
				{
					Rectangle2D box = ShapeUtil.pointsToRectangle(hitbox1, hitbox2);
					
					PacketAddTPTrigger pack = new PacketAddTPTrigger(this.getType(), -1, box.getX(), box.getY(), box.getWidth(), box.getHeight());
					ClientGame.instance().sockets().sender().sendPacket(pack);
				}
				else
				{
					TPTrigger event = getTPTrigger(screenToWorld(ClientGame.instance().input().getMousePos()));
					
					if (event != null)
					{
						if (!(ClientGame.instance().getCurrentScreen() instanceof GuiTPTriggerEditor))
						{
							ClientGame.instance().setCurrentScreen(new GuiTPTriggerEditor(this, event.getID()));
						}
					}
				}
				break;
		}
		
	}
	
	private void loadBuildMode(BuildMode mode)
	{
		timeOnTool = 0;
		
		// Load new build mode
		switch (buildMode)
		{
			case TILE:
				if (cursorAsset == null) cursorAsset = Assets.getAsset(AssetType.fromOrdinal(0).getNextRenderable()).clone();
				ClientGame.instance().setCurrentScreen(new GuiTileEditor());
				break;
			case LIGHT:
				ClientGame.instance().setCurrentScreen(new GuiGrid());
				break;
			case HITBOX:
				ClientGame.instance().setCurrentScreen(new GuiGrid());
				break;
			case TRIGGER:
				
				break;
			case HITBOX_TOGGLE:
				
				break;
			case FLICKERING_LIGHT:
				if (flickerCursor == null) flickerCursor = new FlickeringLight(false, -1, new Vector2DDouble(0, 0), currentFlickerLightRadius, currentFlickerLightRadius = currentFlickerLightFlicker, 10);
				ClientGame.instance().setCurrentScreen(new GuiFlickeringLightEditor());
				break;
			case TP_TRIGGER:
				ClientGame.instance().setCurrentScreen(new GuiGrid());
				break;
		}
	}
	
	@SuppressWarnings("deprecation")
	public void render(Texture renderTo)
	{
		boolean isBuildMode = ClientGame.instance().isBuildMode();
		
		List<List<Entity>> tiles = getTiles();
		
		// Renders Tiles
		for (int i = 0; i <= 2; i++)
		{
			boolean draw = true;
			
			if (isBuildMode && buildMode == BuildMode.TILE)
			{
				switch (i)
				{
					case 0:
						draw = showLayer0;
						break;
					case 1:
						draw = showLayer1;
						break;
					case 2:
						draw = showLayer2;
						break;
				}
			}
			
			if (draw)
			{
				for (Entity tile : tiles.get(i))
				{
					// If it has a texture, render it
					if (tile instanceof ClientAssetEntity)
					{
						((ClientAssetEntity) tile).render(renderTo);
					}
				}
			}
		}
		
		List<Entity> entities = new ArrayList<Entity>();
		
		for (Entity entity : this.getEntities())
		{
			entities.add(entity);
		}
		for (Entity player : this.getPlayers())
		{
			entities.add(player);
		}
		
		if (showLayer6)
		{
			for (Entity dynamicTiles : this.getTiles((byte) 6))
			{
				entities.add(dynamicTiles);
			}
		}
		
		ListUtil.sort(entities, new YComparator());
		
		for (Entity entity : entities)
		{
			if (entity instanceof ClientAssetEntity) ((ClientAssetEntity) entity).render(renderTo);
		}
		
		// Renders Tiles
		for (int i = 3; i <= 5; i++)
		{
			boolean draw = true;
			
			if (isBuildMode && buildMode == BuildMode.TILE)
			{
				switch (i)
				{
					case 3:
						draw = showLayer3;
						break;
					case 4:
						draw = showLayer4;
						break;
					case 5:
						draw = showLayer5;
						break;
				}
			}
			
			if (draw)
			{
				for (Entity tile : tiles.get(i))
				{
					// If it has a texture, render it
					if (tile instanceof ClientAssetEntity) ((ClientAssetEntity) tile).render(renderTo);
				}
			}
		}
		
		if (Options.instance().isLightingOn())
		{
			// Draws all the lighting over everything else
			lightmap.clear();
			
			List<PointLight> lights = this.getLights();
			
			for (PointLight light : lights)
			{
				light.renderCentered(lightmap);
				
				if (ClientGame.instance().isHitboxDebugging())
				{
					int radius = (int) light.getRadius();
					Vector2DInt pos = worldToScreen(light.getPosition()).add(-radius, -radius);
					
					renderTo.drawFillRect(new Color(1F, 1F, 1F, 0.25F), new Color(1F, 1F, 1F, 0.05F), pos, radius * 2, radius * 2);
					
					String number = new StringBuilder().append(light.getID()).toString();
					
					pos.add(0 + radius, -4 + radius);
					lightDebug.renderCentered(renderTo, pos, number);
				}
			}
			
			// Draws the light on the cursor if there is one
			if (isBuildMode)
			{
				if (buildMode == BuildMode.LIGHT)
				{
					int radius = currentBuildLightRadius;
					Vector2DInt pos = ClientGame.instance().input().getMousePos().clone().add(-radius, -radius);
					lightmap.drawLight(pos, radius);
					
					if (ClientGame.instance().isHitboxDebugging())
					{
						renderTo.drawFillRect(new Color(1F, 1F, 1F, 0.25F), new Color(1F, 1F, 1F, 0.05F), pos, radius * 2, radius * 2);
					}
				}
				else if (buildMode == BuildMode.FLICKERING_LIGHT)
				{
					flickerCursor.renderCentered(lightmap);
					
					// if (ClientGame.instance().isHitboxDebugging())
					// {
					// renderTo.drawFillRect(new Color(1F, 1F, 1F, 0.25F), new Color(1F, 1F, 1F, 0.05F), pos, radius * 2, radius * 2);
					// }
				}
			}
			
			lightmap.patch();
			
			renderTo.draw(lightmap, LightMap.PATCH_POSITION);
		}
		
		if (ClientGame.instance().getCurrentScreen() instanceof GuiGrid)
		{
			// Draws the grid
			if (grid.getSize() > 0)
			{
				// The amount of extra lines to draw off the bottom and right sides of the screen to prevent any drawing loss
				int bleed = 2;
				int xOffset = (int) offset.getX() % grid.getSize(); // Gets the grid offsets so the grid draws to the screen with the world position in mind
				int yOffset = (int) offset.getY() % grid.getSize();
				
				Paint oldPaint = renderTo.getGraphics().getPaint();
				
				renderTo.getGraphics().setPaint(new Color(1F, 1F, 1F, 0.1F));
				
				// Vertical lines
				for (int i = 0; i < Math.ceil(ClientGame.WIDTH / (double) grid.getSize()) + bleed; i++)
				{
					int x = (i * grid.getSize()) - xOffset;
					renderTo.getGraphics().drawLine(x, 0, x, ClientGame.HEIGHT);
				}
				
				// Horizontal lines
				for (int i = 0; i < Math.ceil(ClientGame.HEIGHT / (double) grid.getSize()) + bleed; i++)
				{
					int y = (i * grid.getSize()) - yOffset;
					renderTo.getGraphics().drawLine(0, y, ClientGame.WIDTH, y);
				}
				
				renderTo.getGraphics().setPaint(oldPaint);
			}
		}
		
		// Draws out the hitboxes
		if (ClientGame.instance().isHitboxDebugging() || (buildMode == BuildMode.HITBOX && isBuildMode))
		{
			for (Hitbox box : this.getHitboxes())
			{
				Vector2DInt pos = worldToScreen(new Vector2DDouble(box.getX(), box.getY()));
				renderTo.drawFillRect(new Color(1F, 1F, 1F, 1F), new Color(1F, 1F, 1F, 0.3F), pos, (int) box.getWidth(), (int) box.getHeight());
			}
			
			for (Entity entity : this.getEntities())
			{
				for (Hitbox box : entity.hitboxInLevel())
				{
					Vector2DInt pos = worldToScreen(new Vector2DDouble(box.getX(), box.getY()));
					renderTo.drawFillRect(new Color(1F, 1F, 1F, 1F), new Color(1F, 1F, 1F, 0.3F), pos, (int) box.getWidth(), (int) box.getHeight());
				}
			}
			
			for (Entity entity : this.getPlayers())
			{
				for (Hitbox box : entity.hitboxInLevel())
				{
					Vector2DInt pos = worldToScreen(new Vector2DDouble(box.getX(), box.getY()));
					renderTo.drawFillRect(new Color(1F, 1F, 1F, 1F), new Color(1F, 1F, 1F, 0.3F), pos, (int) box.getWidth(), (int) box.getHeight());
				}
			}
		}
		
		// Draws out scripted events
		if (ClientGame.instance().isHitboxDebugging() || ((buildMode == BuildMode.TRIGGER || buildMode == BuildMode.HITBOX_TOGGLE || buildMode == BuildMode.TP_TRIGGER) && isBuildMode))
		{
			for (Event e : getScriptedEvents())
			{
				double x = e.getHitbox().getX();
				double y = e.getHitbox().getY();
				int width = (int) e.getHitbox().getWidth();
				int height = (int) e.getHitbox().getHeight();
				
				Vector2DInt pos = worldToScreen(new Vector2DDouble(x, y));
				
				if (e instanceof HitboxTrigger && (ClientGame.instance().isHitboxDebugging() || (buildMode == BuildMode.TRIGGER)))
				{
					renderTo.drawRect(new Color(0.3F, 0.3F, 1F, 1F), pos, width, height);// Blue
				}
				else if (e instanceof HitboxToggle && (ClientGame.instance().isHitboxDebugging() || (buildMode == BuildMode.HITBOX_TOGGLE)))
				{
					renderTo.drawRect(new Color(1F, 0.3F, 0.3F, 1F), pos, width, height);// Red
				}
				else if (e instanceof TPTrigger && (ClientGame.instance().isHitboxDebugging() || (buildMode == BuildMode.TP_TRIGGER)))
				{
					renderTo.drawRect(new Color(0.3F, 1F, 0.3F, 1F), pos, width, height);// Green
				}
			}
		}
		
		// Draws the building hitbox, cursor asset, the grid, and the tooltips
		if (isBuildMode)
		{
			// Draws whatever it is for the specific build mode, whether it be the asset on the cursor, or the hitbox being created
			if (buildMode == BuildMode.TILE)
			{
				if (cursorAsset != null && cursorAsset instanceof IRenderable) ((IRenderable) cursorAsset).render(renderTo, ClientGame.instance().input().getMousePos());
			}
			else if (buildMode == BuildMode.HITBOX && hitboxClickCount == 1)
			{
				Rectangle box = ShapeUtil.pointsToRectangle(worldToScreen(hitbox1), worldToScreen(hitbox2));
				
				renderTo.drawFillRect(new Color(1F, 1F, 1F, 1F), new Color(1F, 1F, 1F, 0.3F), new Vector2DInt((int) box.getX(), (int) box.getY()), (int) box.getWidth(), (int) box.getHeight());
			}
			else if (buildMode == BuildMode.TRIGGER && hitboxClickCount == 1)
			{
				Rectangle box = ShapeUtil.pointsToRectangle(worldToScreen(hitbox1), worldToScreen(hitbox2));
				
				renderTo.drawRect(new Color(0.3F, 0.3F, 1F, 1F), new Vector2DInt((int) box.getX(), (int) box.getY()), (int) box.getWidth(), (int) box.getHeight());// Blue
			}
			else if (buildMode == BuildMode.HITBOX_TOGGLE && hitboxClickCount == 1)
			{
				Rectangle box = ShapeUtil.pointsToRectangle(worldToScreen(hitbox1), worldToScreen(hitbox2));
				
				renderTo.drawRect(new Color(1F, 0.3F, 0.3F, 1F), new Vector2DInt((int) box.getX(), (int) box.getY()), (int) box.getWidth(), (int) box.getHeight());// Red
			}
			else if (buildMode == BuildMode.TP_TRIGGER && hitboxClickCount == 1)
			{
				Rectangle box = ShapeUtil.pointsToRectangle(worldToScreen(hitbox1), worldToScreen(hitbox2));
				
				renderTo.drawRect(new Color(0.1F, 1F, 0.3F, 1F), new Vector2DInt((int) box.getX(), (int) box.getY()), (int) box.getWidth(), (int) box.getHeight());// Green
			}
			else if (buildMode == BuildMode.FLICKERING_LIGHT && hitboxClickCount == 1)
			{
				Vector2DInt pos1 = worldToScreen(hitbox1);
				Vector2DInt pos2 = worldToScreen(hitbox2);
				
				// The change in x and y from first point to the cursor's point
				int dx = pos2.getX() - pos1.getX();
				int dy = pos2.getY() - pos1.getY();
				
				// Finds the amplitude to modify dx and dy by to achieve the x and y amplitudes for the smaller light flicker line
				double amp = currentFlickerLightFlicker / currentFlickerLightRadius;
				
				// Finds the the x and y amplitudes for the smaller light flicker line
				int cx = (int) (amp * dx);
				int cy = (int) (amp * dy);
				
				// Finds the x and y ordinates of the intermediate point based on the new cx and cy amplitudes
				int x = pos2.getX() - cx;
				int y = pos2.getY() - cy;
				
				Vector2DInt intermediatePoint = new Vector2DInt(x, y);
				
				Paint oldPaint = renderTo.getGraphics().getPaint();
				
				renderTo.getGraphics().setPaint(new Color(1F, 1F, 1F, 1F));
				renderTo.getGraphics().drawLine(pos1.getX(), pos1.getY(), intermediatePoint.getX(), intermediatePoint.getY());
				
				renderTo.getGraphics().setPaint(new Color(0.1F, 0.1F, 1F, 1F));
				renderTo.getGraphics().drawLine(intermediatePoint.getX(), intermediatePoint.getY(), pos2.getX(), pos2.getY());
				
				renderTo.getGraphics().setPaint(oldPaint);
			}
			
			if (timeOnTool < MAX_TOOLTIP_TIME)
			{
				String text1 = "";
				String text2 = "";
				String text3 = "";
				String text4 = "";
				
				int lines = 0;
				
				switch (buildMode)
				{
					case TILE:
						lines = 3;
						text2 = "Tiles";
						text3 = "Scroll to switch texture";
						break;
					case LIGHT:
						lines = 3;
						text2 = "Lights";
						text3 = "Scroll to change size";
						break;
					case HITBOX:
						lines = 3;
						text2 = "Hitboxes";
						text3 = "Click to place both corners";
						text4 = "Right click to cancel corner";
						break;
					case TRIGGER:
						lines = 4;
						text1 = "Triggers";
						text2 = "Click to place both corners";
						text3 = "Right click to cancel corner";
						text4 = "Right click box to edit";
						break;
					case HITBOX_TOGGLE:
						lines = 4;
						text1 = "Hitbox Toggle Revievers";
						text2 = "Click to place both corners";
						text3 = "Right click to cancel corner";
						text4 = "Right click box to edit";
						break;
					case FLICKERING_LIGHT:
						lines = 3;
						text2 = "Flickering Lights";
						text3 = "Click to place center and edge";
						text4 = "Scroll to adjust flicker";
						break;
					case TP_TRIGGER:
						lines = 4;
						text1 = "Teleport Triggers";
						text2 = "Click to place both corners";
						text3 = "Right click to cancel corner";
						text4 = "Right click box to edit";
						break;
				}
				
				if (lines == 4) Assets.getFont(AssetType.FONT_NOBLE).renderCentered(renderTo, new Vector2DInt(ClientGame.WIDTH / 2, ClientGame.HEIGHT - 46), text1);
				if (lines >= 3) Assets.getFont(lines == 3 ? AssetType.FONT_NOBLE : AssetType.FONT_BLACK).renderCentered(renderTo, new Vector2DInt(ClientGame.WIDTH / 2, ClientGame.HEIGHT - 36), text2);
				if (lines >= 2) Assets.getFont(lines == 2 ? AssetType.FONT_NOBLE : AssetType.FONT_BLACK).renderCentered(renderTo, new Vector2DInt(ClientGame.WIDTH / 2, ClientGame.HEIGHT - 26), text3);
				if (lines >= 1) Assets.getFont(lines == 1 ? AssetType.FONT_NOBLE : AssetType.FONT_BLACK).renderCentered(renderTo, new Vector2DInt(ClientGame.WIDTH / 2, ClientGame.HEIGHT - 16), text4);
			}
		}
	}
	
	public Vector2DDouble screenToWorld(Vector2DInt point)
	{
		return new Vector2DDouble(point.getX() + offset.getX(), point.getY() + offset.getY());
	}
	
	public void setCameraCenterInWorld(Vector2DDouble point)
	{
		offset.setPosition(point.getX() - ClientGame.WIDTH / 2, point.getY() - ClientGame.HEIGHT / 2);
	}
	
	@Override
	public void tick()
	{
		boolean newIsBuildMode = ClientGame.instance().isBuildMode();
		
		// Cleanup the selected build mode if build mode has been exited,
		// load if it has just been entered
		if (newIsBuildMode && !lastIsBuildMode)
		{
			loadBuildMode(buildMode);
		}
		else if (!newIsBuildMode && lastIsBuildMode)
		{
			cleanupBuildMode(buildMode);
		}
		
		lastIsBuildMode = newIsBuildMode;
		
		if (newIsBuildMode)
		{
			if (timeOnTool <= MAX_TOOLTIP_TIME) timeOnTool++;
			
			BuildMode lastBuildMode = buildMode;
			
			if (ClientGame.instance().input().e.isPressedFiltered())
			{
				timeOnTool = 0; // On change, reset tooltips
				
				buildMode = buildMode.getNext();
			}
			
			if (ClientGame.instance().input().q.isPressedFiltered())
			{
				timeOnTool = 0; // On change, reset tooltips
				
				buildMode = buildMode.getLast();
			}
			
			// If the build mode has changed, update the build modes
			if (lastBuildMode != buildMode)
			{
				cleanupBuildMode(lastBuildMode);
				loadBuildMode(buildMode);
			}
			
			boolean isShifting = ClientGame.instance().input().shift.isPressed();
			int speed = (isShifting ? 5 : 1);
			
			// Only permit movement if the user is outside of a GUI...
			boolean permitMovement = ClientGame.instance().getCurrentScreen() == null;
			
			// UNLESS it's an instance of the tile editor, or etc...
			if (ClientGame.instance().getCurrentScreen() instanceof GuiTileEditor) permitMovement = true;
			if (ClientGame.instance().getCurrentScreen() instanceof GuiGrid) permitMovement = true;
			
			if (permitMovement)
			{
				if (ClientGame.instance().input().up.isPressed())
				{
					offset.add(0, -speed);
				}
				if (ClientGame.instance().input().down.isPressed())
				{
					offset.add(0, speed);
				}
				if (ClientGame.instance().input().left.isPressed())
				{
					offset.add(-speed, 0);
				}
				if (ClientGame.instance().input().right.isPressed())
				{
					offset.add(speed, 0);
				}
			}
			
			switch (buildMode)
			{
				case TILE:
					if (ClientGame.instance().input().mouseLeft.isPressedFiltered())
					{
						Entity tileToAdd = new Entity(false, -1, cursorAsset.getAssetType(), screenToWorld(ClientGame.instance().input().getMousePos()).alignToGrid(grid));
						PacketAddTile pack = new PacketAddTile(editLayer, this.getType(), tileToAdd);
						ClientGame.instance().sockets().sender().sendPacket(pack);
					}
					
					if (ClientGame.instance().input().mouseRight.isPressedFiltered())
					{
						Entity tile = getTile(editLayer, screenToWorld(ClientGame.instance().input().getMousePos()));
						
						if (tile != null)
						{
							PacketRemoveLevelObject pack = new PacketRemoveLevelObject(tile.getID(), this.getType(), LevelObjectType.TILE);
							ClientGame.instance().sockets().sender().sendPacket(pack);
						}
					}
					
					if (ClientGame.instance().input().mouseWheelDown.isPressedFiltered())
					{
						cursorAsset.dispose();
						cursorAsset = Assets.getAsset(cursorAsset.getAssetType().getLastRenderable()).clone();
					}
					
					if (ClientGame.instance().input().mouseWheelUp.isPressedFiltered())
					{
						cursorAsset.dispose();
						cursorAsset = Assets.getAsset(cursorAsset.getAssetType().getNextRenderable()).clone();
					}
					
					if (cursorAsset instanceof ITickable) ((ITickable) cursorAsset).tick();
					break;
				case LIGHT:
					if (ClientGame.instance().input().mouseLeft.isPressedFiltered())
					{
						PointLight light = new PointLight(false, -1, screenToWorld(ClientGame.instance().input().getMousePos()).alignToGridCenter(grid), currentBuildLightRadius);
						
						ClientGame.instance().sockets().sender().sendPacket(new PacketAddPointLight(this.getType(), light));
					}
					
					if (ClientGame.instance().input().mouseRight.isPressedFiltered())
					{
						PointLight light = getLight(screenToWorld(ClientGame.instance().input().getMousePos()));
						
						if (light != null)
						{
							PacketRemoveLevelObject pack = new PacketRemoveLevelObject(light.getID(), this.getType(), LevelObjectType.POINT_LIGHT);
							ClientGame.instance().sockets().sender().sendPacket(pack);
						}
					}
					
					if (ClientGame.instance().input().mouseWheelDown.isPressedFiltered())
					{
						currentBuildLightRadius -= speed;
						if (currentBuildLightRadius < 2) currentBuildLightRadius = 2;
					}
					
					if (ClientGame.instance().input().mouseWheelUp.isPressedFiltered())
					{
						currentBuildLightRadius += speed;
					}
					break;
				case FLICKERING_LIGHT:
					if (hitboxClickCount == 1)
					{
						flickerCursor.setPosition(hitbox1);
						boolean flickerLightChange = false;
						
						// The change in x and y from first point to the cursor's point
						double dx = hitbox2.getX() - hitbox1.getX();
						double dy = hitbox2.getY() - hitbox1.getY();
						
						// Finds the length of the line (using pothagorean theorem) based on the given change in x and y
						currentFlickerLightRadius = Math.sqrt((dx * dx) + (dy * dy));
						
						if (currentFlickerLightRadius != lastFlickerLightRadius)
						{
							flickerCursor.setRadius(Math.max(currentFlickerLightRadius, 1));
							lastFlickerLightRadius = currentFlickerLightRadius;
							
							flickerLightChange = true;
						}
						
						if (ClientGame.instance().input().mouseWheelDown.isPressedFiltered())
						{
							currentFlickerLightFlicker -= speed;
							if (currentFlickerLightFlicker < 1) currentFlickerLightFlicker = 1;
							flickerLightChange = true;
						}
						
						if (ClientGame.instance().input().mouseWheelUp.isPressedFiltered())
						{
							currentFlickerLightFlicker += speed;
							flickerLightChange = true;
						}
						
						if (flickerLightChange)
						{
							flickerCursor.setRadius2(Math.max(currentFlickerLightRadius - currentFlickerLightFlicker, 1));
						}
					}
				case HITBOX:
				case TRIGGER:
				case HITBOX_TOGGLE:
				case TP_TRIGGER:
					if (ClientGame.instance().getCurrentScreen() instanceof GuiGrid || ClientGame.instance().getCurrentScreen() == null)
					{
						if (ClientGame.instance().input().mouseLeft.isPressedFiltered())
						{
							if (hitboxClickCount == 0)
							{
								hitboxClickCount = 1;
								hitbox1.setPosition(screenToWorld(ClientGame.instance().input().getMousePos())).add(1, 1);
								
								if (buildMode == BuildMode.FLICKERING_LIGHT)
								{
									flickerCursor.addToLevel(this);
								}
							}
							else if (hitboxClickCount == 1)
							{
								hitboxClickCount = 0;
								hitboxBehaviour(buildMode, true);
							}
						}
						
						if (ClientGame.instance().input().mouseRight.isPressedFiltered())
						{
							if (hitboxClickCount == 1)
							{
								hitboxClickCount = 0;
								
								if (buildMode == BuildMode.FLICKERING_LIGHT)
								{
									flickerCursor.setPosition(new Vector2DDouble(Double.MAX_VALUE / 2, Double.MAX_VALUE / 2));
									flickerCursor.removeFromLevel();
								}
							}
							else
							{
								hitboxBehaviour(buildMode, false);
							}
						}
					}
					break;
			}
			
			// Sets up the hitbox when it's been clicked
			if (hitboxClickCount > 0)
			{
				hitbox2.setPosition(screenToWorld(ClientGame.instance().input().getMousePos())); // .add(1, 1)
			}
		}
		else if (ClientGame.instance().getRole() == Role.SPECTATOR)
		{
			ClientGame game = ClientGame.instance();
			PlayerEntity pe = game.getPlayer(game.getSpectatingRole());
			if (pe != null)
			{
				setCameraCenterInWorld(pe.getPosition().clone().add(8, 8));
				
				if (game.input().right.isPressedFiltered())
				{
					Role role = game.getSpectatingRole();
					while ((role = role.getNext()) == Role.SPECTATOR);
					
					game.setSpectatingRole(role);
				}
				if (game.input().left.isPressedFiltered())
				{
					Role role = game.getSpectatingRole();
					while ((role = role.getLast()) == Role.SPECTATOR);
					
					game.setSpectatingRole(role);
				}
			}
			else
			{
				game.logger().log(ALogType.WARNING, "PlayerEntity for role " + game.getSpectatingRole() + " is null");
			}
		}
		
		if (Options.instance().isLightingOn())
		{
			List<PointLight> lights = this.getLights();
			
			for (PointLight light : lights)
			{
				light.tick();
			}
		}
		
		super.tick();
	}
	
	public Vector2DInt worldToScreen(Vector2DDouble point)
	{
		return new Vector2DInt((int) point.getX() - (int) offset.getX(), (int) point.getY() - (int) offset.getY());
	}
}

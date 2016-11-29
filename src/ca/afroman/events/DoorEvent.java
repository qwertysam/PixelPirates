package ca.afroman.events;

import java.util.List;

import ca.afroman.assets.AssetType;
import ca.afroman.assets.Assets;
import ca.afroman.assets.DrawableAsset;
import ca.afroman.entity.Tile;
import ca.afroman.entity.api.Direction;
import ca.afroman.level.api.Level;
import ca.afroman.resource.Vector2DDouble;

public class DoorEvent extends HitboxToggle
{
	Tile open;
	Tile closed;
	
	public DoorEvent(boolean isServerSide, Direction doorLooking, double x, double y, List<Integer> inTriggers, List<Integer> outTriggers)
	{
		super(isServerSide, x, y, 16 + (Math.abs(doorLooking.getYAmplitude()) * 16), 16 + (Math.abs(doorLooking.getXAmplitude()) * 16), inTriggers, outTriggers);
		
		if (!isServerSide)
		{
			DrawableAsset open;
			DrawableAsset closed;
			
			switch (doorLooking)
			{
				default:
				case UP:
					open = Assets.getDrawableAsset(AssetType.TILE_DOOR_UP_OPEN);
					closed = Assets.getDrawableAsset(AssetType.TILE_DOOR_UP_CLOSED);
					break;
				case DOWN:
					open = Assets.getDrawableAsset(AssetType.TILE_DOOR_DOWN_OPEN);
					closed = Assets.getDrawableAsset(AssetType.TILE_DOOR_DOWN_CLOSED);
					break;
				case LEFT:
					open = Assets.getDrawableAsset(AssetType.TILE_DOOR_LEFT_OPEN);
					closed = Assets.getDrawableAsset(AssetType.TILE_DOOR_LEFT_CLOSED);
					break;
				case RIGHT:
					open = Assets.getDrawableAsset(AssetType.TILE_DOOR_RIGHT_OPEN);
					closed = Assets.getDrawableAsset(AssetType.TILE_DOOR_RIGHT_CLOSED);
					break;
			}
			
			// TODO cannot move the Tile with this
			this.open = new Tile(Level.DEFAULT_DYNAMIC_TILE_LAYER_INDEX, true, open, new Vector2DDouble(x, y));
			this.closed = new Tile(Level.DEFAULT_DYNAMIC_TILE_LAYER_INDEX, true, closed, new Vector2DDouble(x, y));
		}
	}
	
	@Override
	public void addToLevel(Level newLevel)
	{
		if (level == newLevel) return;
		
		boolean wasEnabled = enabled;
		
		if (level != null)
		{
			setEnabled(false);
			
			if (!isServerSide())
			{
				// Removes any tiles from their levels (If any are active)
				open.removeFromLevel();
				closed.removeFromLevel();
			}
			
			level.getEvents().remove(this);
		}
		
		// Sets the new level
		level = newLevel;
		
		if (level != null)
		{
			if (!isServerSide())
			{
				// Puts any tiles back to the dynamic ayer of the new level
				open.setLayer(level.getDynamicLayer());
				closed.setLayer(level.getDynamicLayer());
			}
			
			setEnabled(wasEnabled);
			level.getEvents().add(this);
		}
	}
	
	@Override
	public void setEnabled(boolean isActive)
	{
		super.setEnabled(isActive);
		
		if (!isServerSide())
		{
			if (isActive)
			{
				open.removeFromLevel();
				closed.addToLevel(level);
			}
			else
			{
				closed.removeFromLevel();
				open.addToLevel(level);
			}
		}
	}
}
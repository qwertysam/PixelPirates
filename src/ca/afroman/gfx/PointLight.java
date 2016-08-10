package ca.afroman.gfx;

import java.awt.Color;

import ca.afroman.assets.AssetType;
import ca.afroman.entity.api.Entity;
import ca.afroman.level.ClientLevel;
import ca.afroman.level.Level;
import ca.afroman.util.IDCounter;

public class PointLight extends Entity
{
	protected Color colour;
	private double radius;
	
	private static IDCounter idCounter = new IDCounter();
	
	public static IDCounter getIDCounter()
	{
		return idCounter;
	}
	
	public PointLight(boolean isServerSide, int id, double x, double y, double radius)
	{
		this(isServerSide, id, x, y, radius, ColourUtil.TRANSPARENT);
	}
	
	public PointLight(boolean isServerSide, int id, double x, double y, double radius, Color colour)
	{
		super(isServerSide, id, AssetType.INVALID, x, y);
		
		this.colour = colour;
		this.radius = radius;
	}
	
	@Override
	public void tick()
	{
		
	}
	
	@SuppressWarnings("deprecation")
	public void renderCentered(LightMap renderTo)
	{
		if (level instanceof ClientLevel)
		{
			ClientLevel cLevel = (ClientLevel) this.level;
			
			renderTo.drawLight(cLevel.worldToScreenX(x) - getRadius(), cLevel.worldToScreenY(y) - getRadius(), getRadius(), colour);
		}
	}
	
	public double getRadius()
	{
		return radius;
	}
	
	public Color getColour()
	{
		return colour;
	}
	
	/**
	 * Removes an entity from their current level and puts them in another level.
	 * 
	 * @param level the new level.
	 */
	@Override
	public void addToLevel(Level newLevel)
	{
		if (level == newLevel) return;
		
		if (level != null)
		{
			level.getLights().remove(this);
		}
		
		// Sets the new level
		level = newLevel;
		
		if (level != null)
		{
			level.getLights().add(this);
		}
	}
}

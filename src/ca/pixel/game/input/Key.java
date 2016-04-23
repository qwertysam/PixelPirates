package ca.pixel.game.input;

import java.util.List;

public class Key
{
	private boolean pressed = false;
	private boolean filteredPress = false;
	private int[] keyEvents;
	
	public Key(List<Key> container, int... keyEvents)
	{
		this.keyEvents = keyEvents;
		
		container.add(this);
	}
	
	/**
	 * Does the same as the <b>isPressed()</b> method, but reverts to false as soon as it's used once.
	 */
	public boolean isPressedFiltered()
	{
		boolean toReturn = filteredPress;
		filteredPress = false;
		return toReturn;
	}
	
	public boolean isPressed()
	{
		return pressed;
	}
	
	public void update(int keyCode, boolean isPressed)
	{
		for (int key : keyEvents)
		{
			if (keyCode == key)
			{
				setPressed(isPressed);
				return;
			}
		}
	}
	
	private void setPressed(boolean isPressed)
	{
		filteredPress = (!pressed && isPressed);
		pressed = isPressed;
	}
}

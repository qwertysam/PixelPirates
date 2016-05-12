package ca.afroman.packet;

import ca.afroman.level.LevelType;

public class PacketInstantiateLevel extends Packet
{
	private LevelType levelType;
	
	public PacketInstantiateLevel(LevelType levelType)
	{
		super(PacketType.INSTANTIATE_LEVEL);
		
		this.levelType = levelType;
	}
	
	@Override
	public byte[] getData()
	{
		return (type.ordinal() + Packet.SEPARATOR + levelType.ordinal()).getBytes();
	}
}

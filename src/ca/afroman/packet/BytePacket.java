package ca.afroman.packet;

import java.nio.ByteBuffer;
import java.util.Arrays;

import ca.afroman.network.IPConnection;

public class BytePacket
{
	private PacketType type;
	private byte[] content;
	
	private IPConnection[] connections;
	private boolean mustSend;
	
	/**
	 * Parses a BytePacket from raw byte data.
	 * 
	 * @param rawData
	 * @param sender
	 */
	public BytePacket(byte[] rawData)
	{
		try
		{
			ByteBuffer buf = ByteBuffer.wrap(rawData);
			
			type = PacketType.fromOrdinal(buf.get(0));
			content = Arrays.copyOfRange(rawData, 1, rawData.length);
			connections = null; // new IPConnection[0];
		}
		catch (Exception e)
		{
			type = PacketType.INVALID;
			content = new byte[0];
			connections = null;
			// System.err.println("Well shit, this packet is fucked");
			// e.printStackTrace();
		}
	}
	
	/**
	 * Creates a new BytePacket to send.
	 * 
	 * @param type the type of packet being sent
	 * @param mustSend whether this packet must be pushed until the other end confirms of receiving it
	 * @param receiver the desired receiver of the packet
	 */
	public BytePacket(PacketType type, boolean mustSend, IPConnection... receivers)
	{
		this.type = type;
		this.mustSend = mustSend;
		connections = receivers;
		
		// if (receivers != null)
		// {
		// connections = new ArrayList<IPConnection>(receivers.length);
		//
		// for (IPConnection con : receivers)
		// {
		// connections.add(con);
		// }
		// }
		// else
		// {
		// connections = new ArrayList<IPConnection>();
		// }
	}
	
	public IPConnection[] getConnections()
	{
		return connections;
	}
	
	public byte[] getContent()
	{
		return content;
	}
	
	/**
	 * Gets the data from this Packet in a sendable form.
	 * <p>
	 * <b>WARNING:</b> Only intended for reading, DO NOT OVERRIDE.
	 * <p>
	 * Override <code>getUniqueData()</code> instead
	 *
	 * @return the data
	 */
	public final byte[] getData()
	{
		byte[] content = getUniqueData();
		byte[] toRet = new byte[content.length + 1];
		
		toRet[0] = (byte) getType().ordinal();
		
		for (int i = 1; i < toRet.length; i++)
			toRet[i] = content[i - 1];
		
		return toRet;
	}
	
	public PacketType getType()
	{
		return type;
	}
	
	/**
	 * Gets the data from this Packet in a sendable form.
	 * 
	 * @return the data
	 */
	public byte[] getUniqueData()
	{
		return new byte[] {};
	}
	
	public boolean mustSend()
	{
		return mustSend;
	}
	
	public void setConnections(IPConnection... con)
	{
		this.connections = con;
	}
}

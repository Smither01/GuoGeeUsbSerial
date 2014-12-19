package com.guogu.getway;


public class Util
{
	public static int Byte2Int(byte[] ipbyte, int offset)
	{
		if (null == ipbyte)
		{
			return 0;
		}
		int ipint = (ipbyte[0 + offset] & 0xff) << 24 | (ipbyte[1 + offset] & 0xff) << 16 
				| (ipbyte[2 + offset] & 0xff) << 8 | ipbyte[3 + offset] & 0xff;
		return ipint;
	}
	
	public static byte[] Int2Byte(int ipint)
	{
		byte[] ipbyte = new byte[4];
		ipbyte[0] = (byte) ((ipint >> 24) & 0xff);
		ipbyte[1] = (byte) ((ipint >> 16) & 0xff);
		ipbyte[2] = (byte) ((ipint >> 8) & 0xff);
		ipbyte[3] = (byte) (ipint & 0xff);
		return ipbyte;
	}
	
	public static short Byte2Short(byte[] portbyte, int offset)
	{
		if (null == portbyte)
		{
			return 0;
		}
		short portshort = (short) ((portbyte[0 + offset] & 0xff) << 8 | portbyte[1 + offset] & 0xff);
		return portshort;
	}
	
	public static byte[] Short2Byte(short portshort)
	{
		byte[] portbyte = new byte[2];
		portbyte[0] = (byte) ((portshort >> 8) & 0xff);
		portbyte[1] = (byte) (portshort & 0xff);
		return portbyte;
	}
}
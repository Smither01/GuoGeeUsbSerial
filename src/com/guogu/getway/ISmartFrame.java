package com.guogu.getway;

import java.net.DatagramPacket;
 
public class ISmartFrame
{
	byte hedaH;
	byte hedaL;
	short frameLength;
	byte sourceMac[];
	byte targetMac[];
	int ip;
	short port;
	byte mac[];
	short seq;
	byte cipherKey;
	byte reserve[];
	byte dev;
	byte ver;
	byte fun;
	short dataLength;
	short crc;
	byte data[];

	public ISmartFrame()
	{
		sourceMac = new byte[8];
		targetMac = new byte[8];
		mac = new byte[6];
		reserve = new byte[4];
		data = new byte[500];
	}
	
	public ISmartFrame(DatagramPacket Packet)
	{
		sourceMac = new byte[8];
		targetMac = new byte[8];
		mac = new byte[6];
		reserve = new byte[4];
		data = new byte[500];
		
		byte StrData[] = Packet.getData();
		ReadStrData(StrData);
	}
	
	public ISmartFrame(byte[] StrData)
	{
		sourceMac = new byte[8];
		targetMac = new byte[8];
		mac = new byte[6];
		reserve = new byte[4];
		data = new byte[400];
		
		ReadStrData(StrData);
		
	}
	
	public void ReadStrData(byte[] StrData)
	{
		if (null == StrData || 46 > StrData.length)
		{
			return;
		}
		hedaH = StrData[0];
		hedaL = StrData[1];
		frameLength = Util.Byte2Short(StrData, 2);
		System.arraycopy(StrData, 4, sourceMac, 0, 8);
		System.arraycopy(StrData, 12, targetMac, 0, 8);
		ip =  Util.Byte2Int(StrData, 20);
		port =  Util.Byte2Short(StrData, 24);
		System.arraycopy(StrData, 26, mac, 0, 6);
		seq =  Util.Byte2Short(StrData, 32);
		cipherKey = StrData[34];
		System.arraycopy(StrData, 35, reserve, 0, 4);
		dev = StrData[39];
		ver = StrData[40];
		fun = StrData[41];
		dataLength = Util.Byte2Short(StrData, 42);
		crc =  Util.Byte2Short(StrData, 44);
		if (dataLength > 0)
		{
			System.arraycopy(StrData, 46, data, 0, dataLength);
		}
	}
	
	public byte[] GetStrData()
	{
		if (0 >= frameLength)
		{
			return null;
		}
		byte[] StrData = new byte[frameLength];
		StrData[0] = hedaH;
		StrData[1] = hedaL;
		StrData[2] = (byte) ((frameLength >> 8) & 0xff);
		StrData[3] =  (byte) (frameLength & 0xff);
		System.arraycopy(sourceMac, 0, StrData, 4, 8);
		System.arraycopy(targetMac, 0, StrData, 12, 8);
		StrData[20] = (byte) ((ip  >> 24) & 0xff);
		StrData[21] = (byte) ((ip  >> 16) & 0xff);
		StrData[22] = (byte) ((ip  >> 8) & 0xff);
		StrData[23] = (byte) (ip & 0xff);
		StrData[24] = (byte) ((port >> 8) & 0xff);
		StrData[25] = (byte) (port & 0xff);
		System.arraycopy(mac, 0, StrData, 26, 6);
		StrData[32] = (byte) ((seq >> 8) & 0xff);
		StrData[33] = (byte) (seq & 0xff);
		StrData[34] = cipherKey;
		System.arraycopy(reserve, 0, StrData, 35, 4);
		StrData[39] = dev;
		StrData[40] = ver;
		StrData[41] = fun;
		StrData[42] = (byte) ((dataLength >> 8) & 0xff);
		StrData[43] = (byte) (dataLength & 0xff);
		StrData[44] = (byte) ((crc >> 8) & 0xff);
		StrData[45] = (byte) (crc & 0xff);
		if (dataLength > 0)
		{
			System.arraycopy(data, 0, StrData, 46, dataLength);
		}
		return StrData;
	}
	
	public int GetSize()
	{
		return frameLength;
	}
	
	public int GetIP()
	{
		return ip;
	}
	
	public int GetPort()
	{
		return (port & 0x0000ffff);
	}
	
	public byte GetDev()
	{
		return dev;
	}
		
	public byte GetVer()
	{
		return ver;
	}
	
	public byte GetFun()
	{
		return fun;
	}
	
	public byte[] GetSourceMac()
	{
		return sourceMac;
	}
	
	public byte[] GetData()
	{
		return data;
	}
	
	public boolean CheckError()
	{
		if ( !CheckAA55())
		{
			System.out.println("AA 55 check error");
			return false;
		}
		else if (frameLength - dataLength != 46)
		{
			System.out.println("frameLength dataLength check error");
			return false;
		}
		else if (!CheckCRC())
		{
			System.out.println("CRC check error");
			return false;
		}
		return true;
	}
	
	public boolean CheckAA55()
	{
		if ((hedaH & 0xff) != 0xAA || (hedaL & 0xff) != 0x55)
		{
			System.out.println(Integer.toHexString(hedaH));
			System.out.println(Integer.toHexString(hedaL));
			return false;
		}
		return true;
	}
	
	public boolean CheckCRC()
	{
		return true;
	}
	
	public void FillAA55()
	{
		this.hedaH = (byte) 0xAA;
		this.hedaL = (byte) 0x55;
	}
	public void FillIP(int paramip)
	{
		this.ip = paramip;
	}
	
	public void FillPort(short paramport)
	{
		this.port = paramport;
	}
	
	public void FillSourceMac(byte[] paramsourcemac)
	{
		this.sourceMac = paramsourcemac;
	}
	public void FillSeq(short paramSeq)
	{
		this.seq = paramSeq;
	}
	public void FillCRC()
	{
		return;
	}
	public void SetFrameLength(short Len)
	{
		frameLength = Len;
		if (Len > 46)
		{
			dataLength = (short) (Len - 46);
		}
	}
	public void SetVer(byte Ver)
	{
		ver = Ver;
	}
	public void SetFun(byte Fun)
	{
		fun = Fun;
	}
	public void SetDev(byte Dev)
	{
		dev = Dev;
	}
	public void SetSourceMac(byte[] Mac)
	{
		if (null != Mac)
		{
			System.arraycopy(Mac, 0, sourceMac, 0, 8);
		}
	}
	public void setData(byte[] dataArr)
	{
		if (null != dataArr)
		{
			System.arraycopy(dataArr, 0, data, 0,dataArr.length);
		}
	}
	
	public byte[] getTargetMac()
	{
		return targetMac;
	}
}
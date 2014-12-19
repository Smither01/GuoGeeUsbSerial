package com.guogu.getway;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.hardware.Camera.Size;
import android.util.Log;

public class Protocol {
	private boolean m_RemoteFlag;
	private int m_CtrlIP;
	private short m_CtrlPort;
	private int m_DataIP;
	private short m_DataPort;
	private byte[] m_GatewayMac;
	private ThreadKeepAlive m_ThreadAlive;
	private boolean m_ThreadAliveFlag;
	private Context context;

	private static Protocol instance;

	private volatile List<byte[]> dataPackage;
	
	private Protocol() {
		m_RemoteFlag = false;
		setCtrlIP(0);
		setCtrlPort((short) 0);
		setDataIP(0);
		setDataPort((short) 0);
		m_GatewayMac = null;
		m_ThreadAlive = null;
		m_ThreadAliveFlag = false;
		dataPackage = new ArrayList<byte[]>();
	}

	public static Protocol getInstance() {
		if (instance == null) {
			instance = new Protocol();
		}
		return instance;
	}

	public synchronized boolean start(Context contex) {
		System.out.println("protocol start() begin");
		boolean Flag = true;
		this.context = contex;
		System.out.println("protocol start() end : " + Flag);
		return Flag;
	}

	public synchronized byte[] getFirstDataByte() {
		if (dataPackage.size() > 0) {
			byte[] data = dataPackage.get(0);
			dataPackage.remove(0);
			return data;
		} else
			return new byte[] {};
	}

	public int getDataPackageSize() {
		return dataPackage.size();
	}

	public synchronized boolean startThread() {
		// System.out.println("protocol startThread() begin");
		Log.v("LZP", "protocol startThread() begin");
		boolean Flag = true;
		m_ThreadAliveFlag = true;
		m_ThreadAlive = new ThreadKeepAlive("keepalive");
		m_ThreadAlive.start();
		// System.out.println("protocol startThread() end : " + Flag);
		Log.v("LZP", "protocol startThread() end : " + Flag);
		return Flag;
	}

	public synchronized void stop() {
		System.out.println("protocol stop() begin");
		ClearGatewarMac();
		m_ThreadAliveFlag = false;
		m_ThreadAlive.interrupt();
		m_ThreadAlive = null;
		System.out.println("protocol stop() end");
	}

	/* 启动线程：心跳线程 */
	class ThreadKeepAlive extends Thread {
		
		private int localtion = 0;
		ThreadKeepAlive(String name) {
			super(name);// 调用父类带参数的构造方法
		}

		public void run() {
			// System.out.println(" is saled by "+Thread.currentThread().getName());
			while (m_ThreadAliveFlag) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// 若相应的USB串口不存在
				if (!m_ThreadAliveFlag
						|| SerialComm.getInstance().getUsbNotExist())
				// if (!m_ThreadAliveFlag ||
				// SerialComm.getInstance(context).getUsbNotExist())
				{
					m_GatewayMac = null;
					m_RemoteFlag = false;

					continue;
				}

				// 若相应的USB串口存在
				if (m_ThreadAliveFlag && null == m_GatewayMac
						&& (!SerialComm.getInstance().getUsbNotExist()))
				// if (m_ThreadAliveFlag && null == m_GatewayMac &&
				// (!SerialComm.getInstance(context).getUsbNotExist()))
				{
					ISmartFrame GetGateWayFrame = PackGetGatewayMac();
					Log.v("ASDFG", "From ThreadKeepAlive Write In to USB");
					dataPackage.add(GetGateWayFrame.GetStrData());
					continue;
				}
				while (m_ThreadAliveFlag && null != m_GatewayMac) {
					if(m_ThreadAliveFlag && (!SerialComm.getInstance().getUsbNotExist()))
					{
						//更改为每隔APP_CHECK_NODE_STATUS_TIME
						if(localtion <  SerialComm.getInstance().getNodeStatusList().size()){
							dataPackage.add(senRequestOfPoint(localtion));
							localtion++;
						}
						else{
							localtion = 0;
						}
					//	senRequestOfPoint();
					}
					if (!m_RemoteFlag) {
						Log.v("LZP", "! m_RemoteFlag server success");
						ISmartFrame GetRemoveSerFrame = PackGetRemoveSer();

						boolean flag = true;
						InetAddress address = null;
						try {
							address = InetAddress
									.getByName("regsrv1.guogee.com");
						} catch (UnknownHostException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							flag = false;
						}
						if (flag) {
							DatagramPacket SendPacket = new DatagramPacket(
									GetRemoveSerFrame.GetStrData(),
									GetRemoveSerFrame.GetSize(), address, 3001);
							NetComm.getInstance().send(SendPacket);
						}
					} else {
						// System.out.println("Remote server success");
						Log.v("LZP", "Remote server success");
						boolean flag = true;
						ISmartFrame KeepAliveFrame = PackKeepAlive();
						DatagramPacket SendPacket = null;
						try {
							SendPacket = new DatagramPacket(
									KeepAliveFrame.GetStrData(),
									KeepAliveFrame.GetSize(),
									InetAddress.getByAddress(Util
											.Int2Byte(getCtrlIP())),
									getCtrlPort());
						} catch (UnknownHostException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							flag = false;
						}
						if (flag) {
							NetComm.getInstance().send(SendPacket);
						}
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			// System.out.println("thread KeepAlive exit");
			Log.v("AAAAA", "thread KeepAlive exit");
		}
	}
	//返回遍历查询节点状态指令
	private byte[] senRequestOfPoint(int location)
	{
		List<SmartNode> nodes = SerialComm.getInstance().getNodeStatusList();
		if(location > nodes.size())
			return null;
		SmartNode node = nodes.get(location);
		byte[] task = new byte[46];
		System.arraycopy(new byte[]{(byte)0xAA,0x55,0x0,0x2E,0x0,0x00,0x00,0x0,0x0,0x00,0x00,0x00}, 0, task, 0, 12);
		System.arraycopy(node.getMac(), 0, task, 12, node.getMac().length);
		System.arraycopy(new byte[]{(byte) 0xC0,(byte) 0xA8,(byte) 0xC7,(byte) 0x8E,(byte) 0xD6,(byte) 0xED,0x0,0x0,0x0,0x0,0x0,0x0,(byte) 0x0,0x0,0x0,0x1,0x1,0x00,0x0,0x10,0x1,(byte) 0xFF,0x0,0x0,0x0,0x0}, 0, task, 20, 26);
		task[39] = node.getType();
		task[41] = (byte)0xFF;
		return task;
	}

	public synchronized void ClearGatewarMac() {
		m_GatewayMac = null;
		ClearRemoteSer();
	}

	public void ClearRemoteSer() {
		m_RemoteFlag = false;
		setCtrlIP(0);
		setCtrlPort((short) 0);
		setDataIP(0);
		setDataPort((short) 0);
	}

	public synchronized byte[] GetGatewayMac() {
		return m_GatewayMac;
	}

	public void DealNetFrame(ISmartFrame NetFrame) {
		if (!NetFrame.CheckError()) {
			return;
		}
//		Log.v("AAAAA", "dev:" + NetFrame.GetDev() + " ver:"
//				+ NetFrame.GetVer() + " fun:" + (NetFrame.GetFun() & 0xff));
		if (FilterKeepAliveRet(NetFrame)) {
			Log.v("ASDFG", "FilterKeepAliveRet");
			return;
		} else if (FilterCtlServerGet(NetFrame)) {
			Log.v("ASDFG", "FilterCtlServerGet");
			return;
		}

		//如果指令为查询所有节点状态
		if(NetFrame.GetDev() == SmartNode.PROTOCOL_TYPE_GATEWAY || NetFrame.GetDev() == SmartNode.PROTOCOL_TYPE_GATEWAY_SECOND)
		{
			if(NetFrame.GetFun() == 0x08){//若是返回节点状态包
				try{
					List<SmartNode>  nodeStatusList = SerialComm.getInstance().getNodeStatusList();
					int length = nodeStatusList.size();
					if(length == 0)
						return;
					//分包处理		
					for (int m = 0; m < length/80 +1; m++) {		
							byte[] data;
							if(length * 5 > 400)
								data = new byte[length*5];
							else
								data = new byte[400];
							for (int i = 0; i < data.length; i++) {
								data[i] = 0;
							}	
							for (int i = 0; i < length; i++) {
									data[i*5] = nodeStatusList.get(i).getShortMac()[0];
									data[i*5 + 1] = nodeStatusList.get(i).getShortMac()[1];
									data[i*5 + 2] = nodeStatusList.get(i).getType();
									data[i*5 + 3] = nodeStatusList.get(i).getStatus();
									data[i*5 + 4] = (byte)(( System.currentTimeMillis()/1000) - nodeStatusList.get(i).getTime());
								}
								//将数据重新封装
								byte[] getSourceByte = NetFrame.GetStrData();
							   byte[] queryByte = new byte[446];
						//    	byte[] queryByte = new byte[46 + length*5];
								System.arraycopy(getSourceByte, 0, queryByte, 0, 2);//头	
						//		byte[] queryByteLength = Util.Short2Byte((short)(0x2E + length*5));
								byte[] queryByteLength = Util.Short2Byte((short)(0x2E + 400));
								queryByte[2] = queryByteLength[0];//长度
								queryByte[3] = queryByteLength[1];//长度
								System.arraycopy(NetFrame.GetSourceMac(), 0, queryByte, 4, NetFrame.GetSourceMac().length);//源Mac
								System.arraycopy(new byte[]{0x0,0x12,0x4B,0x0,0x3,(byte)0x9F,(byte)0xBE,(byte)0xC9}, 0, queryByte, 12, 8);//目标Mac
								System.arraycopy(getSourceByte, 20, queryByte, 20, 21);//从网络源IP复制到版本号位置
								
								queryByte[34] = (byte)(length/80 +1);//帧长度
								queryByte[35] = (byte)(m+1);//当前帧序号
								
								queryByte[41] = 0x09;//返回指令
						//		byte[] dataLength = Util.Short2Byte((short)(length*5));//CRC
								byte[] dataLength = Util.Short2Byte((short)(400));//CRC
								queryByte[42] = dataLength[0];
								queryByte[43] = dataLength[1];	
								System.arraycopy(getSourceByte, 44, queryByte, 44, 2);
								if(data.length > 0)
									System.arraycopy(data, m*400, queryByte, 46, 400);
							/*	String sss = "";
								for (int i = 0; i < queryByte.length; i++) {
									sss += Integer.toHexString(queryByte[i] & 0xff) + " ";
								}
								Log.v("AAAAA", "sss:"+sss);*/
								this.DealSerialFrame(new ISmartFrame(queryByte));
					}
				}catch(Exception e)
				{
					Log.v("Protocol","Protocol:"+e.toString());
				}
				
				return;
			}
		}
	/*	byte[] tmp = NetFrame.GetStrData();
		String StrHex = " ";
		for (int i = 0; i < NetFrame.GetSize(); i++)
		{
			StrHex += Integer.toHexString(tmp[i] & 0xff);
			StrHex += " ";
		}
		StrHex = StrHex.toUpperCase();
		// System.out.println(StrHex);
		Log.v("ASDFG", "Write into USB:" + StrHex);*/
		// Log.v("LZP", "Write into USB");
		// 写入USB接口中
		// if(!SerialComm.getInstance(context).getUsbNotExist())
		if (!SerialComm.getInstance().getUsbNotExist()) {
			dataPackage.add(NetFrame.GetStrData());
			Log.v("ASDFG", "dataPackage Size :"+dataPackage.size());
		} else {
			if (dataPackage.size() > 0)
				dataPackage.clear();
			if(SerialComm.getInstance().getNodeStatusList().size() > 0)
				SerialComm.getInstance().setEmptyNodeList();
		}
		// SerialComm.getInstance(context).write(NetFrame.GetStrData(),
		// NetFrame.GetSize());
		return;
	}

	public void DealSerialFrame(ISmartFrame SerialFrame) {
		System.out.println("DealSerialFrame");
		if (!SerialFrame.CheckError()) {
			return;
		}
		if (FilterGatewayMac(SerialFrame)) {
			return;
		}
		if (0 == SerialFrame.GetIP()) {
			System.out.println("SerialFrame.GetIP :" + SerialFrame.GetIP());
			return;
		}
		try {
			System.out.println("SendPacket begin");
			System.out.println("IP:" + Util.Int2Byte(SerialFrame.GetIP()));
			System.out.println("Port:" + SerialFrame.GetPort());
			DatagramPacket SendPacket = new DatagramPacket(
					SerialFrame.GetStrData(),
					SerialFrame.GetSize(),
					InetAddress.getByAddress(Util.Int2Byte(SerialFrame.GetIP())),
					SerialFrame.GetPort());
			Log.v("ASDFG", "SendPacket OK");
			NetComm.getInstance().send(SendPacket);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public boolean FilterCtlServerGet(ISmartFrame NetFrame) {
		if (0x00 != (NetFrame.GetDev() & 0xff)
				|| 0x02 != (NetFrame.GetFun() & 0xff)) {
			return false;
		}
		byte[] NetFrameData = NetFrame.GetData();
		setCtrlIP(Util.Byte2Int(NetFrameData, 0));
		setCtrlPort(Util.Byte2Short(NetFrameData, 4));
		setDataIP(Util.Byte2Int(NetFrameData, 6));
		setDataPort(Util.Byte2Short(NetFrameData, 10));
		m_RemoteFlag = true;
		System.out.println("filte get romote server ");
		return true;
	}

	public boolean FilterKeepAliveRet(ISmartFrame NetFrame) {
		if (0x00 != (NetFrame.GetDev() & 0xff)
				|| 0x04 != (NetFrame.GetFun() & 0xff)) {
			return false;
		}
		System.out.println("filte keep alive ret");
		return true;
	}

	public boolean FilterGatewayMac(ISmartFrame NetFrame) {
		if (0x00 != (NetFrame.GetDev() & 0xff)
				|| 0xfe != (NetFrame.GetFun() & 0xff)) {
			return false;
		}
		if (null != m_GatewayMac) {
			return false;
		} else {
			m_GatewayMac = new byte[8];
			System.arraycopy(NetFrame.GetSourceMac(), 0, m_GatewayMac, 0, 8);
		}

		String StrHexRead = " ";
		for (int i = 0; i < 8; i++) {
			StrHexRead += Integer.toHexString(m_GatewayMac[i] & 0xff);
			StrHexRead += " ";
		}
		System.out.println("GateMac : " + StrHexRead);

		System.out.println("filte gatemac success");
		return true;
	}

	public ISmartFrame PackGetGatewayMac() {
		ISmartFrame GetGatewayMac = new ISmartFrame();
		GetGatewayMac.FillAA55();
		GetGatewayMac.SetFrameLength((short) 46);
		GetGatewayMac.SetDev((byte) 0x00);
		GetGatewayMac.SetVer((byte) 0x01);
		GetGatewayMac.SetFun((byte) 0xff);
		GetGatewayMac.FillCRC();

		System.out.println("pack getgatemac");
		return GetGatewayMac;
	}

	public ISmartFrame PackKeepAlive() {
		ISmartFrame KeepAlive = new ISmartFrame();
		KeepAlive.FillAA55();
		KeepAlive.SetFrameLength((short) 46);
		if (null != m_GatewayMac) {
			KeepAlive.SetSourceMac(m_GatewayMac);
		}
		KeepAlive.SetDev((byte) 0x00);
		KeepAlive.SetVer((byte) 0x00);
		KeepAlive.SetFun((byte) 0x03);
		KeepAlive.FillCRC();

		return KeepAlive;
	}

	public ISmartFrame PackGetRemoveSer() {
		ISmartFrame GetRemoveSer = new ISmartFrame();
		GetRemoveSer.FillAA55();
		GetRemoveSer.SetFrameLength((short) 46);
		if (null != m_GatewayMac) {
			GetRemoveSer.SetSourceMac(m_GatewayMac);
		}
		GetRemoveSer.SetDev((byte) 0x00);
		GetRemoveSer.SetVer((byte) 0x01);
		GetRemoveSer.SetFun((byte) 0x01);
		GetRemoveSer.FillCRC();

		return GetRemoveSer;
	}

	public int getCtrlIP() {
		return m_CtrlIP;
	}

	public void setCtrlIP(int ctrlIP) {
		m_CtrlIP = ctrlIP;
	}

	public int getCtrlPort() {
		return (m_CtrlPort & 0x0000ffff);
	}

	public void setCtrlPort(short ctrlPort) {
		m_CtrlPort = ctrlPort;
	}

	public int getDataIP() {
		return m_DataIP;
	}

	public void setDataIP(int dataIP) {
		m_DataIP = dataIP;
	}

	public int getDataPort() {
		return (m_DataPort & 0x0000ffff);
	}

	public void setDataPort(short dataPort) {
		m_DataPort = dataPort;
	}
}

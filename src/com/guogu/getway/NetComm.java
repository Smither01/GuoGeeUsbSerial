package com.guogu.getway;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;



public class NetComm
{
	private DatagramSocket m_socket;
	ThreadNetComm m_ThreadNC;
	private boolean m_ThreadNCFlag;
	
	
	private static NetComm instance;
	public static NetComm getInstance()
	{
		if (instance == null) {
			instance = new NetComm();
		}
		return instance;
	}
	private NetComm()
	{
		m_socket = null;
		m_ThreadNC = null;
		m_ThreadNCFlag = false;
	}
	
	public synchronized boolean start(Context Context)
	{
		System.out.println("netcomm start() begin");
		boolean Flag = true;
		try
		{
			m_socket = new DatagramSocket(3000);
		}catch(Exception e)
		{
			System.out.println("can not listen to:"+e);
			Flag = false;
		}
		System.out.println("netcomm start() end : " + Flag);

		return Flag;
	}
	
	public synchronized boolean startThread()
	{
		Log.v("LZP", "netcomm startThread() begin");
		boolean Flag = true;
		m_ThreadNCFlag = true;
		m_ThreadNC = new ThreadNetComm("netcomm");
		m_ThreadNC.start();
		Log.v("LZP", "netcomm startThread() end : " + Flag);
		return Flag;
	}
	
	public synchronized void stop()
	{
		System.out.println("netcomm stop() begin");
		m_ThreadNCFlag = false;
		m_ThreadNC.interrupt();
		m_ThreadNC.interrupt();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		m_ThreadNC.interrupt();
		m_ThreadNC.interrupt();

		m_socket.close();
		m_socket = null;
		m_ThreadNC = null;
		System.out.println("netcomm stop() end ");
	}
	
	public synchronized void send(DatagramPacket sendPacket)
	{
		if (null == m_socket)
		{
			return;
		}
		try {
			m_socket.send(sendPacket);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.v("AAAAA", "NetComm send:"+e.toString());
		} 
		System.out.println("send");
	}
	
	class ThreadNetComm extends Thread
	{
		ThreadNetComm(String name){
	        super(name);//调用父类带参数的构造方法
	    }
		byte[] arb = new byte[1024];
		DatagramPacket RecvPacket = new DatagramPacket(arb, arb.length);
	    public void run(){
	   	Log.v("LZP", "ThreadNetComm");
	        //System.out.println(" is saled by "+Thread.currentThread().getName());
	        while (m_ThreadNCFlag)
	        {
	        	boolean Errflag = false;
	        	try {
	        	 	Log.v("LZP", "m_socket.receive(RecvPacket)");
	        		m_socket.receive(RecvPacket);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
			//		System.out.println("m_ThreadNCFlag:" + m_ThreadNCFlag);
					Log.v("LZP", "m_ThreadNCFlag:" + m_ThreadNCFlag);	
					Errflag = true;
				}
	        	if (Errflag)
	        	{
	        		//异常退出，不处理，直接返回
	        		continue;
	        	}
	        //	System.out.println("recv");
	     //   	Log.v("LZP","recv");
	        	if (46 > RecvPacket.getLength())
	        	{
	        		continue;
	        	}
	        	
	        	ISmartFrame RecvFrame = new ISmartFrame(RecvPacket);
	        	RecvFrame.FillIP( Util.Byte2Int(RecvPacket.getAddress().getAddress(), 0) );
	        	RecvFrame.FillPort((short) RecvPacket.getPort());
	        	
	    //		System.out.println(StrHex);
		//        Log.v("LZP","StrHex:"+StrHex);
	        	Protocol.getInstance().DealNetFrame(RecvFrame);
	        }
	    //    System.out.println("thread netcomm exit");
	        Log.v("LZP","thread netcomm exit");
	        
	    }
	    
	}

}
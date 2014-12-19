package com.guogu.getway;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;

public class PackageReceiveService extends Service{

	private NetComm netcomm;
	private Protocol protocol;

	@Override
	public void onCreate() {
		super.onCreate();
		netcomm = NetComm.getInstance();
		protocol = Protocol.getInstance();
		if(netcomm.start(this))
			netcomm.startThread();
		if(protocol.start(this))
			protocol.startThread();
		SerialComm serialComm = SerialComm.getInstance(this);
		
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		super.onStartCommand(intent, flags, startId);
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		NetComm.getInstance().stop();
		Protocol.getInstance().stop();
		SerialComm.getInstance().stopUSB();
	}
}

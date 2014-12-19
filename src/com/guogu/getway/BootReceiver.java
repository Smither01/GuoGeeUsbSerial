package com.guogu.getway;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver{

	/*@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		Log.v("LZP", "Start Service");
		if (intent.getAction().equals(Context.CONNECTIVITY_CHANGE))
		{
			Intent startService = new Intent(context,PackageReceiveService.class);
			startService.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 
			context.startService(startService);
		}
		
		
	}*/
	
	@Override 
    public void onReceive(Context context, Intent intent) { 
        ConnectivityManager connectivityManager= 
        (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE); 
        if (connectivityManager!=null) { 
            NetworkInfo [] networkInfos=connectivityManager.getAllNetworkInfo(); 
            for (int i = 0; i < networkInfos.length; i++) { 
                State state=networkInfos[i].getState(); 
                if (NetworkInfo.State.CONNECTED==state) { 
                    Log.v("LZP", "Network is ok");
                    Intent startService = new Intent(context,PackageReceiveService.class);
                    
            		startService.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 
            		context.startService(startService);
                } 
            } 
        }
       
	}

}

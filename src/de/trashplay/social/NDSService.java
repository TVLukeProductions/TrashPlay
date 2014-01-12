package de.trashplay.social;

import java.io.IOException;
import java.net.ServerSocket;

import de.trashplay.main.TrashPlayConstants;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class NDSService  extends Service{

	ServerSocket mServerSocket;
    //static RegistrationListener mRegistrationListener;
    String mServiceName;
    //static NsdManager mNsdManager;

	public static final String TAG = TrashPlayConstants.TAG;
	public static final String PREFS_NAME = TrashPlayConstants.PREFS_NAME;
     
	@Override
	public IBinder onBind(Intent arg0) 
	{
		// TODO Auto-generated method stub
		return null;
	}
	
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) 
    {
            super.onStartCommand(intent, flags, startId);
            Log.e(TAG, "START NSD SERVICE");
            int port = getAvaliablePort();
            //registerService(port);
            return START_STICKY;
    }
    
    @Override
    public void onCreate() 
    {
            super.onCreate();
            Log.e(TAG, "onCreate()");
    }
    
    @Override
    public void onDestroy()
    {
        tearDown();
        super.onDestroy();
        Log.d(TAG, "Stop... Destroy!");
    }
    
    public void registerService(int port) 
    {
       /** Log.e(TAG, "REGISTER STARTS");
        // Create the NsdServiceInfo object, and populate it.
        NsdServiceInfo serviceInfo  = new NsdServiceInfo();

        // The name is subject to change based on conflicts
        // with other services advertised on the same network.
        
        serviceInfo.setServiceName("DynamixBridge_"+port);
        serviceInfo.setServiceType("_http._tcp.");
        serviceInfo.setPort(port);
        
        initializeRegistrationListener();
        
        mNsdManager = (NsdManager) this.getSystemService(Context.NSD_SERVICE);
        
        mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
        
        Log.e(TAG, "REGISTER DONE... WHAT NOW?");**/
    }
    
    public int getAvaliablePort() 
    {
        Log.e(TAG, "I AM SEARCHING FOR AN AVALIABLE PORT");
        int mLocalPort=8889;
        // Initialize a server socket on the next available port.
        try 
        {
        	mServerSocket = new ServerSocket(0);
        } 
        catch (IOException e) 
        {
        	// TODO Auto-generated catch block
        	//e.printStackTrace();
        }
        Log.e(TAG, "THE CHOSEN PORT IS");
        // Store the chosen port.
        mLocalPort = mServerSocket.getLocalPort();
        Log.e(TAG, "->"+mLocalPort);
        return mLocalPort;
    }
    
    public void initializeRegistrationListener() 
    {
      /**  mRegistrationListener = new NsdManager.RegistrationListener() 
        {

            @Override
            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) 
            {
                    Log.e(TAG, "ON NSD SERVICE REGISTERED");
                // Save the service name.  Android may have changed it in order to
                // resolve a conflict, so update the name you initially requested
                // with the name Android actually used.
                mServiceName = NsdServiceInfo.getServiceName();
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) 
            {
                // Registration failed!  Put debugging code here to determine why.
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0) 
            {
                // Service has been unregistered.  This only happens when you call
                // NsdManager.unregisterService() and pass in this listener.
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) 
            {
                // Unregistration failed.  Put debugging code here to determine why.
            }
        };**/
    }
    
     // NsdHelper's tearDown method
	public static void tearDown() 
	{
	       /** mNsdManager.unregisterService(mRegistrationListener);**/
	}
    
}

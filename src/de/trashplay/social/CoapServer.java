package de.trashplay.social;

import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Field;

import android.util.Log;
import de.trashplay.main.TrashPlayConstants;
import de.uniluebeck.itm.ncoap.application.server.CoapServerApplication;
import de.uniluebeck.itm.ncoap.application.server.webservice.WebService;
import de.uniluebeck.itm.ncoap.application.server.webservice.WellKnownCoreResource;

public class CoapServer extends CoapServerApplication
{
	public static final String TAG = TrashPlayConstants.TAG;
	public static final String PREFS_NAME = TrashPlayConstants.PREFS_NAME;
	
	public CoapServer()
	{
		super();
	}
	
	public CoapServer(int port)
	{
		super(port);
		Log.d(TAG, "CoapServer...");
	}
	
	protected ConcurrentHashMap<String, WebService> getRegisteredServices() 
    {
		Log.d(TAG, "getRegisteredServices");
        try 
        {
            Field privateStringField = getClass().getDeclaredField("registeredServices");
            return (ConcurrentHashMap<String, WebService>) privateStringField.get(this);
        } 
        catch (NoSuchFieldException e) 
        {
            e.printStackTrace();
        } 
        catch (IllegalAccessException e) 
        {
            e.printStackTrace();
        }
        return new ConcurrentHashMap<String, WebService>();
    }

    public void refreshRootService() 
    {
        removeService("/.well-known/core");
        registerService(new WellKnownCoreResource(getRegisteredServices()));
    }
}

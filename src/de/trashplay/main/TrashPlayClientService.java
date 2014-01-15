package de.trashplay.main;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

import org.jboss.netty.buffer.ChannelBuffer;

import de.uniluebeck.itm.ncoap.application.client.CoapClientApplication;
import de.uniluebeck.itm.ncoap.application.client.CoapResponseProcessor;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.EmptyAcknowledgementProcessor;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.InternalEmptyAcknowledgementReceivedMessage;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.InternalRetransmissionTimeoutMessage;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.RetransmissionTimeoutProcessor;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.InvalidHeaderException;
import de.uniluebeck.itm.ncoap.message.InvalidMessageException;
import de.uniluebeck.itm.ncoap.message.MessageType;
import de.uniluebeck.itm.ncoap.message.MessageCode;
import de.uniluebeck.itm.ncoap.message.options.ContentFormat;
import de.uniluebeck.itm.ncoap.message.options.InvalidOptionException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

public class TrashPlayClientService extends Service 
{

	public static TrashPlayClientService ctx=null;
	public static final String TAG = TrashPlayConstants.TAG;
	public static final String PREFS_NAME = TrashPlayConstants.PREFS_NAME;
	
	String TrashServerIP="192.168.1.103:5683";
	
	CoapClientApplication client;
	
	@Override
	public IBinder onBind(Intent intent) 
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
    public int onStartCommand(Intent intent, int flags, int startId) 
    {
		if(MainActivity.ctx!=null)
		{
			ctx=this;
			start();
			return START_STICKY;
		}
		else
		{
			stopSelf();
			return START_NOT_STICKY;
		}
    }
	
	@Override
	public void onCreate() 
	{
		Log.d(TAG, "Client Service on Create");
		super.onCreate();
	}
	
	@Override
    public void onDestroy() 
    {
		ctx=null;
        super.onDestroy();
    }
	
	private void start()
	{
		final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		TrashServerIP = settings.getString("ServerIP", "");
		 client = new CoapClientApplication();
		    new Thread(new Runnable() 
	    	{
	    	    public void run() 
	    	    {
					 //try 
					 //{
						 if(!TrashServerIP.equals(""))
						 {
							//URI targetURI = new URI ("coap://"+TrashServerIP+":5683/trashplayer");
							//CoapRequest coapRequest = new CoapRequest(MessageType.Name.CON, MessageCode.Name.GET, targetURI);
							//coapRequest.setAccept(ContentFormat.Name.APP_JSON);
							//coapRequest.setObserve(true);
					        //client.writeCoapRequest(coapRequest, new SimpleResponseProcessor());
					            
							//URI targetURI = new URI ("coap://192.168.1.103:5683/TrashPlayer");
							//CoapRequest coapRequest = new CoapRequest(MessageType.Name.CON, MessageCode.Name.GET, targetURI);
							//coapRequest.setObserveOptionRequest();
							//String r ="{\n";
							//r=r+"\"command\": \"loudDown\",\n";
							//r=r+"}";
							//byte[] bytes = r.getBytes();
				            //coapRequest.setContent(bytes);
				            //client.writeCoapRequest(coapRequest, new SimpleResponseProcessor());
						 }
					//} 
					/** catch (URISyntaxException e) 
					 {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} 
					 catch (InvalidOptionException e) 
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					} 
					 catch (UnknownHostException e) 
					 {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} 
					 catch (InvalidHeaderException e) 
					 {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} 
					 
					 //catch (InvalidMessageException e) 
					 //{
					//	// TODO Auto-generated catch block
					//	e.printStackTrace();
					//} **/
				}
	    	}).start();
	}
	
	public class SimpleResponseProcessor implements CoapResponseProcessor, EmptyAcknowledgementProcessor, RetransmissionTimeoutProcessor 
    {

            
            @Override
            public void processCoapResponse(CoapResponse coapResponse) 
            {
                Log.d(TAG, "Received Response: " + coapResponse);
                Log.d(TAG, "MAX AGE->"+coapResponse.getMaxAge());
                if(coapResponse.getContent()!=null)
                {
                	ChannelBuffer paylo = coapResponse.getContent();
                    byte[] paylobyte = paylo.array();
                    String strpay = new String(paylobyte);
                    Log.d(TAG, "PAYLOAD->"+strpay);
                    Log.d(TAG, "X");
	                
                }
                else
                {
                    Log.d(TAG, "NO PAYLOAD");
                }
                return;
            }
            
            @Override
            public void processEmptyAcknowledgement(InternalEmptyAcknowledgementReceivedMessage message) 
            {
                    Log.d(TAG, "Received empty ACK: " + message);
            }
            
            @Override
            public void processRetransmissionTimeout(InternalRetransmissionTimeoutMessage timeoutMessage) 
            {
                    Log.d(TAG, "Transmission timed out: " + timeoutMessage);
            }
    }

	public void stopTrashPlayer() 
	{

		final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		TrashServerIP = settings.getString("ServerIP", "");
		 client = new CoapClientApplication();
		    new Thread(new Runnable() 
	    	{
	    	    public void run() 
	    	    {
					 try 
					 {
						 if(!TrashServerIP.equals(""))
						 {
							Log.d(TAG, "try to kill the player...");
							URI targetURI = new URI ("coap://"+TrashServerIP+":5683/trashplayer");
							CoapRequest coapRequest = new CoapRequest(MessageType.Name.CON, MessageCode.Name.POST, targetURI);
							String r ="{\n";
							r=r+"\"command\": \"stop\",\n";
							r=r+"}";
							byte[] bytes = r.getBytes();
				            coapRequest.setContent(bytes, ContentFormat.Name.APP_JSON);
					        client.writeCoapRequest(coapRequest, new SimpleResponseProcessor());
						 }
				      } 
					 catch (URISyntaxException e) 
					 {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} 
					 catch (InvalidOptionException e) 
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					} 
					 catch (UnknownHostException e) 
					 {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} 
					 catch (InvalidHeaderException e) 
					 {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} 
					 catch (InvalidMessageException e) 
					 {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
	    	}).start();
		
	}

	public void stop() 
	{
		stopSelf();
	}
}

package de.trashplay.main;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;

import org.jboss.netty.buffer.ChannelBuffer;

import de.trashplay.social.ContentManager;
import de.uniluebeck.itm.ncoap.application.client.CoapClientApplication;
import de.uniluebeck.itm.ncoap.application.client.CoapResponseProcessor;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.EmptyAcknowledgementProcessor;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.InternalEmptyAcknowledgementReceivedMessage;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.InternalRetransmissionTimeoutMessage;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.RetransmissionTimeoutProcessor;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.InvalidHeaderException;
import de.uniluebeck.itm.ncoap.message.MessageType;
import de.uniluebeck.itm.ncoap.message.MessageCode;
import de.uniluebeck.itm.ncoap.message.options.ContentFormat;
import de.uniluebeck.itm.ncoap.message.options.InvalidOptionException;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;

public class SearchForHubs extends Activity
{
	public static final String TAG = TrashPlayConstants.TAG;
	public static final String PREFS_NAME = TrashPlayConstants.PREFS_NAME;
	
	private boolean endactivity = false;
	String ipbase="";
	HashMap<String, String> ipmap = new HashMap<String, String>();
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
	   //Store all IPS where a hub is in an SharedPref Thing
	   //set a flag to alow to end the activity
		setContentView(R.layout.searchforhubs);
		 SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		Editor edit = settings.edit();
        edit.putString("workingHubIPs", "");
        edit.commit();
		String ipadr = ContentManager.getIPAddress(true);
		Log.d(TAG, ipadr);
		ipadr = ipadr.replace(".", "X");
		Log.d(TAG, ipadr);
		String[] ipspace = ipadr.split("X");
		ipbase=ipspace[0]+"."+ipspace[1]+"."+ipspace[2];
		new Thread(new Runnable() 
		{
			public void run() 
			{
				for(int i=1; i<256; i++)
				{
					final String testip = ipbase+"."+i;
					final int j=i;
					new Thread(new Runnable() 
					{
						public void run() 
						{
		    	    		Log.d(TAG, "Test: "+testip);
							try 
							{
								int k = j;
								CoapClientApplication client = new CoapClientApplication();
			    	    		URI targetURI = new URI ("coap://"+testip+":5683/trashplayer");
								CoapRequest coapRequest = new CoapRequest(MessageType.Name.CON, MessageCode.Name.GET, targetURI);
								coapRequest.setAccept(ContentFormat.Name.APP_JSON);
								coapRequest.setMessageID(j);
								int id = coapRequest.getMessageID();
								Log.d(TAG, "put("+id+", "+k+")");
								ipmap.put(""+id, ""+k);
						        client.writeCoapRequest(coapRequest, new SimpleResponseProcessor());
							} 
							catch (InvalidOptionException e) 
							{
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (UnknownHostException e) 
							{
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (URISyntaxException e) 
							{
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (InvalidHeaderException e) 
							{
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}).start();
				} 
			}
		}).start();
	}
	
	public class SimpleResponseProcessor implements CoapResponseProcessor, EmptyAcknowledgementProcessor, RetransmissionTimeoutProcessor
    {

            
            @Override
            public void processCoapResponse(CoapResponse coapResponse) 
            {
                Log.d(TAG, "Received Response:  " + coapResponse);
                ChannelBuffer content = coapResponse.getContent();
                //TODO How do I get the fucking thing...
                byte[] contar = content.array();
                int ofs = content.arrayOffset();
                Log.d(TAG, "length: "+contar.length);
                byte[] contar100 = Arrays.copyOfRange(contar, 0, 100);
                byte[] contar200 = Arrays.copyOfRange(contar, 0, 200);
                byte[] contar300 = Arrays.copyOfRange(contar, 0, 300);
                Log.d(TAG, "ofset: "+ofs);
                String str = new String(contar);
                int lastbr = str.lastIndexOf("}");
                str = str.substring(0, (lastbr-14));//The "-14" are a strange thing. for some reasons the last 13 bytes are repeated at the end of the string. have no idea why. Might be a copa thing.
                Log.d(TAG, str);
                Log.d(TAG, "");
                //Log.d(TAG, new String(contar100));
                //Log.d(TAG, "");
                //Log.d(TAG, new String(contar200));
                //Log.d(TAG, "");
                //Log.d(TAG, new String(contar300));
                
           
                Log.d(TAG, ".getMessageID() " + coapResponse.getMessageID());
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                String workingips = settings.getString("workingHubIPs", "");
                Editor edit = settings.edit();
                String xx = ipmap.get(""+coapResponse.getMessageID());
                edit.putString("workingHubIPs", workingips+","+ipbase+"."+xx);
                Log.d(TAG, "found one at:"+ipbase+"."+xx);
                edit.commit();
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
				Log.d(TAG, "Timeou: "+timeoutMessage.getRemoteAddress());
				if(timeoutMessage.getRemoteAddress().toString().contains(".255:"))
				{
					//Log.d(TAG, "alow for end of activity");
					endactivity=true;
					SearchForHubs.this.finish();
				}
				
			}

    }
}

package de.trashplay.main;

import java.net.URI;
import java.net.URISyntaxException;
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
import de.uniluebeck.itm.ncoap.message.InvalidMessageException;
import de.uniluebeck.itm.ncoap.message.MessageDoesNotAllowPayloadException;
import de.uniluebeck.itm.ncoap.message.header.Code;
import de.uniluebeck.itm.ncoap.message.header.MsgType;
import de.uniluebeck.itm.ncoap.message.options.InvalidOptionException;
import de.uniluebeck.itm.ncoap.message.options.ToManyOptionsException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class TrashPlayClientService extends Service 
{

	Context ctx;
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
		ctx=this;
		start();
		return START_STICKY;
    }
	
	@Override
	public void onCreate() 
	{
		super.onCreate();
	}
	
	@Override
    public void onDestroy() 
    {
        super.onDestroy();
    }
	
	private void start()
	{
		 client = new CoapClientApplication();
		    new Thread(new Runnable() 
	    	{
	    	    public void run() 
	    	    {
					 try 
					 {
						URI targetURI = new URI ("coap://192.168.1.103:5683/TrashPlayer");
						CoapRequest coapRequest =  new CoapRequest(MsgType.CON, Code.POST, targetURI);
						coapRequest.setObserveOptionRequest();
						String r ="{\n";
						r=r+"\"command\": \"loudDown\",\n";
						r=r+"}";
						byte[] bytes = r.getBytes();
			            coapRequest.setPayload(bytes);
			            client.writeCoapRequest(coapRequest, new SimpleResponseProcessor());
					} 
					 catch (URISyntaxException e) 
					 {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} 
					 catch (InvalidMessageException e) 
					 {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} 
					 catch (ToManyOptionsException e) 
					 {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} 
					 catch (InvalidOptionException e) 
					 {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (MessageDoesNotAllowPayloadException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} 
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
                if(coapResponse.getPayload()!=null)
                {
	                ChannelBuffer paylo = coapResponse.getPayload();
	                byte[] paylobyte = paylo.array();
	                String strpay = new String(paylobyte);
	                String str = new String(coapResponse.getPayload().array(), Charset.forName("UTF-8"));
	                //Log.d(TAG, "PAYLOAD->"+strpay);
                }
                else
                {
                    Log.d(TAG, "NO PAYLOAD");
                }
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
}

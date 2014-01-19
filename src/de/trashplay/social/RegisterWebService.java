package de.trashplay.social;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.uniluebeck.itm.ncoap.message.options.ContentFormat.Name.APP_JSON;
import android.util.Log;

import com.google.common.util.concurrent.SettableFuture;

import de.trashplay.main.TrashPlayConstants;
import de.trashplay.main.TrashPlayServerService;
import de.trashplay.main.TrashPlayService;
import de.uniluebeck.itm.ncoap.message.*;
import de.uniluebeck.itm.ncoap.application.server.webservice.AcceptedContentFormatNotSupportedException;
import de.uniluebeck.itm.ncoap.application.server.webservice.ObservableWebservice;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.InvalidHeaderException;
import de.uniluebeck.itm.ncoap.message.InvalidMessageException;
import de.uniluebeck.itm.ncoap.message.options.InvalidOptionException;
import de.uniluebeck.itm.ncoap.message.options.Option;

public class RegisterWebService extends ObservableWebservice<String> 
{
	public static final String TAG = TrashPlayConstants.TAG;
	public static final String PREFS_NAME = TrashPlayConstants.PREFS_NAME;

	private String songinfoStr="";
	private int updateintervall=60000;
	
	public static RegisterWebService service;
	private static boolean stopall=false;
	
    private Logger log = LoggerFactory.getLogger(this.getClass().getName());
	
	protected RegisterWebService(String path, String initialStatus) 
	{
		super(path, initialStatus);
		service=this;
	}


	@Override
	public void shutdown() 
	{
	
	}

	@Override
	public void processCoapRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest, InetSocketAddress remoteAddress) 
	{
		log.debug("got request");
        try
        {
        	//we should check, if its a retransmit. if so, kill it with fire.
		    if(coapRequest.getMessageCodeName() == MessageCode.Name.GET)
		    {
		    	Log.d(TAG, "GET");
                Log.d(TAG, remoteAddress.getAddress().getHostAddress());
                String query = coapRequest.getContent().toString(Charset.forName("UTF-8"));
                if(query!=null)
                {
                        Log.d(TAG, query);
                }
                processGet(responseFuture, coapRequest, query);
		    }
		    else if(coapRequest.getMessageCodeName() == MessageCode.Name.POST)
            {
		    	Log.d(TAG, "POST");
                //Header h = coapRequest.getHeader();
                //Log.d(TAG, "header "+h.toString());
                Log.d(TAG, remoteAddress.getAddress().getHostAddress());
                processPost(responseFuture, coapRequest);
                Log.d(TAG, "DONE");
            }
		    else if(coapRequest.getMessageCodeName() == MessageCode.Name.PUT)
		    {
		    	Log.d(TAG, "PUT");
		    }
		    else
            {
                responseFuture.set(new CoapResponse(MessageCode.Name.METHOD_NOT_ALLOWED_405));
            }
        }
        catch(Exception e)
        {
            Log.e(TAG, ""+e);
            try 
            {
				responseFuture.set(new CoapResponse(MessageCode.Name.INTERNAL_SERVER_ERROR_500));
			} 
            catch (InvalidHeaderException e1) 
            {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        }
        if(stopall)
        {
        	shutdown();
			Log.d(TAG, "STOP HAMMERTIME");
			if(TrashPlayService.ctx!=null)
			{
				TrashPlayService.ctx.stop();
			}
        }
	}

	
	private void processPost(SettableFuture<CoapResponse> responseFuture, CoapRequest request)
    {
        CoapResponse response = null;
		String payload = request.getContent().toString(Charset.forName("UTF-8"));
		long requestMediaType = request.getContentFormat();
        Log.d(TAG, "POST: "+payload);
        String responseString=""; 
        responseString = ContentManager.parseRequest(MessageCode.Name.POST, payload, requestMediaType); 
        if(!responseString.equals(""))
        {
        	Log.d(TAG, "response was not empty");
        	Log.d(TAG, "response: "+responseString);
            try 
            {
                response = new CoapResponse(MessageCode.Name.CONTENT_205);
				response.setContent(responseString.getBytes(Charset.forName("UTF-8")), APP_JSON);
		        responseFuture.set(response);
		        if(responseString.equals("{\n\"stop\": \"ok\",\n}"))
		        {
		        	stopall=true;
		        }
		        	
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
			} catch (InvalidOptionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
        }
        else
        {
        	try 
        	{
				response = new CoapResponse(MessageCode.Name.BAD_REQUEST_400);
				responseFuture.set(response);
			} 
        	catch (InvalidHeaderException e) 
        	{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }
	
	private void processGet(SettableFuture<CoapResponse> responseFuture, CoapRequest request, String query)
	{
		Log.d(TAG, "process get");
		Set<Long> acceptOptions = request.getAcceptedContentFormats();
		String payload = request.getContent().toString(Charset.forName("UTF-8"));
		long requestMediaType =  request.getContentFormat();
        Log.d(TAG, "GET: "+payload);
        try
        {
	          Log.d(TAG, "accept optioon is empty ");
	          CoapResponse response = new CoapResponse(MessageCode.Name.CONTENT_205);
              response.setContent(ContentManager.createPayload(APP_JSON), APP_JSON);
              //response.setContentType(APP_XML);
              responseFuture.set(response);
        }
        catch(Exception e)
        {
        	
        }
		
	}

	@Override
	public void updateEtag(String resourceStatus) 
	{
		byte[] etag = new byte[4];
		//TODO: später vernünftig machen!
		if(resourceStatus.getBytes().length>3)
		{
			etag[0]= resourceStatus.getBytes()[0];
			etag[1]= resourceStatus.getBytes()[1];
			etag[2]= resourceStatus.getBytes()[2];
			etag[3]= resourceStatus.getBytes()[3];
		}
		else
		{
			etag[0]= resourceStatus.getBytes()[0];
			etag[1]= resourceStatus.getBytes()[0];
			etag[2]= resourceStatus.getBytes()[0];
			etag[3]= resourceStatus.getBytes()[0];
		}
		setEtag(etag);
		
	}

	@Override
	public boolean allowsDelete() 
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public byte[] getSerializedResourceStatus(long contentFormat) throws AcceptedContentFormatNotSupportedException 
	{
		return ContentManager.createPayload(contentFormat);
	}

}

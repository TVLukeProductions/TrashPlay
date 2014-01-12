package de.trashplay.social;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static de.uniluebeck.itm.ncoap.message.options.OptionRegistry.MediaType.APP_XML;
import static de.uniluebeck.itm.ncoap.message.options.OptionRegistry.MediaType.APP_JSON;
import android.util.Log;

import com.google.common.util.concurrent.SettableFuture;

import de.trashplay.main.TrashPlayConstants;
import de.trashplay.main.TrashPlayServerService;
import de.uniluebeck.itm.ncoap.application.server.webservice.MediaTypeNotSupportedException;
import de.uniluebeck.itm.ncoap.application.server.webservice.ObservableWebService;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.MessageDoesNotAllowPayloadException;
import de.uniluebeck.itm.ncoap.message.header.Code;
import de.uniluebeck.itm.ncoap.message.header.Header;
import de.uniluebeck.itm.ncoap.message.options.InvalidOptionException;
import de.uniluebeck.itm.ncoap.message.options.Option;
import de.uniluebeck.itm.ncoap.message.options.OptionRegistry.MediaType;
import de.uniluebeck.itm.ncoap.message.options.OptionRegistry.OptionName;
import de.uniluebeck.itm.ncoap.message.options.ToManyOptionsException;

public class TrashPlayerWebService extends ObservableWebService<String> 
{
	public static final String TAG = TrashPlayConstants.TAG;
	public static final String PREFS_NAME = TrashPlayConstants.PREFS_NAME;

	private String songinfoStr="";
	private int updateintervall=60000;
	
	protected TrashPlayerWebService(String path, String initialStatus, TrashPlayServerService tps) 
	{
		super(path, initialStatus);
		tps.registerForUpdates(this);
	}

	@Override
	public void shutdown() 
	{
	
	}

	@Override
	public byte[] getSerializedResourceStatus(MediaType mediaType) throws MediaTypeNotSupportedException 
	{
		return ContentManager.createPayload(mediaType);
	}

	@Override
	public void processCoapRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest, InetSocketAddress remoteAddress) 
	{
		Log.d(TAG, "got request");
        try
        {
		    if(coapRequest.getCode() == Code.GET)
		    {
		    	Log.d(TAG, "GET");
		    	Header h = coapRequest.getHeader();
                Log.d(TAG, "header "+h.toString());
                Log.d(TAG, remoteAddress.getAddress().getHostAddress());
                String query = coapRequest.getTargetUri().getQuery();
                if(query!=null)
                {
                        Log.d(TAG, query);
                }
                processGet(responseFuture, coapRequest, query);
		    }
		    else if(coapRequest.getCode() == Code.POST)
            {
		    	Log.d(TAG, "POST");
                Header h = coapRequest.getHeader();
                Log.d(TAG, "header "+h.toString());
                Log.d(TAG, remoteAddress.getAddress().getHostAddress());
                processPost(responseFuture, coapRequest);
                Log.d(TAG, "DONE");
            }
		    else if(coapRequest.getCode() == Code.PUT)
		    {
		    	Log.d(TAG, "PUT");
		    }
		    else
            {
                responseFuture.set(new CoapResponse(Code.METHOD_NOT_ALLOWED_405));
            }
        }
        catch(Exception e)
        {
            Log.e(TAG, ""+e);
            responseFuture.set(new CoapResponse(Code.INTERNAL_SERVER_ERROR_500));
        }
	}

	
	private void processPost(SettableFuture<CoapResponse> responseFuture, CoapRequest request)
    {
        CoapResponse response = null;
		String payload = request.getPayload().toString(Charset.forName("UTF-8"));
		MediaType requestMediaType =  request.getContentType();
        Log.d(TAG, "POST: "+payload);
        String responseString=""; 
        responseString = ContentManager.parseRequest(Code.POST, payload, requestMediaType); 
        if(!responseString.equals(""))
        {
        	Log.d(TAG, "response was not empty");
        	Log.d(TAG, "response: "+responseString);
            try 
            {
                response = new CoapResponse(Code.CONTENT_205);
				response.setPayload(responseString.getBytes(Charset.forName("UTF-8")));
				response.setContentType(APP_JSON);
		        responseFuture.set(response);
			} 
            catch (MessageDoesNotAllowPayloadException e1) 
            {
				// TODO Auto-generated catch block
            	Log.e(TAG, "e1");
				e1.printStackTrace();
			}
            catch (InvalidOptionException e) 
            {
				// TODO Auto-generated catch block
            	Log.e(TAG, "e");
				e.printStackTrace();
			} 
            catch (ToManyOptionsException ex) 
            {
				// TODO Auto-generated catch block
            	Log.e(TAG, "ex");
				ex.printStackTrace();
			}
        }
    }
	
	private void processGet(SettableFuture<CoapResponse> responseFuture, CoapRequest request, String query)
	{
		Log.d(TAG, "process get");
		List<Option> acceptOptions = request.getOption(OptionName.ACCEPT);
		String payload = request.getPayload().toString(Charset.forName("UTF-8"));
		MediaType requestMediaType =  request.getContentType();
        Log.d(TAG, "GET: "+payload);
        try
        {
	          Log.d(TAG, "accept optioon is empty ");
	          CoapResponse response = new CoapResponse(Code.CONTENT_205);
              response.setPayload(ContentManager.createPayload(APP_XML));
              response.setContentType(APP_XML);
              responseFuture.set(response);
        }
        catch(Exception e)
        {
        	
        }
		
	}

}

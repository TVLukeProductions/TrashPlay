package de.trashplay.social;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.conn.util.InetAddressUtils;
import org.farng.mp3.MP3File;
import org.farng.mp3.TagException;
import org.farng.mp3.id3.ID3v1;

import com.json.parsers.JSONParser;
import com.json.parsers.JsonParserFactory;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;
import de.trashplay.main.TrashPlayConstants;
import de.trashplay.main.TrashPlayServerService;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.header.Code;
import de.uniluebeck.itm.ncoap.message.options.OptionRegistry.MediaType;

/**
 * This class is used by the HTTP and CoaP Server to get Content, so that both Web Interfaces always provide the same Data
 * 
 * @author Lukas
 *
 */
public class ContentManager extends Service
{

	public static final String TAG = TrashPlayConstants.TAG;
	public static final String PREFS_NAME = TrashPlayConstants.PREFS_NAME;
	static TrashPlayServerService tps;
	
	private static Context context=null;
	 
	public static void startServer(TrashPlayServerService tps)
    {
      	Log.d(TAG, "Content startServer");
      	Log.d(TAG, "IP "+getIPAddress(true));
      	CoapServer server = new CoapServer(5683);
      	server.registerService(new TrashPlayerWebService("/TrashPlayer"," ", tps));
    }
	 
	public static byte[] createPayload(MediaType mediaType) 
	{
		Log.d(TAG, "create Payload");
		ArrayList<File> songlist = TrashPlayServerService.songlist();
		if(songlist.size()>0)
		{
			Log.d(TAG, "greater 0 and so..."+songlist.get(1).getAbsolutePath());
			return getCurrentSongInfo().getBytes(Charset.forName("UTF-8"));
		}
		else
		{
			Log.d(TAG, "0");
			return " ".getBytes(Charset.forName("UTF-8"));
		}
	}

	public static String parseRequest(Code get, String payload, MediaType mediaType) 
	{
		
		Log.d(TAG, "parse Request");
        Log.d(TAG, "Mediatype: "+mediaType.toString());
        Date d = new Date();
        if(mediaType == MediaType.TEXT_PLAIN_UTF8)
        {
                Log.d(TAG, "MediaType Plain Text");
                if(payload.equals(""))
                {
                        //nothing was posted...
                }
                else
                {
                        return parsePlainTextRequest(payload);
                }
        }
        if(mediaType == MediaType.APP_JSON)
        {
                Log.d(TAG, "MediaType JSON");
                if(payload.equals(""))
                {
                        //
                }
                else
                {
                	String jj = parseJSONRequest(payload);
                	Log.d(TAG, jj);
                	return jj;
                }
        }
		return "";
	}
	
	private static String parseJSONRequest(String payload) 
	{
		Log.d(TAG, "parse json string...");
		String result = "";
		JsonParserFactory factory=JsonParserFactory.getInstance();
        JSONParser parser=factory.newJsonParser();
        Map jsonData=parser.parseJson(payload);
        Set keys = jsonData.keySet();
        Iterator it = keys.iterator();
        
        while(it.hasNext())
        {
        	Object key = it.next();
            Object x=jsonData.get(key);
            if(x instanceof String)
            {
            	if(key.equals("token"))
                {
            		
                }
            	if(key.equals("command"))
            	{
            		Log.d(TAG, key+" "+x);
            		if(x.equals("next"))
            		{
            			
            		}
            		if(x.equals("voteUp"))
            		{
            			
            		}
            		if(x.equals("votedown"))
            		{
            			
            		}
            		if(x.equals("loudUp"))
            		{
            			Log.d(TAG, "command, louder");
            			int loudness = TrashPlayServerService.louder(true);
            			
            			Log.d(TAG, "loudness"+loudness); 
            			result ="{\n";
            			result=result+"\"loudness\":\""+loudness+"\"\n";
            			result=result+"}";		
            		}
            		if(x.equals("loudDown"))
            		{
            			int loudness = TrashPlayServerService.louder(false);
            			
            			Log.d(TAG, "loudness"+loudness);
            			result ="{\n";
            			result=result+"\"loudness\":\""+loudness+"\"\n";
            			result=result+"}";	
            		}
            	}
            }
        }
		return result;
	}

	private static String parsePlainTextRequest(String payload) 
	{
		
		return "";
	}

	public static String getIPAddress(boolean useIPv4) 
	{

        String addresses = "";
        try 
        {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) 
            {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) 
                {
                    if (!addr.isLoopbackAddress()) 
                    {
                        String sAddr = addr.getHostAddress().toUpperCase();
                        boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        if (useIPv4) 
                        {
                            if (isIPv4)
                                addresses += sAddr + ", ";
                        } 
                        else 
                        {
                            if (!isIPv4) 
                            {
                                int delim = sAddr.indexOf('%'); // drop ip6 port suffix
                                if(delim<0) addresses += sAddr + ", ";
                                else addresses += sAddr.substring(0, delim) + ", ";
                            }
                        }
                    }
                }
            }
        } 
        catch (Exception ex) 
        { 
        	
        } 
        if(addresses == null || addresses.length() <= 3) return "";
        return addresses.subSequence(0, addresses.length()-2).toString();
    }

	public static String getCurrentSongInfo() 
	{
		Log.d(TAG, "current Song Info");
		String result="";
		ArrayList<File> songlist = TrashPlayServerService.songlist();
		if(songlist.size()>1)
		{
			//Log.d(TAG, "greater 0 and so..."+songlist.get(0).getAbsolutePath());
			//String path = songlist.get(0).getAbsolutePath();
			File f = new File(TrashPlayServerService.file);
			try 
			{
				MP3File mp3 = new MP3File(f);
				ID3v1 id3 = mp3.getID3v1Tag();
				result=result+"{\n";
				result=result="  \"Song\": {\n";
				result=result+"    \"Artist\": \""+id3.getArtist()+"\",\n";
				result=result+"    \"Title\": \""+id3.getSongTitle()+"\",\n";
				result=result+"    \"Size\": \""+id3.getSize()+"\",\n";
				result=result+"    \"Year\": \""+id3.getYear()+"\",\n";
				result=result+"    \"File\": \""+f.getName()+"\",\n";
				result=result+"  },\n";
				result=result+"  \"Player\": {\n";
				result=result+"    \"Playing\": \""+TrashPlayServerService.playing+"\",\n";
				result=result+"    \"Loudness\": \""+TrashPlayServerService.loudness+"\",\n";
				result=result+"  },";
				result=result+"}\n";
				
			} 
			catch (Exception e) 
			{
				String[] metadata = new String[2];
				metadata[0]="";
				metadata[1]="";
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e(TAG, "There has been an exception while extracting ID3 Tag Information from the MP3");
				String fn = f.getName();
				fn.replace("–", "-"); //wired symbols that look alike
				if(fn.contains("-") && fn.endsWith("mp3"))
				{
					fn=fn.replace(".mp3", "");
					String[] spl = fn.split("-");
					if(spl.length==2)
					{
						result="";
						metadata[0]=spl[0];
						metadata[0]=metadata[0].trim();
						metadata[1]=spl[1];
						metadata[1]=metadata[1].trim();
						result=result+"{\n";
						result=result="  \"Song\": {\n";
						result=result+"    \"Artist\": \""+metadata[0]+"\",\n";
						result=result+"    \"Title\": \""+metadata[1]+"\",\n";
						result=result+"    \"Size\": \"unknown\",\n";
						result=result+"    \"Year\": \"unknown\",\n";
						result=result+"    \"File\": \""+f.getName()+"\",\n";
						result=result+"  },\n";
						result=result+"  \"Player\": {\n";
						result=result+"    \"Playing\": \""+TrashPlayServerService.playing+"\",\n";
						result=result+"    \"Loudness\": \""+TrashPlayServerService.loudness+"\",\n";
						result=result+"  },";
						result=result+"}\n";
						
					}
				}
				else
				{
					result="";
					result=result+"{\n";
					result=result="  \"Song\": {\n";
					result=result+"  \"Artist\": \"unknown\",\n";
					result=result+"  \"Title\": \"unknown\",\n";
					result=result+"  \"Size\": \"unknown\",\n";
					result=result+"  \"Year\": \"unknown\",\n";
					result=result+"  \"File\": \""+f.getName()+"\",\n";
					result=result+"  },\n";
					result=result+"  \"Player\": {\n";
					result=result+"    \"Playing\": \""+TrashPlayServerService.playing+"\",\n";
					result=result+"    \"Loudness\": \""+TrashPlayServerService.loudness+"\",\n";
					result=result+"  },";
					result=result+"}\n";
				}
			} 
			
			Log.d(TAG, "ID3:" + result);
			return result;
		}
		else
		{
			result=result+"{\n";
			result=result="  \"Song\": {\n";
			result=result+"  \"Artist\": \"unknown\",\n";
			result=result+"  \"Title\": \"unknown\",\n";
			result=result+"  \"Size\": \"unknown\",\n";
			result=result+"  \"Year\": \"unknown\",\n";
			result=result+"  \"File\": \"unknown\",\n";
			result=result+"  },\n";
			result=result+"  \"Player\": {\n";
			result=result+"    \"Playing\": \""+TrashPlayServerService.playing+"\",\n";
			result=result+"    \"Loudness\": \""+TrashPlayServerService.loudness+"\",\n";
			result=result+"  },";
			result=result+"}\n";
			return " ";
		}
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}

package de.trashplay.dropbox;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.Session.AccessType;

import de.trashplay.main.TrashPlayConstants;
import de.trashplay.main.TrashPlayService;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Environment;
import android.util.Log;

public class DropBox 
{
	public static final String TAG = TrashPlayConstants.TAG;

	 final static private AccessType ACCESS_TYPE = AccessType.DROPBOX;
	 public static ArrayList<String> folders = new ArrayList<String>();
	 public static boolean syncinprogress=false;
	 
	
	public static void syncFiles(final SharedPreferences settings) 
	{
		// TODO Auto-generated method stub
		if(!syncinprogress)
		{
			final DropboxAPI<AndroidAuthSession> mApi = TrashPlayService.mDBApi;
			new Thread(new Runnable() 
	    	{
	    	    public void run() 
	    	    {
	    	    	syncinprogress=true;
        			File folder = new File(Environment.getExternalStorageDirectory().getPath() + "/Music/TrashPlay");
        			folder.mkdirs();
    	    		ArrayList<String> fileNames = new ArrayList<String>();
	    	    	try
	    	    	{ 
	    	    		//Log.d(TAG, "try");
	    	    		//ArrayList<String> folderName=new ArrayList<String>();
	    	    		Entry dropboxDir1=null;
	    	    		try
	    	    		{
	    	    			dropboxDir1 = mApi.metadata("/DropBoxTrashPlaylistDerHoelle", 0, null, true, null);    
	    	    		}
	    	    		catch(DropboxException ex)
	    	    		{
	    	    			Log.e(TAG, "->"+ex.getMessage());
	    	    			mApi.createFolder("/DropBoxTrashPlaylistDerHoelle");
	    	    			dropboxDir1 = mApi.metadata("/DropBoxTrashPlaylistDerHoelle", 0, null, true, null); 
	    	    		}
	    	    		if (dropboxDir1!=null && dropboxDir1.isDir) 
	    	    		{ 
	    	    			List<Entry> contents1 = dropboxDir1.contents;
	    	    		    if (contents1 != null) 
	    	    		    {
	    	    		     	fileNames.clear();
	    	    		        for (int i = 0; i < contents1.size(); i++) 
	    	    		        {
	    	    		        	Entry e = contents1.get(i);
	    	    		            String a = e.fileName();  
	    	    		            if(!e.isDir)
	    	    		            {
	    	    		            	//Log.d(TAG, e.fileName());
	    	    		            	if(e.fileName().endsWith(".mp3") || e.fileName().endsWith(".wav") || e.fileName().endsWith(".mp4"))
	    	    		            	{
	    	    		            		FileOutputStream outputStream = null;
	    	    		            		try 
	    	    		            		{
		    	    		            		//Log.d(TAG, "music file");
		    	    		            		fileNames.add(e.fileName());
		    	    		            		if(!e.modified.equals(settings.getString("lastchange"+e.fileName(), "")))//last change has changed
	   	    		            				{
		    	    		            			if(TrashPlayService.wifi)
		    	    		            			{
		    	    		            				Log.d(TAG, "wifi is");
			    	    		            			Editor edit = settings.edit();
			    	    		            			File file = new File(Environment.getExternalStorageDirectory().getPath() + "/Music/TrashPlay/"+e.fileName());
				    	    		            		Log.d(TAG, "have new file");
				    	    		            		outputStream = new FileOutputStream(file);
				    	    		            		Log.d(TAG, "have output stream for the file");
				    	    		            		Log.d(TAG, "String->"+dropboxDir1.fileName()+"/"+e.fileName());
				    	    		            		mApi.getFile(dropboxDir1.fileName()+"/"+e.fileName(), null, outputStream, null);
				    	    		            		Log.d(TAG, "stuff with stuff");
			    	    		            			edit.putString("lastchange"+e.fileName(), e.modified);
			    	    		            			edit.commit();
		    	    		            			}
		    	    		            			else
		    	    		            			{
		    	    		            				Log.d(TAG, "no wifi");
		    	    		            			}
	   	    		            				}	    	    		            		
		    	    		            	} 
	    	    		            		catch (Exception em) 
	    	    		            		{
	    	    		            		    System.out.println("Something went wrong: " + em);
	    	    		    	    	    	syncinprogress=false;
	    	    		            		} 
	    	    		            		finally 
	    	    		            		{
	    	    		            		    if (outputStream != null) 
	    	    		            		    {
	    	    		            		        try 
	    	    		            		        {
	    	    		            		            outputStream.close();
	    	    		            		        } 
	    	    		            		        catch (IOException ef) 
	    	    		            		        {
	    	    		            		        	
	    	    		            		        }
	    	    		            		    }
	    	    		            		}
	    	    		            	}
	    	    		            }
	    	    		         }
	    	    		    }
	    	    		}
	    	    	}
	    	    	catch (Exception ex) 
	    	    	{
	    	    		Log.e(TAG, "ERROR in the DropBox Class");
	    	    		Log.e(TAG, "->"+ex.getMessage());
	    	    		
		    	    	syncinprogress=false;
		    	    	return;
	    	    	}
	    	    	List<File> files = getListFiles(folder); 
	    	    	//Log.d(TAG, "compare local files to delete those that are not needed no more");
	    	    	for(int i=0; i<files.size(); i++)
	    	    	{
	    	    		//Log.d(TAG, files.get(i).getName());
	    	    		if(fileNames.contains(files.get(i).getName()))
	    	    		{
	    	    			
	    	    		}
	    	    		else
	    	    		{
	    	    			//TODO: theoretically a file could be deleted while it is played. that would most certainly crash the app.
	    	    			Log.d(TAG, "DELETE"+files.get(i).getName());
	    	    			if(!TrashPlayService.file.contains(files.get(i).getName()))//do not delet the file that is currently played plz
	    	    			{
		    	    			files.get(i).delete();
		    	    			Editor edit = settings.edit();
		            			edit.putString("lastchange"+files.get(i).getName(), "");
		            			edit.commit();
	    	    			}
	    	    		}
	    	    	}
	    	    	syncinprogress=false;
	    	    }
	    	}).start();
		}
		else
		{
			Log.d(TAG, "not syncing, because syn in progress");
		}
    }
    
    //thanks http://stackoverflow.com/questions/9530921/list-all-the-files-from-all-the-folder-in-a-single-list
    private static List<File> getListFiles(File parentDir) 
    {
        ArrayList<File> inFiles = new ArrayList<File>();
        File[] files = parentDir.listFiles();
        for (File file : files) 
        {
            if (file.isDirectory()) 
            {
            } 
            else 
            {
            	inFiles.add(file);
            }
        }
        return inFiles;
    }

}

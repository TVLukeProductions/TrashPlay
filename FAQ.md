# Trash Player 1.0 - Like A Boss

#What is this I don't even?
The first rule of TrashPlaylist is: You do not skip a track on the TrashPlaylist. 

The second rule of TrashPlaylist is: You do not skip a track on the TrashPlaylist. 

Third rule of TrashPlaylist: Someone yells stop, goes limp, taps out, you turn on the Swag. 

Fourth rule: Only those who are invited may add songs. 

Fifth rule: One random order at a time, fellas. 

Sixth rule: Valid artist, valid title (ID3 tag & file name!). 

Seventh rule: Road trips will go on as long as they have to. 

And the eighth and final rule: If this is your first night with the TrashPlaylist, you have to listen.

#No, really: WHAT?
Trash Player is a Music Player. It's purpose is to support groups of friends who have bundled their
music for some purpose (like a party, a road trip, any fun event or just for whatever) to play this music.

Imagine you are planing a barbeque and you would like to play music where everyone can listen to songs they like.
Just open up a folder on your favorite cloud storage[1] and let them contribute. You could call such a 
play list "Trash Playlist". Actually, you should.

When the party starts just start the Trash Player. Since the Trash Player does not have a "next" or 
"previous" button you are save from guest with itchy trigger fingers. A song will randomly come up 
when it does. Or not.
 
So what is trash player. It is kind of a social media player. Just more fun. 

# I started my app and there is no music?
The Trash Player gets its music from cloud storage services. It also supports local tracks but they 
have to be in a specific folder of you mobile device.

You can put files into the folder Music/TrashPlay/local if you want to store them only locally and 
they will show up in your TrashPlayList as a seperate,local, play list [2].

Otherwise you have to connect to one of the cloud storage services like DropBox or google Drive [1] 
and download music. Trash Player will download all mp3s from the folders you have denoted as 
TrashPlayer download lists. 

How do I do this? Just put a file that starts with ".trashplay" into your folder, it could for 
example be an empty txt file. 

#OK, it seems to synchonize with my cloud storage service, but it takes forever!
You probably have a lot of files. Just wait. If it lasts longer than 5 hours, tell your doc... I 
mean, write a bug report [3].

# My folder just does not synchronize
The app only looks for new folders every 15 minutes. So if you just added the ".trashplay.txt" file, 
you may have to wait a bit. You can force a search by disconnecting from your storage and quickly reconnecting. 
This should not delete any of your play lists.

#Why No skipping?
"Why will you not let me skip this stupid track someone else has put into the folder?"
"Well that's just the way of the Trash Player."
"But there has to be a way!"
"Easy there captain kirk, there kind of is, just read the next question."

# Whats Trash Mode?
Trash Mode is the standard mode of your Trash Player. It means randomly selected songs with no posibility of skipping.
However, you can deactivate TrashMode. You will be given a "next" and a "previous" button".

Note: If you return to trashMode, the paly list for the next 10 songs will revert to those that where in line when you 
deactivated TrashMode because on the TrashPlaylist, there is no skipping.
 
# What is radio?
No all things that require a Trash Playlist take place at a single place. There may be road trips with multiple
cars or hackathons in multiple cities or whatever.

With radio you can start a radio and other Trash Players can join, then all of them will play the 
same songs at (roughly) the same moment.

You can not always start a radio. Since others will want to "check in" your radio the app needs to 
make sure that they have the same songs you have, so you can only start a radio if:
* you currently have only ne playlist activated (settings -> configue Playlists)
* This single activated playlist is not local

You also need to give the station a name.

#OK, those ".station" files are stupid. I get DropBox-notifications like every minute
Yes, you need to turn of synchronization for this folder. Click on the DropBox logo on your PC or 
Mac, go to settings,a dn deactivate synchronization for the "Radio"-Folder.

#What kind of data-usage will using or broadcasting radio entail?
Its not a lot, but off course your app has to send its play list into the web or download someone elses.
There is no correct answer here but my gues would by 

#Known Bugs
* When going into the settings you have to reenter the name of your radio every time
* 

[1] Currently Trash Player supports only DropBox
[2] This feature is not yet fully implemented.
[3] https://github.com/TVLukeProductions/TrashPlay/tree/LikeABoss
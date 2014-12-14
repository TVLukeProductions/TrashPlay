<p align="center">
<img src="https://raw.github.com/TVLuke/TrashPlay/master/res/drawable-hdpi/ic_launcher.png" alt="Logo"/>
</p>

# Trash Player 1.0 - Like A Boss
### All the features you always hated and many more.

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

Imagine you are planing a barbecue and you would like to play music where everyone can listen to songs they like.
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
they will show up in your TrashPlayList as a separate,local, play list [2].

Otherwise you have to connect to one of the cloud storage services like DropBox or google Drive [1] 
and download music. Trash Player will download all mp3s from the folders you have denoted as 
TrashPlayer download lists. 

How do I do this? Just put a file that starts with ".trashplay" into your folder, it could for 
example be an empty txt file called ".trashplay.txt". 

#OK, it seems to synchronize with my cloud storage service, but it takes forever!
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
"Easy there Captain Kirk, there kind of is, just read the next question."

# Whats Trash Mode?
Trash Mode is the standard mode of your Trash Player. It means randomly selected songs with no possibility of skipping.
However, you can deactivate TrashMode. You will be given a "next" and a "previous" button".

Note: If you return to trashMode, the play list for the next 10 songs will revert to those that where in line when you 
deactivated TrashMode because on the TrashPlaylist, there is no skipping.
 
# What is "Radio"?
Not all things that require a Trash Playlist take place at a single place. There may be road trips with multiple
cars or hackathons in multiple cities or whatever.

With radio you can start a radio and other Trash Players can join, then all of them will play the 
same songs at (roughly) the same moment.

You can not always start a radio. Since others will want to "check in" your radio the app needs to 
make sure that they have the same songs you have, so you can only start a radio if:
* you currently have only ne playlist activated (settings -> configure Playlists)
* This single activated playlist is not local

You also need to give the station a name.

#OK, those ".station" files are stupid. I get DropBox-notifications like every minute
Yes, you need to turn of synchronization for this folder. Click on the DropBox logo on your PC or 
Mac, go to settings,a dn deactivate synchronization for the "Radio"-Folder.

#What kind of data-usage will using or broadcasting radio entail?
Its not a lot, but off course your app has to send its play list into the web or download someone else's.
There is no correct answer here but my guess would be that you would have to use radio outside of 
wifi for about 4 days to get 1 mb of data... but I might be wrong.

#How much other traffic does this app generate
Songs (mp3 files) are only downloaded in wifi, however scrobbling to the TrashPlay-Last.fm account is 
always happening. This isn't a lot of data either (probably even less than what radio uses) but its there.

Over time I will find out what kind of usage the app has and will update this info.

#Known Bugs
* When going into the settings you have to reenter the name of your radio every time
* The Previous Button doesn't do anything....

[1] Currently Trash Player supports only DropBox
[2] This feature is not yet fully implemented.
[3] https://github.com/TVLukeProductions/TrashPlay/tree/LikeABoss

## Release History

### Current

#### 1.0.4 b / 1.0.5 b - Like A Boss (14.12.2014)

* Rewrote entire code base
* Persitence layer
* Posibility to deactivate trash Mode
* Scrobbling to TrashPlay and Private last.fm account
* Introducing Trash-Play Radio: You play songs from the playlists and other can listen to the same music at the same time at another place
* Support for Multiple Remote folders with music

<p align="center">
<img src="https://raw.githubusercontent.com/TVLukeProductions/TrashPlay/LikeABoss/screenshots/trashplay105a.png" alt="Screenshot 1"/>
<img src="https://raw.githubusercontent.com/TVLukeProductions/TrashPlay/LikeABoss/screenshots/trashplay105b.png" alt="Screenshot 2"/>
</p>

### Older Versions
#### v 0.2.b Hund im Betastadium (17.01.2014) 
* Some UI Improvements
* **BUG FIXES**
* The last version was restarting from time to time and would just play music. This one does not
* Reliability

<p align="center">
<img src="https://raw.github.com/TVLuke/TrashPlay/master/screenshots/trashplay02b_a.png" alt="Screenshot 1"/>
<img src="https://raw.github.com/TVLuke/TrashPlay/master/screenshots/trashplay02b_b.png" alt="Screenshot 2"/>
</p>

#### v 0.1.dayum (11.01.2014) 
* The volume control is now only turing up to 10% if the loudness was done to 0%. The previous strategy turned out to suck for headphone users.
* changes in UI
* List of coming songs
* **Bug Fixes**
* Some minor fixes for convenience
* Checking if files are changed or deleted before playing (this used to get the app into some hickups)

#### v 0.1.roflcopter (02.01.2014) 
* Play music
* Shut down on stop
* Display title of the current track
* Dropbox connection possible
* Creates _DropBoxTrashPlaylistDerHoelle_ folder if it does not exist.
* Syncing with the _DropBoxTrashPlaylistDerHoelle_ folder
* stops syncing when you leave the Wifi
* only show play button if play is actually an option
* Scrobbling to last.fm
* No Silence. Music will be turned up again at once.
* Remember last song and continue with that song when restarted

<p align="center">
<img src="https://raw.github.com/TVLuke/TrashPlay/master/screenshots/device-2014-01-02-191841.png" alt="Screenshot 1"/>
<img src="https://raw.github.com/TVLuke/TrashPlay/master/screenshots/device-2014-01-02-191103.png" alt="Screenshot 2"/>
</p>

#### v 0.1.deinemudda (31.12.2013)
* Play music
* Shut down on stop
* DropBox connection possible
* Syncing with the _DropBoxTrashPlaylistDerHoelle_ folder
* stops syncing when you leave the Wifi
* only show play button if play is actually an option
* Scrobbling to last.fm
* No Silence. Music will be turned up again after a few minutes.

##Changelog
### 03.12.2014
* Activity zum abspielen erstellt
* Verbinden zu DropBox
* Synchronisieren von multiplen DropBox Ordnern
* Abspielen eines Songs
* Scrobblen zum TrashPlayList Last.fm account
* Abspielen auch wieder Stoppen (App geht dann aus)
* GitHub Branch "LikeABoss"

### 04.12.2014
* readme.md erstellt
* Dateien aus der App löschen, wenn sie in keiner Playlist mehr sind
* Sicherstellen dass die App nicht versucht Dateien abzuspielen die es nicht mehr gibt
* Erst den PlayButton anzeiegen, wenn es etwas zum abspielen gibt
* In der Mainactivity checken ob die app schon läuft (Dann direkt Stop Button anzeigen)
* MusicPlayer.scrobble überarbeiten
* Musiktitel in der Aktivität anzeigen
* Sync-Anzeige (Mit Animation)
* Es möglich machen die App abzuschalten ohne jemals Musik zu starten

### 06.12.2014
* Fehler in der interaction zwischen Player und MuiscCollectionManager ausgearbeitet
* Song Liste darstellen
* Länge des Stücks und bisher gespielte Zeit in der Main Aktivität einblenden
* Menge der Songs in der Main Activity Anzeigen
* Infobox zwischen Playbutton und Liste
* SongSelection
 * Default Wahrscheinlichkeits-SongSelector
 * EqualsPlaySongSelector
* Persitenzlayer
* Mit dem Song weitermachen, mitdem man aufgehört hat
* Plays zählen pr Song
* Optinal Next and Previous Button
* Settings Activity anlegen
* Scrobblen zum persönlichen Last.fm account
* Anlegen von Menu-Items für weitere Funktionen
* Aktivieren und deaktivieren synchronisierter Playlists

### 07.12.2014
* Synchronisiertes Musikhören via DropBox
* Lots and Lots and Lots and Lots of debugging. Die Persitenzschicht macht ordentlich Ärger. Hätt ichs doch einfach selber geschrieben.
* Radiosymbol wenn man sendet

###08.12.2014
* Zugriff auf Persitenzobjekte nur noch von zwei Helper KLassen für Transparenz und Refactorbarkeit
* Realm wird jetzt durch ORMLite erstetzt
* ORMLIte wird jetzt durch ActiveAndroid ersetzt
* com.android.support:support-v4:20.0.0 in gradele weil ActiveAndroid sonst nicht tut

###09.12.2014
* Die umstellung auf Active Android zuende bringen
* Speichern der Playlists in Songs nur serialisiert. Weil n-n Realtionships in ActiveAndroid nicht vernünfig sind. Gnaaaaa.

###11.12.2014
* Die Badges als persistentes Element Hinzugefügt 

###13.12.2014 
* Aus Log.d zu Logger.d usw. umgestellt für den Alpha-Betrieb
* Wenn man TrashPlayMode reaktiviert wird die letzte Playlist wiederhergestellt bei der man es deaktiviert hatte
* Häufigeres Synchronisieren der Radiostationenliste
* Alte Radiostationen sollten nicht gezeigt werden
-> Release 1.0.1

### 14.12.2014
* Bugfix: "App crash on oriantation change".
-> Release 1.0.2
* Bugfix: No RadioMode when multiple playlists, even if one is turned off
-> Release 1.0.4
* Bugfix: "Radiostations can not have a space in their name"
* Bugfix: "Songs that are in one deactivated playlist do not show up"
* Bugfix: "App scrobbles to TrashPlays Last.fm account even if not in TrashMode"
* Previous Button hat jetzt überhaupt erstmal eine Funktion
* String encoding von Playlists verbessert
* Massive verbesserungen bei Radio funktionen
-> Release 1.0.5

## ToDo
* Sammeln von persönlichen Statistiken
* bessere sync animation
* Local Storage als Speicherort
* Exportfunktion (z.B. für Handywechsel oder sowas)

* Bessere/Mehr/Überhaupt Unit-Tests

* Lokalisierung
** Deutsch
** Endlisch

* GDrive Integration
* Chromecast Integration
* Hue Light Party Funktion

# Worüber ich mir so Gedanken mache=
* Redesign von Logos
* Badges
* Verschiedene Wahrscheinlichkeitsverteilungen (nach Ort, zeit, Tag, Jahreszeit, Geschwindigkeit...)
* Social Interaction
* Non Mobile Java version, die in der Dropbox liegt
* Die Android Klasse SyncAdapter nutzen um zukünftige CloudStorage sachen vernünftig zu wrappen

#Kudos
## This App uses:
* [Dropbox android SDK](https://www.dropbox.com/developers/core/sdks/android)
* [lastfm-java](https://code.google.com/p/lastfm-java/)
* [Java ID3 Tag Library](http://javamusictag.sourceforge.net/)
* [Active Android](http://www.activeandroid.com/)

# License
[CC-BY-NC](http://creativecommons.org/licenses/by-nc/4.0/)
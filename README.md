<p align="center">
<img src="https://raw.github.com/TVLuke/TrashPlay/master/res/drawable-hdpi/ic_launcher.png" alt="Logo"/>
</p>
## TrashPlay

[Download current version: 0.2.b Hund im Betastadium](https://www.dropbox.com/s/qe885dgip6c31rt/TrashPlay_0.2hundbeta.apk)
(This thing is totally Beta. So report Bugs, Questions and Feature Requests [here](https://github.com/TVLuke/TrashPlay/issues) and check from time to time if there is an update.)

TrashPlay is a simple music player for Android (3.x and 4.x) phones and tablets to use during car trips and other events that need a Trash Playlist (for reasons). It offers only one button. Play. There is no skipping and no pausing. Ever.

The player takes its music from the Dropbox, using a folder called _DropBoxTrashPlaylistDerHoelle_. You should share that folder with all the people who will be on the car ride to put music in. TrashPlay will not use any other folder. Ever.

The player is nice to your bandwidth and synchronizes only when you are in a Wifi. So, don't forget to sync before traveling. The music in your Dropbox folder will be put on your phone. Music that isn't in the Dropbox folder will not stay on the phone. Ever.

If you enable last.fm (that is, when the last.fm button is grey) it scrobbles the music you listen to a [specific last.fm account](http://www.lastfm.de/user/TrashPlayList). This last.fm-account represents the record of all music played on any TrashPlay App. Ever.

If you stop the app TrashPlay will resume with the song played when you stoped the app. There is no skipping with Trash. Ever.

## Release History

### Current
#### v 0.2.b Hund im Betastadium (17.01.2014) [Download](https://www.dropbox.com/s/qe885dgip6c31rt/TrashPlay_0.2hundbeta.apk)
* Some UI Improvements
* **BUG FIXES**
* The last version was restarting from time to time and would just play music. This one does not
* Reliability

## Next Up (version 0.2.1 Trash im Buero)
* Check to keep some storage on the device
* **Party Mode** (connect via Wifi/CoaP and somehow influence the playlist)
* Client Activity: Clients "log in" to a TrashPlayer session. They can then 
 * Scroble the song to their own last.fm account
 * Control loudness
 * See what songs are coming up
 * Like and dislike songs
 * More Stuff I have to think of


### Older Versions
#### v 0.1.dayum (11.01.2014) [Download](https://www.dropbox.com/s/w2z350mt5urbu20/TrashPlay_0.1.dayum.apk)
* The volume control is now only turing up to 10% if the loudness was done to 0%. The previous strategy turned out to suck for headphone users.
* changes in UI
* List of coming songs
* **Bug Fixes**
* Some minor fixes for convenience
* Checking if files are changed or deleted before playing (this used to get the app into some hickups)

#### v 0.1.roflcopter (02.01.2014) [Download](https://www.dropbox.com/s/g0jf9bbckhkohtk/TrasPlay_0.1.roflcopter.apk)
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
* Dropbox connection possible
* Syncing with the _DropBoxTrashPlaylistDerHoelle_ folder
* stops syncing when you leave the Wifi
* only show play button if play is actually an option
* Scrobbling to last.fm
* No Silence. Music will be turned up again after a few minutes.

### Ideas for Future Releases 

#### 0.2.2
* HTTP Server in addition to the existing coap Server
* Better Service/Resource structure provided via more then just one URI
* QR Code visualisation to make connecting to a server easy
* More Meta Info on Tracks (probably from last.fm)
* Automated Light Show via Philips Hue

#### 0.3 Taking the Trashplayer to Isengard
* facebook Social Graph API Integration
* Geofencing feature. Certain Songs that get palyed when entering or exiting specific locations Possibly as a global feature...
* Integrate different sub-folders by date (Christmas, Easter, Party,...)

#### 0.4 Yumi, Yumi, Trashy
* Allow Users to define new sub-folders and rules on how and when these should be used
* Network Service Discovery
* Visualize the people who are in a group of listeners

#### 0.5 Ein TrashPlayer mit Elfenohren
* Integrating other sources of music besides the dropbox (NAS Storage...)

#### 1.0 ?
* Allow for Trash Mode and regular music player mode (at this point the project is not actually the Trash Player anymore and needs to be forked)


## This App uses:
* [Dropbox android SDK](https://www.dropbox.com/developers/core/sdks/android)
* [lastfm-java](https://code.google.com/p/lastfm-java/)
* [Java ID3 Tag Library](http://javamusictag.sourceforge.net/)

## License
[CC-BY-NC](http://creativecommons.org/licenses/by-nc/4.0/)

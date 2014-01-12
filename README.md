<p align="center">
<img src="https://raw.github.com/TVLuke/TrashPlay/master/res/drawable-hdpi/ic_launcher.png" alt="Logo"/>
</p>
## TrashPlay

[Download current version](https://www.dropbox.com/s/g0jf9bbckhkohtk/TrasPlay_0.1.roflcopter.apk)
(This thing is totally Beta. So report Bugs, Questions and Feature Requests [here](https://github.com/TVLuke/TrashPlay/issues) and check from time to time if there is an update.)

TrashPlay is a simple music player for Android (3.x and 4.x) phones and tablets to use during car trips and other events that need a Trash Playlist (for reasons). It offers only one button. Play. There is no skipping and no pausing. Ever.

The player takes its music from the Dropbox, using a folder called _DropBoxTrashPlaylistDerHoelle_. You should share that folder with all the people who will be on the car ride to put music in. TrashPlay will not use any other folder. Ever.

The player is nice to your bandwidth and synchronizes only when you are in a Wifi. So, don't forget to sync before traveling. The music in your Dropbox folder will be put on your phone. Music that isn't in the Dropbox folder will not stay on the phone. Ever.

If you enable last.fm (that is, when the last.fm button is grey) it scrobbles the music you listen to a [specific last.fm account](http://www.lastfm.de/user/TrashPlayList). This last.fm-account represents the record of all music played on any TrashPlay App. Ever.

If you turn down the volume of the phone, it will turn back up. You cannot silence the trash. Ever.

If you stop the app TrashPlay will resume with the song played when you stoped the app. There is no skipping with Trash. Ever.

## Next Up (version 0.1.Freezer)
* Check to keep some storrage on the device
* Partymode (connect via Wifi/CoaP and somehow influence the playlist)
* NextUp (A preview of comming attarctions)
## Ideas for Future Releases (0.2.hundimbuero)**

* Since Trash-Playlists are a social thing, this app will, in the near future, be a server, connecting to phones in its environment, as clients, who can in some way (to be determined) the playlist or the experience for other users of the TrashPlay. This will be exiting, and strange. And I have no Idea yet how it will work. 
* Weighted probabilities. There are reasons (for example special purpose songs) that should occur with higher probability
* Geofencing feature. Certain Songs that get palyed when entering or exiting specific locations. Possibly as a global feature...

## Release History

### Current
#### v 0.1.dayum (11.01.2014) [Download]()
* The volume control is now only turing up to 10% if the loudness was done to 0%. The previous strategy turned out to suck for headphone users.
* changes in UI
* Partymode: The TrashPlayer now opens a CoaP Server to provide info to clients
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
### Older Versions

#### v 0.1.deinemudda (31.12.2013)
* Play music
* Shut down on stop
* Dropbox connection possible
* Syncing with the _DropBoxTrashPlaylistDerHoelle_ folder
* stops syncing when you leave the Wifi
* only show play button if play is actually an option
* Scrobbling to last.fm
* No Silence. Music will be turned up again after a few minutes.

## This App uses:
* [Dropbox android SDK](https://www.dropbox.com/developers/core/sdks/android)
* [lastfm-java](https://code.google.com/p/lastfm-java/)
* [Java ID3 Tag Library](http://javamusictag.sourceforge.net/)

## License
[CC-BY-NC](http://creativecommons.org/licenses/by-nc/4.0/)

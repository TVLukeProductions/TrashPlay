An diesem Ort entshet die neue Version der App.

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

# Worüber ich mir so Gedanken mache=
* Redesign von Logos
* Badges
* Verschiedene Wahrscheinlichkeitsverteilungen (nach Ort, zeit, Tag, Jahreszeit, Geschwindigkeit...)
* Social Interaction
* Non Android Java version, die in der Dropbox liegt
* Die Android Klasse SyncAdapter nutzen um zukünftige CloudStorage sachen vernünftig zu wrappen
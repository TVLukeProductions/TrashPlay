An diesem Ort entshet die neue Version der App.

# Was sie schon kann (Sozusagen changelog)
## 03.12.2014
* Activity zum abspielen erstellt
* Verbinden zu DropBox
* Synchronisieren von multiplen DropBox Ordnern
* Abspielen eines Songs
* Scrobblen zum TrashPlayList Last.fm account
* Abspielen auch wieder Stoppen (App geht dann aus)
* GitHub Branch "LikeABoss"

## 04.12.2014
* readme.md erstellt
* Dateien aus der App löschen, wenn sie in keiner Playlist mehr sind
* Sicherstellen dass die App nicht versucht Dateien abzuspielen die es nicht mehr gibt
* Erst den PlayButton anzeiegen, wenn es etwas zum abspielen gibt
* In der Mainactivity checken ob die app schon läuft (Dann direkt Stop Button anzeigen)
* MusicPlayer.scrobble überarbeiten
* Musiktitel in der Aktivität anzeigen
* Sync-Anzeige (Mit Animation)
* Es möglich machen die App abzuschalten ohne jemals Musik zu starten
## 06.12.2014
* Fehler in der interaction zwischen Player und MuiscCollectionManager ausgearbeitet
* Song Liste darstellen
* Länge des Stücks und bisher gespielte Zeit in der Main Aktivität einblenden
* Menge der Songs in der Main Activity Anzeigen
* Infobox zwischen Playbutton und Liste
* SongSelection
** Default Wahrscheinlichkeits-SongSelector
** EqualsPlaySongSelector
* Persitenz von Informationen über Songs
* Optinal Next and Previous Button
# Was sie bald können wird
* Sammeln von persönlichen Statistiken
* bessere sync animation
* Exportfunktion 
* Settings Activity anlegen
* Scrobblen zum persönlichen Last.fm account

* Bessere/Mehr Unit-Tests

* Lokalisierung
** Deutsch
** Endlisch

* Interner Mechanismus um die App upzudaten (insb. für die Testphase)?

*Chromecast Integration

# Worüber ich mir so Gedanken mache=
* Redesign von Logos
* Badges
* Verschiedene Wahrscheinlichkeitsverteilungen (nach Ort, zeit, Tag, Jahreszeit, Geschwindigkeit...)
* Social Interaction
* Non Android Java version, die in der Dropbox liegt
* Die Android Klasse SyncAdapter nutzen um zukünftige CloudStorage sachen vernünftig zu wrappen
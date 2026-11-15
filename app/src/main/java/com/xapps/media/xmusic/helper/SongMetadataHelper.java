package com.xapps.media.xmusic.helper;
import com.xapps.media.xmusic.fragment.base.*;
import com.xapps.media.xmusic.fragment.main.*;
import com.xapps.media.xmusic.fragment.settings.*;
import com.xapps.media.xmusic.fragment.info.*;
import com.xapps.media.xmusic.ui.widget.*;
import com.xapps.media.xmusic.ui.lyrics.*;
import com.xapps.media.xmusic.ui.scroll.*;
import com.xapps.media.xmusic.ui.behavior.*;
import com.xapps.media.xmusic.lyrics.*;
import com.xapps.media.xmusic.callback.*;
import com.xapps.media.xmusic.models.*;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import com.xapps.media.xmusic.callback.ArtistLoadListener;
import com.xapps.media.xmusic.callback.SongLoadListener;
import com.xapps.media.xmusic.models.Artist;
import com.xapps.media.xmusic.models.Song;

import java.io.File;
import java.util.ArrayList;

public class SongMetadataHelper {

    private static ArrayList<Song> songs = new ArrayList<>();
    private static ArrayList<Artist> artists = new ArrayList<>();

    public static void getAllSongs(Context context, SongLoadListener listener) {
        if (!songs.isEmpty()) {
            if (listener != null) listener.onComplete(songs);
            return;
        }

        ArrayList<Song> songsList = new ArrayList<>();
        Cursor cursor = null;

        try {
            String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

            String[] projection = {
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.ALBUM_ID,
                    MediaStore.Audio.Media.ALBUM_ARTIST,
                    MediaStore.Audio.Media.YEAR,
                    MediaStore.Audio.Media.TRACK,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.DATE_ADDED,
                    MediaStore.Audio.Media.DATE_MODIFIED,
                    MediaStore.Audio.Media.MIME_TYPE,
                    MediaStore.Audio.Media.SIZE
            };

            cursor = context.getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    null,
                    MediaStore.Audio.Media.DATE_ADDED + " DESC"
            );

            if (cursor == null) {
                if (listener != null) listener.onComplete(songsList);
                return;
            }
            
            int totalSongs = cursor.getCount();
            if (listener != null) {
                listener.onStarted(totalSongs);
            }

            int idIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int pathIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            int titleIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
            int artistIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
            int albumIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
            int albumIdIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
            int albumArtistIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ARTIST);
            int yearIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR);
            int trackIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK);
            int durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
            int dateAddedIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED);
            int dateModifiedIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED);
            int mimeIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE);
            int sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);

            while (cursor.moveToNext()) {
                long songId = cursor.getLong(idIndex);
                String path = cursor.getString(pathIndex);
                String title = cursor.getString(titleIndex);
                String artist = cursor.getString(artistIndex);
                String album = cursor.getString(albumIndex);
                long albumId = cursor.getLong(albumIdIndex);
                String albumArtist = cursor.getString(albumArtistIndex);
                int year = cursor.getInt(yearIndex);
                int track = cursor.getInt(trackIndex);
                long duration = cursor.getLong(durationIndex);
                long dateAdded = cursor.getLong(dateAddedIndex);
                long dateModified = cursor.getLong(dateModifiedIndex);
                String mimeType = cursor.getString(mimeIndex);
                long size = cursor.getLong(sizeIndex);

                if (title == null || title.trim().isEmpty()) {
                    if (path != null) {
                        String name = new File(path).getName();
                        int dot = name.lastIndexOf('.');
                        title = dot > 0 ? name.substring(0, dot) : name;
                    }
                    if (title == null || title.trim().isEmpty()) {
                        title = "Unknown Title";
                    }
                }
                if (artist == null || artist.trim().isEmpty() || "<unknown>".equalsIgnoreCase(artist.trim())) {
                    artist = "Unknown Artist";
                }
                if (album == null || album.trim().isEmpty()) {
                    album = "Unknown Album";
                }

                Song song = new Song(songId, path, title, artist, album, albumId, albumArtist, year, track, duration, dateAdded, dateModified, mimeType, size);
                songsList.add(song);

                if (listener != null) {
                    listener.onProgress(songsList, songsList.size());
                }
            }

            songs = songsList;

            if (listener != null) {
                listener.onComplete(songsList);
            }
        } catch (Exception e) {
            if (listener != null) listener.onError(e);
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    public static void getAllArtists(Context context, ArtistLoadListener listener) {
        if (!artists.isEmpty()) {
            if (listener != null) listener.onComplete(artists);
            return;
        }

        ArrayList<Artist> artistList = new ArrayList<>();
        Cursor cursor = null;

        try {
            String[] projection = {
                    MediaStore.Audio.Artists._ID,
                    MediaStore.Audio.Artists.ARTIST,
                    MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
                    MediaStore.Audio.Artists.NUMBER_OF_TRACKS
            };

            cursor = context.getContentResolver().query(
                    MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    MediaStore.Audio.Artists.ARTIST + " ASC"
            );

            if (cursor == null) {
                if (listener != null) listener.onComplete(artistList);
                return;
            }

            int totalArtists = cursor.getCount();
            if (listener != null) {
                listener.onStarted(totalArtists);
            }

            int idIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID);
            int nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST);
            int numAlbumsIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS);
            int numTracksIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_TRACKS);

            while (cursor.moveToNext()) {
                long artistId = cursor.getLong(idIndex);
                String name = cursor.getString(nameIndex);
                int numAlbums = cursor.getInt(numAlbumsIndex);
                int numTracks = cursor.getInt(numTracksIndex);

                if (name == null || name.trim().isEmpty() || "<unknown>".equalsIgnoreCase(name.trim())) {
                    name = "Unknown Artist";
                }

                Artist artist = new Artist(artistId, name, numAlbums, numTracks);
                artistList.add(artist);

                if (listener != null) {
                    listener.onProgress(artistList, artistList.size());
                }
            }

            artists = artistList;

            if (listener != null) {
                listener.onComplete(artistList);
            }
        } catch (Exception e) {
            if (listener != null) listener.onError(e);
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    public static void clearCachedList() {
        songs = new ArrayList<>();
        artists = new ArrayList<>();
    }
}
package com.xapps.media.xmusic.helper;

import com.xapps.media.xmusic.data.RuntimeData;
import com.xapps.media.xmusic.models.Song;

import java.util.ArrayList;
import java.util.Locale;

public class SongSearchHelper {

    public static ArrayList<Song> search(
            String query,
            boolean searchTitle,
            boolean searchArtist,
            boolean searchAlbum,
            boolean searchAlbumArtist
    ) {
        ArrayList<Song> results = new ArrayList<>();

        if (query == null || query.trim().isEmpty()) {
            results.addAll(RuntimeData.songs);
            return results;
        }

        String lowerQuery = query.toLowerCase(Locale.ROOT);

        for (Song song : RuntimeData.songs) {
            if (searchTitle && searchArtist && searchAlbum && searchAlbumArtist) {
                if (song.getSearchKey().contains(lowerQuery)) {
                    results.add(song);
                }
                continue;
            }

            boolean match = false;

            if (searchTitle && contains(song.title, lowerQuery)) {
                match = true;
            }

            if (!match && searchArtist && contains(song.artist, lowerQuery)) {
                match = true;
            }

            if (!match && searchAlbum && contains(song.album, lowerQuery)) {
                match = true;
            }

            if (!match && searchAlbumArtist && contains(song.albumArtist, lowerQuery)) {
                match = true;
            }

            if (match) {
                results.add(song);
            }
        }

        return results;
    }

    private static boolean contains(String source, String query) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(query);
    }
}
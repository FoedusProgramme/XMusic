package com.xapps.media.xmusic.helper;

import android.os.Handler;
import android.os.Looper;

import com.xapps.media.xmusic.models.Song;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SongSorter {

    private static final ExecutorService sortExecutor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface SortListener {
        void onSortComplete(ArrayList<Song> sortedList);
    }

    public enum SortBy {
        TITLE,
        ARTIST,
        ALBUM,
        ALBUM_ARTIST,
        YEAR,
        TRACK,
        DURATION,
        DATE_ADDED,
        DATE_MODIFIED,
        SIZE
    }

    public static void sort(ArrayList<Song> inputList, SortBy criteria, boolean ascending, SortListener listener) {
        sortExecutor.execute(() -> {
            ArrayList<Song> sortedList = new ArrayList<>(inputList);

            Collections.sort(sortedList, (s1, s2) -> {
                int result;

                switch (criteria) {
                    case TITLE:
                        result = compareStrings(s1.title, s2.title);
                        break;

                    case ARTIST:
                        result = compareStrings(s1.artist, s2.artist);
                        break;

                    case ALBUM:
                        result = compareStrings(s1.album, s2.album);
                        break;

                    case ALBUM_ARTIST:
                        result = compareStrings(s1.albumArtist, s2.albumArtist);
                        break;

                    case YEAR:
                        result = Integer.compare(s1.year, s2.year);
                        break;

                    case TRACK:
                        result = Integer.compare(s1.track, s2.track);
                        break;

                    case DURATION:
                        result = Long.compare(s1.duration, s2.duration);
                        break;

                    case DATE_ADDED:
                        result = Long.compare(s1.dateAdded, s2.dateAdded);
                        break;

                    case DATE_MODIFIED:
                        result = Long.compare(s1.dateModified, s2.dateModified);
                        break;

                    case SIZE:
                        result = Long.compare(s1.size, s2.size);
                        break;

                    default:
                        result = 0;
                }

                return ascending ? result : -result;
            });

            mainHandler.post(() -> {
                if (listener != null) {
                    listener.onSortComplete(sortedList);
                }
            });
        });
    }

    private static int compareStrings(String s1, String s2) {
        if (s1 == s2) return 0;
        if (s1 == null) return 1;
        if (s2 == null) return -1;
        return s1.compareToIgnoreCase(s2);
    }
}
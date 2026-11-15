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

import android.os.Handler;
import android.os.Looper;

import com.xapps.media.xmusic.models.Artist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ArtistSorter {

    private static final ExecutorService sortExecutor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface SortListener {
        void onSortComplete(ArrayList<Artist> sortedList);
    }

    public enum SortBy {
        ALPHABETIC,
        SONG_COUNT
    }

    public static void sort(ArrayList<Artist> inputList, SortBy criteria, boolean ascending, SortListener listener) {
        sortExecutor.execute(() -> {
            ArrayList<Artist> sortedList = new ArrayList<>(inputList);

            Collections.sort(sortedList, (a1, a2) -> {
                int result;

                switch (criteria) {
                    case ALPHABETIC:
                        result = compareStrings(a1.name, a2.name);
                        break;

                    case SONG_COUNT:
                        result = Integer.compare(a1.numberOfTracks, a2.numberOfTracks);
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
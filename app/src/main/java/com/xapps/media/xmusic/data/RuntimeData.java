package com.xapps.media.xmusic.data;
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
import com.xapps.media.xmusic.models.Song;
import java.util.ArrayList;
import java.util.HashMap;

public class RuntimeData {
    public static long currentProgress = -1;
    public static ArrayList<Song> songs = new ArrayList<>();
    public static ArrayList<com.xapps.media.xmusic.models.Artist> artists = new ArrayList<>();
}

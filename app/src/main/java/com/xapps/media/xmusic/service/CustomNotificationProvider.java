package com.xapps.media.xmusic.service;
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
import androidx.annotation.Nullable;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.DefaultMediaNotificationProvider;

@UnstableApi
public class CustomNotificationProvider extends DefaultMediaNotificationProvider {

    public CustomNotificationProvider(Context context) {
        super(context);
    }

    @Nullable
    @Override
    protected CharSequence getNotificationContentTitle(MediaMetadata metadata) {
        CharSequence title = metadata.title;
        if (title == null || title.length() == 0) {
            title = metadata.displayTitle;
        }
        if (title == null || title.length() == 0) {
            title = "Unknown Title";
        }
        return title;
    }

    @Nullable
    @Override
    protected CharSequence getNotificationContentText(MediaMetadata metadata) {
        CharSequence artist = metadata.artist;
        if (artist == null || artist.length() == 0) {
            artist = metadata.subtitle;
        }
        if (artist == null || artist.length() == 0) {
            artist = "Unknown Artist";
        }
        return artist;
    }
}

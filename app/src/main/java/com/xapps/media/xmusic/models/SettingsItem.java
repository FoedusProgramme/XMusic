package com.xapps.media.xmusic.models;
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
import androidx.fragment.app.Fragment;

public class SettingsItem {
    
    public static final int TYPE_HEADER = 0;
    public static final int TYPE_NAV = 1;
    public static final int TYPE_SWITCH = 2;

    public int type;
    public String title;
    public String id;
    public String description;
    public Fragment destinationFragment;

    public SettingsItem(int itemType, String id, String title, String desc, Fragment frag) {
        this.id = id;
        this.type = itemType;
        this.title = title;
        this.description = desc;
        this.destinationFragment = frag;
    }
}

package de.lukeslog.trashplay.ui;

import de.lukeslog.trashplay.R;
import de.lukeslog.trashplay.constants.TrashPlayConstants;
import de.lukeslog.trashplay.support.Logger;

public class IconHelper {

    public static final String TAG = TrashPlayConstants.TAG;

    public static final int[] ICON_PLAY = {R.drawable.play};
    public static final int[] ICON_RADIO = {R.drawable.radio2, R.drawable.radio3, R.drawable.radio4};
    public static final int[] ICON_RADIO_INVISIBLE = {R.drawable.transp};
    public static final int[] ICON_STOP = {R.drawable.pause};
    public static final int[] ICON_WIFI = {R.drawable.wifi};
    public static final int[] ICON_WIFI_INVISIBLE = {R.drawable.transp};
    public static final int[] ICON_NEXT = {R.drawable.ic_action_playback_next};
    public static final int[] ICON_PREV = {R.drawable.ic_action_playback_prev};
    public static final int[] ICON_SYNC = {R.drawable.synchronize1, R.drawable.synchronize2};
    public static final int[] ICON_SYNC_INVISIBLE = {R.drawable.transp};
    public static final int[] ICON_SYNC_HEADPHONES = {R.drawable.headphones};
    public static final int[] ICON_SYNC_HEADPHONES_INVISIBLE = {R.drawable.transp};
    public static final int[] SANTA = {R.drawable.santa1, R.drawable.santa2, R.drawable.santa3, R.drawable.santa4, R.drawable.santa5};


    public static int getIcon(int[] icon, int animationcounter) {
        int i = animationcounter%icon.length;
        return icon[i];
    }
}

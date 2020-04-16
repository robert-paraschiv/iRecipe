package com.rokudoz.irecipe.Utils;

import android.content.res.Resources;
import android.util.DisplayMetrics;

public class ConvertDpToPixel {

    public ConvertDpToPixel() {
    }

    //This function to convert DPs to pixels
    public int convertDpToPixel(float dp) {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return Math.round(px);
    }
}

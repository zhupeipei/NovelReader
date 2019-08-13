package com.kooreader.util;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

/**
 * Created by nali on 2018/11/21.
 */

public class PadAdaptUtil {

    public static int getWidth(Activity activity) {
        if (activity == null)
            return 0;
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        display.getMetrics(dm);
        int width;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            //横屏进入华为2560*1600大屏平板时，一般方法获取宽高值不准确，故采用这个方法
            Point point = new Point();
            display.getRealSize(point);
//            height = point.y;
            width = point.x;
        } else {
//            height = dm.heightPixels;
            width = dm.widthPixels;
        }
        return width;
    }

    public static int getHeight(Activity activity) {
        if (activity == null)
            return 0;
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        display.getMetrics(dm);
        int height;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            //横屏进入华为2560*1600大屏平板时，一般方法获取宽高值不准确，故采用这个方法
            Point point = new Point();
            display.getRealSize(point);
            height = point.y;
//            width = point.x;
        } else {
            height = dm.heightPixels;
//            width = dm.widthPixels;
        }
        return height;
    }

    public static boolean isPad(Context context) {
        return context != null && (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    /**
     * 获取大平板、水平方向的宽度
     *
     * @param activity
     * @return
     */
    public static int getLandscapeWidth(Activity activity) {
        if (activity == null) {
            return WindowManager.LayoutParams.MATCH_PARENT;
        }
        return Math.min(PadAdaptUtil.getWidth(activity), PadAdaptUtil.getHeight(activity));
    }

    public static int getMatchParentWidth(Activity activity) {
        if (activity == null) {
            return WindowManager.LayoutParams.MATCH_PARENT;
        }

        if (isPadLandscape(activity)) {
            return getLandscapeWidth(activity);
        }

        return WindowManager.LayoutParams.MATCH_PARENT;
    }

    public static boolean isPadLandscape(Activity activity) {
        try {
            Resources resources = activity.getResources();

            return PadAdaptUtil.isPad(activity)
                    && resources.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public static int getHorizontalPadding(Activity activity) {
        if (isPadLandscape(activity)) {
            return getPadPaddingValue(activity);
        }
        return 0;
    }

    public static int getPadPaddingValue(Activity activity) {
        int width = PadAdaptUtil.getWidth(activity);
        int height = PadAdaptUtil.getHeight(activity);

        return Math.abs(width - height) / 2;
    }


    public static void changeScreenWidth(Activity activity, int padding) {
        if (activity == null || BaseUtil.isHWANL_AN00())
            return;
        View rootView = activity.getWindow().getDecorView().findViewById(android.R.id.content);
        ViewGroup.LayoutParams params = rootView.getLayoutParams();
        ViewGroup.MarginLayoutParams marginLayoutParams;
        if(params instanceof ViewGroup.MarginLayoutParams) {
            marginLayoutParams = (ViewGroup.MarginLayoutParams) params;
        } else {
            marginLayoutParams = new ViewGroup.MarginLayoutParams(params);
        }
        marginLayoutParams.setMargins(padding,0,padding,0);
        rootView.setLayoutParams(marginLayoutParams);
    }

    public static void setMargins(View view, int padding) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        ViewGroup.MarginLayoutParams marginLayoutParams;
        if(params != null && params instanceof ViewGroup.MarginLayoutParams) {
            marginLayoutParams = (ViewGroup.MarginLayoutParams) params;
        } else {
            if(params == null) {
                params = new ViewGroup.LayoutParams(view.getWidth(),view.getHeight());
            }
            marginLayoutParams = new ViewGroup.MarginLayoutParams(params);
        }
        marginLayoutParams.setMargins(padding,0,padding,0);
        view.setLayoutParams(marginLayoutParams);
    }

    public static boolean isHWPad_CMR_AL09() {
        return "CMR-AL09".equals(Build.MODEL);
    }

}

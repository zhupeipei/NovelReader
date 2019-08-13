package com.kooreader.util;

import android.os.Build;
import android.view.View;
import android.view.Window;

/**
 * @author zhupeipei
 * @date 2019/4/17 16:58
 */
public class NavigationUtil {

    /**
     * 隐藏导航栏
     * @param window
     * @param hide
     */
    public static void hideNavigationBar(Window window, boolean hide) {
        if (window == null) {
            return;
        }

        View decorView = window.getDecorView();
        if (decorView != null) {

            int ui = decorView.getSystemUiVisibility();
            if (hide) {
                ui |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
                if (Build.VERSION.SDK_INT >= 19) {
                    ui |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
                }
            } else {
                ui &= ~View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
                if (Build.VERSION.SDK_INT >= 19) {
                    ui &= ~View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
                }
            }

            decorView.setSystemUiVisibility(ui);
        }
    }
}

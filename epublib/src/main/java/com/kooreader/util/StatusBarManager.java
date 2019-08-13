package com.kooreader.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by jack.qin on 2016/10/9.
* @author jack.qin
 */
public class StatusBarManager {

    public static boolean CAN_CHANGE_STATUSBAR_COLOR;
    /**
     * 刘海屏
     */
    public static boolean FEATURE_BANG_SCREEN;
    public static void canChangeColor(Window window) {
        if(!CAN_CHANGE_STATUSBAR_COLOR) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                CAN_CHANGE_STATUSBAR_COLOR = true;
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (isMeiZu(window) || isXiaoMi(window)) {
                    CAN_CHANGE_STATUSBAR_COLOR = true;
                }
            } else {
                CAN_CHANGE_STATUSBAR_COLOR = false;
            }
            FEATURE_BANG_SCREEN = isBangScreen(window.getContext());
        }
    }

    private static boolean isBangScreen(Context context) {
        if (context != null) {
            String model = Build.MANUFACTURER.toLowerCase();
            if ("oppo".equals(model)) {
                return isOppoBangScreen(context);
            }
        }
        return false;
    }

    /***
     * oppo 凹形屏
     */
    private static boolean isOppoBangScreen(@NonNull Context context) {
        return context.getPackageManager().hasSystemFeature("com.oppo.feature.screen.heteromorphism");
    }

    static boolean isXiaoMi(Window window) {
        if (!("Xiaomi".equals(Build.MANUFACTURER))) {
            return false;
        }

        Class<? extends Window> clazz = window.getClass();
        try {
            Class<?> layoutParams = Class.forName("android.view.MiuiWindowManager$LayoutParams");
            Field field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE");
            Method extraFlagField = clazz.getMethod("setExtraFlags", int.class, int.class);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    static boolean isMeiZu(Window window) {
        try {
            WindowManager.LayoutParams.class.getDeclaredField("MEIZU_FLAG_DARK_STATUS_BAR_ICON");
            WindowManager.LayoutParams.class.getDeclaredField("meizuFlags");
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static void transparencyBar(@NonNull Activity activity) {
        Window window = activity.getWindow();
        transparencyBar(window);
    }

    // 在DialogFragment全屏显示时需要传入的window是getDialog().getWindow()而不是getActivity().getWindow() 仔细想下就知道为什么
    public static void transparencyBar(@NonNull Window window) {
        if (!CAN_CHANGE_STATUSBAR_COLOR) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            int systemUiVisibility = window.getDecorView().getSystemUiVisibility();
            systemUiVisibility |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            window.getDecorView().setSystemUiVisibility(systemUiVisibility);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    private static boolean mIsNightMode = false;
    public static void setIsNightMode(boolean isNightMode){
        mIsNightMode = isNightMode;
    }

    public static void hideStatusBar(Window window, boolean hide){
        if (window == null) {
            return;
        }

        View decorView = window.getDecorView();
        if(decorView != null) {
            int ui = decorView.getSystemUiVisibility();
            if (hide) {
                ui |= View.SYSTEM_UI_FLAG_FULLSCREEN;
            } else {
                ui &= ~View.SYSTEM_UI_FLAG_FULLSCREEN;
            }
            decorView.setSystemUiVisibility(ui);
        }
    }


    private static boolean lastStatusBar = false;
    private static WeakReference<Window> lastWindow;
    private static boolean isSetBarColor = false;
    public static void setStatusBarColor(Window window, boolean lightStatusBar) {//true黑色；false白色
        if (window == null) {
            return;
        }

        if(mIsNightMode){
            lightStatusBar = true;
        }

        if(isSetBarColor && lastStatusBar == lightStatusBar
                && lastWindow != null && lastWindow.get() != null && lastWindow.get() == window) {
            return;
        }

        isSetBarColor = true;
        lastStatusBar = lightStatusBar;
        lastWindow = new WeakReference<Window>(window);

        if(CAN_CHANGE_STATUSBAR_COLOR) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setStatusBarColorForM(window, lightStatusBar);
                if(MobileConfigUtil.isMIUI()) {
                    setStatusBarColorForMi(window, lightStatusBar);
                }
                if (MobileConfigUtil.isFlymeOS()) {
                    setStatusBarColorForMeiZu(window, lightStatusBar);
                }
            }else {
                if(!setStatusBarColorForMi(window, lightStatusBar)) {
                    setStatusBarColorForMeiZu(window, lightStatusBar);
                }
            }

        }
//        if (CAN_CHANGE_STATUSBAR_COLOR && !setStatusBarColorForMi(window, lightStatusBar)) {
//            if (!setStatusBarColorForMeiZu(window, lightStatusBar)) {
//                setStatusBarColorForM(window, lightStatusBar);
//            }
//        }
    }

    private static boolean setStatusBarColorForMi(Window window, boolean lightStatusBar) {
        if (window == null) {
            return false;
        }
        Class<? extends Window> clazz = window.getClass();
        try {
            Class<?> layoutParams = Class.forName("android.view.MiuiWindowManager$LayoutParams");
            Field field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE");
            int darkModeFlag = field.getInt(layoutParams);
            Method extraFlagField = clazz.getMethod("setExtraFlags", int.class, int.class);
            extraFlagField.invoke(window, lightStatusBar ? darkModeFlag : 0, darkModeFlag);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * 设置状态栏图标为深色和魅族特定的文字风格
     *
     * @param window         需要设置的窗口
     * @param lightStatusBar 是否把状态栏颜色设置为深色，true 深色
     * @return boolean 成功执行返回true
     */
    private static boolean setStatusBarColorForMeiZu(Window window, boolean lightStatusBar) {
        try{
            FlymeStatusbarColorUtils.setStatusBarDarkIcon(window, lightStatusBar);
        }catch (Throwable e){
            e.printStackTrace();

            return false;
        }

        return true;
    }

    private static boolean isErrored = false;
    private static void setStatusBarColorForM(Window window, boolean lightStatusBar) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }

        View decor = window.getDecorView();
        int ui = decor.getSystemUiVisibility();

        //颜色相同，直接return
        if((ui & View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)== View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR && lightStatusBar ||
                (ui & View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR) == 0 && !lightStatusBar){
            return;
        }

        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.TRANSPARENT);

        if (lightStatusBar) {
            ui |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        } else {
            ui &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        decor.setSystemUiVisibility(ui);//系统设置方法

        View v = window.findViewById(android.R.id.content);
        if (v != null) {
            v.setForeground(null);
        }

        if(!isErrored) {
            //以下专门给系统手机设置不生效的手机设置
            Class localClass = window.getClass();
            try {
                Class[] arrayOfClass = new Class[1];
                arrayOfClass[0] = Integer.TYPE;
                Method localMethod = localClass.getMethod("setStatusBarIconColor", arrayOfClass);
                Object[] arrayOfObject = new Object[1];
                if (lightStatusBar) {
                    arrayOfObject[0] = Integer.valueOf(Color.BLACK);
                } else {
                    arrayOfObject[0] = Integer.valueOf(Color.WHITE);
                }
                localMethod.invoke(window, arrayOfObject);
            } catch (Exception e) {
                e.printStackTrace();
                isErrored = true;
            }
        }
    }

    private static final double COLOR_THRESHOLD = 180.0;

    private static boolean isTextColorSimilar(int baseColor, int color) {
        int simpleBaseColor = baseColor | 0xff000000;
        int simpleColor = color | 0xff000000;
        int baseRed = Color.red(simpleBaseColor) - Color.red(simpleColor);
        int baseGreen = Color.green(simpleBaseColor) - Color.green(simpleColor);
        int baseBlue = Color.blue(simpleBaseColor) - Color.blue(simpleColor);
        double value = Math.sqrt(baseRed * baseRed + baseGreen * baseGreen + baseBlue * baseBlue);
        if (value < COLOR_THRESHOLD) {
            return true;
        }
        return false;
    }

}

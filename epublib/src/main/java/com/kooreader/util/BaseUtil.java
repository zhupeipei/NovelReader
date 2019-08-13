package com.kooreader.util;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Build;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.ViewConfiguration;
import android.view.WindowManager;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * ClassName:BaseUtil Function: TODO ADD FUNCTION Reason: TODO ADD REASON
 *
 * @author jack.qin
 * @Date 2015-4-20 下午3:54:37
 * @see
 * @since Ver 1.1
 */
public class BaseUtil {

    public static int dp2px(Context context, float dipValue) {
        if (context == null)
            return (int) (dipValue * 1.5);
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    public static float dp2pxReturnFloat(Context context, float dipValue) {
        if (context == null)
            return dipValue * 1.5f;
        final float scale = context.getResources().getDisplayMetrics().density;
        return dipValue * scale + 0.5f;
    }

    public static int sp2px(Context context, float dipValue) {
        if (context == null)
            return (int) (dipValue * 1.5);
        final float scale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (dipValue * scale + 0.5f);
    }

    public static int px2dip(Context context, float pxValue) {
        if (context == null)
            return (int) (pxValue * 1.5);
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    public static int px2sp(Context context, float pxValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (pxValue / fontScale + 0.5f);
    }

    /**
     * 优先从Activity获取WindowManager
     * @param context
     * @return
     */
    private static WindowManager getWindowManagerForActivity(Context context) {
        WindowManager windowManager = null;
//        if (!(context instanceof Activity)) {
//            Activity activity = BaseApplication.getTopActivity();
//            if (activity != null) {
//                windowManager = activity.getWindowManager();
//            }
//        }
        if (windowManager == null) {
            windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        }
        return windowManager;
    }

    /**
     * get the width of the device screen
     *
     * @param context
     * @return
     */
    public static int getScreenWidth(Context context) {
        if (context == null) {
            return 1;
        }
        //平板横屏时，采用界面居中、左右留白的方案，此时宽高比为1:1，故宽度取短边的值，华为折叠屏手机展开相当于平板，但不需要采用左右留白的方案
//        if (PadAdaptUtil.isPadLandscape(BaseApplication.getTopActivity()) && !isHWANL_AN00()) {
//            return PadAdaptUtil.getLandscapeWidth(BaseApplication.getTopActivity());
//        }

        // 优先从Activity获取WindowManager，因为在分屏时Activity的WindowManager可以拿到分屏后的大小。
        WindowManager wm = getWindowManagerForActivity(context);
        if(wm == null) {
            return 1;
        }
        Point size = new Point();
        wm.getDefaultDisplay().getSize(size);
        return size.x;
    }

    /**
     * get the height of the device screen
     *
     * @param context
     * @return
     */
    public static int getScreenHeight(Context context) {
        if (context == null) {
            return 1;
        }

        if(PadAdaptUtil.isPad(context)) {
            //平板横屏时，采用界面居中、左右留白的方案，此时宽高比为1:1，故宽度取短边的值，高度取当前屏幕的高度；竖屏逻辑正常
            return 0; // PadAdaptUtil.getHeight(BaseApplication.getTopActivity());
        } else {
            WindowManager wm = getWindowManagerForActivity(context);
            if (wm == null) {
                return 1;
            }
            Point size = new Point();
            wm.getDefaultDisplay().getSize(size);
            return size.y;
        }
    }

    public static boolean isHWANL_AN00() {
        return "ANL-AN00".equals(Build.MODEL);
    }

    public static boolean isHWFoldLargeScreen(Context context) {
        return isHWANL_AN00() && getScreenWidth(context) >= 2200;
    }

    /**
     * 获取状态栏的高度
     */
    public static int mStatusBarHeight = 0;
    private static boolean mIsStatusBarHeightCached = false;

    public static int getStatusBarHeight(Context context) {
        if (context == null)
            return 0;
        if (mStatusBarHeight != 0) {
            return mStatusBarHeight;
        }
        try {
            Class<?> clazz = Class.forName("com.android.internal.R$dimen");
            Object obj = clazz.newInstance();
            Field field = clazz.getField("status_bar_height");
            int temp = Integer.parseInt(field.get(obj).toString());
            mStatusBarHeight = context.getResources()
                    .getDimensionPixelSize(temp);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //缓存statusBar高度，防止以后获取不到高度
//        if (mStatusBarHeight == 0) {
//            mStatusBarHeight = SharedPreferencesUtil.getInstance(context).
//                    getInt(PreferenceConstantsInOpenSdk.OPENSDK_KEY_STATUS_BAR_HEIGHT, 0);
//        } else {
//            if (!mIsStatusBarHeightCached) {
//                mIsStatusBarHeightCached = true;
//                SharedPreferencesUtil.getInstance(context).
//                        saveInt(PreferenceConstantsInOpenSdk.OPENSDK_KEY_STATUS_BAR_HEIGHT, mStatusBarHeight);
//            }
//        }

        return mStatusBarHeight;
    }

    /**
     * 是否是平板
     *
     * @param context
     * @return
     */
    public static boolean isTabletDevice(Context context) {
        if (context == null) {
            return false;
        }
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >=
                Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static int[] getNotchSize(Context context) {

        int[] ret = new int[]{0, 0};

        try {

            ClassLoader cl = context.getClassLoader();

            Class HwNotchSizeUtil = cl.loadClass("com.huawei.android.util.HwNotchSizeUtil");

            Method get = HwNotchSizeUtil.getMethod("getNotchSize");

            ret = (int[]) get.invoke(HwNotchSizeUtil);

        } catch (ClassNotFoundException e) {

            Log.e("test", "getNotchSize ClassNotFoundException");

        } catch (NoSuchMethodException e) {

            Log.e("test", "getNotchSize NoSuchMethodException");

        } catch (Exception e) {

            Log.e("test", "getNotchSize Exception");

        } finally {

            return ret;

        }

    }

//    public static void getTileBarView(ViewGroup viewGroup, Context context, View[] result) {
//        if (viewGroup == null) {
//            return;
//        }
//        int count = viewGroup.getChildCount();
//        for (int i = 0; i < count; i++) {
//            View view = viewGroup.getChildAt(i);
//            CharSequence des = view.getContentDescription();
//            if (!TextUtils.isEmpty(des)) {
//                if (des.equals(context.getString(R.string.framework_title_bar_contentDescription))) {
//                    if (result[0] == null) {
//                        result[0] = view;
//                        if (!StatusBarManager.FEATURE_BANG_SCREEN) {
//                            return;
//                        }
//                    }
//                }
//                if (StatusBarManager.FEATURE_BANG_SCREEN) {
//                    if (des.equals(context.getString(R.string.framework_bang_screen_contentDescription))) {
//                        result[1] = view;
//                        if (result[0] != null) {
//                            return;
//                        }
//                    }
//                }
//            } else if (view instanceof ViewGroup) {
//                getTileBarView((ViewGroup) view, context, result);
//            }
//        }
//    }


    /**
     * 获取虚拟按键栏高度
     *
     * @param context
     * @return
     */
    public static int getNavigationBarHeight(Context context) {
        if (isMeizu()) {
            return getSmartBarHeight(context);
        }
        int result = 0;
        if (hasNavBar(context)) {
            Resources res = context.getResources();
            int resourceId = res.getIdentifier("navigation_bar_height", "dimen", "android");
            if (resourceId > 0) {
                result = res.getDimensionPixelSize(resourceId);
            }
        }
        return result;
    }

    /**
     * 检查是否存在虚拟按键栏
     *
     * @param context
     * @return
     */
    public static boolean hasNavBar(Context context) {
        if (context == null) {
            return false;
        }

        if (isSmartisanR1()) {
            return true;
        }

        Resources res = context.getResources();
        int resourceId = res.getIdentifier("config_showNavigationBar", "bool", "android");
        if (resourceId != 0) {
            boolean hasNav = res.getBoolean(resourceId);
            // check override flag
            String sNavBarOverride = getNavBarOverride();
            if ("1".equals(sNavBarOverride)) {
                hasNav = false;
            } else if ("0".equals(sNavBarOverride)) {
                hasNav = true;
            }
            return hasNav;
        } else { // fallback
            return !ViewConfiguration.get(context).hasPermanentMenuKey();
        }
    }

    /**
     * 判断虚拟按键栏是否重写
     *
     * @return
     */
    private static String getNavBarOverride() {
        String sNavBarOverride = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                Class c = Class.forName("android.os.SystemProperties");
                Method m = c.getDeclaredMethod("get", String.class);
                m.setAccessible(true);
                sNavBarOverride = (String) m.invoke(null, "qemu.hw.mainkeys");
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return sNavBarOverride;
    }

    /**
     * 判断是否meizu手机
     *
     * @return
     */
    public static boolean isMeizu() {
        return Build.BRAND.equals("Meizu");
    }

    /**
     * 获取魅族手机底部虚拟键盘高度
     *
     * @param context
     * @return
     */
    public static int getSmartBarHeight(Context context) {
        if (context == null)
            return 0;

        try {
            Class c = Class.forName("com.android.internal.R$dimen");
            Object obj = c.newInstance();
            Field field = c.getField("mz_action_button_min_height");
            int height = Integer.parseInt(field.get(obj).toString());
            return context.getResources().getDimensionPixelSize(height);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }


    public static PackageInfo getPackageArchiveInfo(PackageManager packageManager, String archiveFilePath, int flags) {
        // Workaround for https://code.google.com/p/android/issues/detail?id=9151#c8

        return packageManager.getPackageArchiveInfo(archiveFilePath, flags);


//		try {
//			Class packageParserClass = Class.forName(
//					"android.content.pm.PackageParser");
//			Class[] innerClasses = packageParserClass.getDeclaredClasses();
//			Class packageParserPackageClass = null;
//			for (Class innerClass : innerClasses) {
//				if (0 == innerClass.getName().compareTo("android.content.pm.PackageParser$Package")) {
//					packageParserPackageClass = innerClass;
//					break;
//				}
//			}
//			Constructor packageParserConstructor = packageParserClass.getConstructor(
//					String.class);
//			Method parsePackageMethod = packageParserClass.getDeclaredMethod(
//					"parsePackage", File.class, String.class, DisplayMetrics.class, int.class);
//			Method collectCertificatesMethod = packageParserClass.getDeclaredMethod(
//					"collectCertificates", packageParserPackageClass, int.class);
//			Method generatePackageInfoMethod = packageParserClass.getDeclaredMethod(
//					"generatePackageInfo", packageParserPackageClass, int[].class, int.class, long.class, long.class);
//			packageParserConstructor.setAccessible(true);
//			parsePackageMethod.setAccessible(true);
//			collectCertificatesMethod.setAccessible(true);
//			generatePackageInfoMethod.setAccessible(true);
//
//			Object packageParser = packageParserConstructor.newInstance(archiveFilePath);
//
//			DisplayMetrics metrics = new DisplayMetrics();
//			metrics.setToDefaults();
//
//			final File sourceFile = new File(archiveFilePath);
//
//			Object pkg = parsePackageMethod.invoke(
//					packageParser,
//					sourceFile,
//					archiveFilePath,
//					metrics,
//					0);
//			if (pkg == null) {
//				return null;
//			}
//
//			if ((flags & android.content.pm.PackageManager.GET_SIGNATURES) != 0) {
//				collectCertificatesMethod.invoke(packageParser, pkg, 0);
//			}
//
//			return (PackageInfo) generatePackageInfoMethod.invoke(null, pkg, null, flags, 0, 0);
//		} catch (Exception e) {
//			Log.e("Signature Monitor",
//					"android.content.pm.PackageParser reflection failed: " + e.toString());
//		}
//
//		return null;
    }

    public static boolean verifyPluginFileSignature(Context context, String pluginFilepath) {
        File pluginFile = new File(pluginFilepath);
        if (!pluginFile.exists()) {
            return false;
        }

        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo newInfo = getPackageArchiveInfo(packageManager, pluginFilepath, PackageManager.GET_ACTIVITIES | PackageManager.GET_SIGNATURES);
            PackageInfo mainPkgInfo = packageManager.getPackageInfo(
                    context.getPackageName(), PackageManager.GET_SIGNATURES);
            return checkSignatures(newInfo, mainPkgInfo) == PackageManager.SIGNATURE_MATCH;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    private static int checkSignatures(PackageInfo pluginPackageInfo,
                                       PackageInfo mainPackageInfo) {

        Signature[] pluginSignatures = pluginPackageInfo.signatures;
        Signature[] mainSignatures = mainPackageInfo.signatures;
        boolean pluginSigned = pluginSignatures != null
                && pluginSignatures.length > 0;
        boolean mainSigned = mainSignatures != null
                && mainSignatures.length > 0;

        if (pluginSignatures != null && mainSignatures != null)
            Log.d("checkSignatures ", pluginSignatures.length + "  " + mainSignatures.length);

        if (!pluginSigned && !mainSigned) {
            return PackageManager.SIGNATURE_NEITHER_SIGNED;
        } else if (!pluginSigned && mainSigned) {
            return PackageManager.SIGNATURE_FIRST_NOT_SIGNED;
        } else if (pluginSigned && !mainSigned) {
            return PackageManager.SIGNATURE_SECOND_NOT_SIGNED;
        } else {
            if (pluginSignatures.length == mainSignatures.length) {
                for (int i = 0; i < pluginSignatures.length; i++) {
                    Signature s1 = pluginSignatures[i];
                    Signature s2 = mainSignatures[i];
                    if (!Arrays.equals(s1.toByteArray(), s2.toByteArray())) {
                        return PackageManager.SIGNATURE_NO_MATCH;
                    }
                }
                return PackageManager.SIGNATURE_MATCH;
            } else {
                return PackageManager.SIGNATURE_NO_MATCH;
            }
        }
    }

    public static boolean activityIsLive(Activity activity) {
        if (activity == null) {
            return false;
        }
        if (activity.isFinishing()) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if (activity.isDestroyed()) {
                return false;
            }
        }
        return true;
    }

    private static int startCount = 0;
    public static boolean hasReadStart = false;

    public static int getSceenWidthForDp(Context context) {
        if (context == null)
            return 1;
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int width = dm.widthPixels;// 屏幕宽度（像素）
        float density = dm.density;//屏幕密度（0.75 / 1.0 / 1.5）
        //屏幕宽度算法:屏幕宽度（像素）/屏幕密度
        int screenWidth = (int) (width / density);//屏幕宽度(dp)
        return screenWidth;
    }

    /**
     * 获取屏幕尺寸，但是不包括虚拟功能高度
     *
     * @return
     */
    public static int getNoVirtualNavBarScreenHeight(Context context) {
        if (context == null) {
            return 0;
        }

        WindowManager wm = (WindowManager) context.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) {
            return 0;
        }

        Display d = wm.getDefaultDisplay();
        if (d == null) {
            return 0;
        }

        DisplayMetrics dm = new DisplayMetrics();
        d.getMetrics(dm);

        return dm.heightPixels;
    }

    /**
     * 通过反射，获取包含虚拟键的整体屏幕高度
     *
     * @return 包含虚拟键的整体屏幕高度
     */
    public static int getHasVirtualNavBarScreenHeight(Context context) {
        if (context == null) {
            return 0;
        }

        WindowManager wm = (WindowManager) context.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) {
            return 0;
        }

        int px = 0;
        Display display = wm.getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        @SuppressWarnings("rawtypes")
        Class c;
        try {
            c = Class.forName("android.view.Display");
            @SuppressWarnings("unchecked")
            Method method = c.getMethod("getRealMetrics", DisplayMetrics.class);
            method.invoke(display, dm);
            px = dm.heightPixels;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return px;
    }

    /**
     * 检查虚拟导航键是否显示（通用方法）
     *
     * @param context 上下文
     * @return true 显示 false 隐藏
     */
    public static boolean isNavigationBarShow(Context context) {
        return getHasVirtualNavBarScreenHeight(context) - getNoVirtualNavBarScreenHeight(context) > 0;
    }

    /**
     * 获取 OPPO R15 手机导航模式
     * <p>
     * 使用虚拟导航键时为 0 或者 1:(0 表示使用虚拟导航按键且关闭手动隐藏功能，1表示使用虚拟导航按键且打开手动隐藏功能)
     * 使用导航手势时为 2
     */
    private static final String HIDE_NAVIGATIONBAR_ENABLE = "hide_navigationbar_enable";

    public static int oppoHideNavigationBarEnabled(Context context) {
        int val = 0;
        try {
            val = Settings.Secure.getInt(context.getContentResolver(), HIDE_NAVIGATIONBAR_ENABLE, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return val;
    }

    /**
     * 判断虚拟导航键是否隐藏的方法(OPPO 的机器，包括手动隐藏和 导航手势)
     *
     * @param context
     * @return true 隐藏 false 显示
     */
    public static boolean isOPPONavigationBarHide(Context context) {
        boolean hideNav = false;
        try {
            Class systemPropertiesClass = Class.forName("android.os.SystemProperties");
            Method m = systemPropertiesClass.getMethod("get", String.class);
            String hideNavigationBar = (String) m.invoke(systemPropertiesClass, "oppo.hide.navigationbar");
            if ("1".equals(hideNavigationBar)) {
                hideNav = true;
            }
        } catch (Exception e) {
            hideNav = false;
        }
        return hideNav;
    }

    private static String processName = null;
    /**
     * add process name cache
     *
     * @param context
     * @return
     */
    public static String getProcessName(final Context context) {
        if (processName != null) {
            return processName;
        }
        //will not null
        processName = getProcessNameInternal(context);
        return processName;
    }


    private static String getProcessNameInternal(final Context context) {
        int myPid = android.os.Process.myPid();

        if (context == null || myPid <= 0) {
            return "";
        }

        ActivityManager.RunningAppProcessInfo myProcess = null;
        ActivityManager activityManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        if (activityManager != null) {
            try {
                List<ActivityManager.RunningAppProcessInfo> appProcessList = activityManager
                        .getRunningAppProcesses();

                if (appProcessList != null) {
                    for (ActivityManager.RunningAppProcessInfo process : appProcessList) {
                        if (process.pid == myPid) {
                            myProcess = process;
                            break;
                        }
                    }

                    if (myProcess != null) {
                        return myProcess.processName;
                    }
                }
            } catch (Exception e) {
            }
        }

        byte[] b = new byte[128];
        FileInputStream in = null;
        try {
            in = new FileInputStream("/proc/" + myPid + "/cmdline");
            int len = in.read(b);
            if (len > 0) {
                for (int i = 0; i < len; i++) { // lots of '0' in tail , remove them
                    if ((((int) b[i]) & 0xFF) > 128 || b[i] <= 0) {
                        len = i;
                        break;
                    }
                }
                return new String(b, 0, len);
            }

        } catch (Exception e) {
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e) {
            }
        }

        return "";
    }

    /**
     * 检测手机是否坚果R1手机
     *
     * @return true 是坚果R1 false 不是坚果R1
     */
    public static boolean isSmartisanR1() {
        return "trident".equalsIgnoreCase(Build.DEVICE);
    }

    public static boolean isHuaWei() {
        return "huawei".equalsIgnoreCase(Build.MANUFACTURER);
    }
}

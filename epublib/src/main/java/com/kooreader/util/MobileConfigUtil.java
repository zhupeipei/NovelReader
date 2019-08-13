package com.kooreader.util;

import android.text.TextUtils;

import java.lang.reflect.Method;

public class MobileConfigUtil {
    private static final String KEY_RO_BUILD_VERSION_HUAWEI_ROM = "ro.build.version.emui";
    private static final String KEY_RO_BUILD_VERSION_VIVO_ROM = "ro.vivo.os.version";

    private static final String KEY_MIUI_VERSION_CODE = "ro.miui.ui.version.code";
    private static final String KEY_MIUI_VERSION_NAME = "ro.miui.ui.version.name";
    private static final String KEY_MIUI_INTERNAL_STORAGE = "ro.miui.internal.storage";

    private static final String KEY_RO_BUILD_VERSION_OPPOROM = "ro.build.version.opporom";

    private static boolean hasCheckHuawei;
    private static boolean isHuawei;
    private static boolean hasCheckVivo;
    private static boolean isVivo;

    private static boolean hasCheckFlyme = false;
    private static boolean isFlyme = false;

    private static boolean hasCheckXiaoMi = false;
    private static boolean isXiaoMi = false;

    private static boolean sHasCheckOppo = false;
    private static boolean sIsOppoOs = false;
    /**
     * 判断是否是魅族的 FlyMe OS
     *
     * @return FlyMe OS
     */
    public static boolean isFlymeOS() {
        if (hasCheckFlyme) {
            return isFlyme;
        }

        // 获取魅族系统操作版本标识
        String meiZuFlyMeOSFlag = getSystemProperty("ro.build.display.id", "");
        if (TextUtils.isEmpty(meiZuFlyMeOSFlag)) {
            isFlyme = false;
        } else if (meiZuFlyMeOSFlag.contains("Flyme") || meiZuFlyMeOSFlag.toLowerCase().contains("flyme")) {
            isFlyme = true;
        } else {
            isFlyme = false;
        }

        hasCheckFlyme = true;

        return isFlyme;
    }

    public static boolean isMIUI() {
        if(hasCheckXiaoMi) {
            return isXiaoMi;
        }
        try {
            isXiaoMi = TextUtils.isEmpty(getSystemProperty(KEY_MIUI_VERSION_CODE, ""))
                    || TextUtils.isEmpty(getSystemProperty(KEY_MIUI_VERSION_NAME, ""))
                    || TextUtils.isEmpty(getSystemProperty(KEY_MIUI_INTERNAL_STORAGE, ""));
        } catch (Exception e) {
            isXiaoMi = false;
        }
        return isXiaoMi;
    }


    /**
     * 判断是否是 oppo os
     *
     * @return
     */
    public static boolean isOppoOs() {
        if (sHasCheckOppo) {
            return sIsOppoOs;
        }

        String oppoOSFlag = getSystemProperty(KEY_RO_BUILD_VERSION_OPPOROM, "");
        if (TextUtils.isEmpty(oppoOSFlag)) {
            sIsOppoOs = false;
        } else {
            sIsOppoOs = true;
        }

        sHasCheckOppo = true;

        return sIsOppoOs;
    }

    public static boolean isHuawei() {
        if (hasCheckHuawei) {
            return isHuawei;
        }
        String huaweiFlag = getSystemProperty(KEY_RO_BUILD_VERSION_HUAWEI_ROM, "");
        if (TextUtils.isEmpty(huaweiFlag)) {
            isHuawei = false;
        } else {
            isHuawei = true;
        }
        hasCheckHuawei = true;
        return isHuawei;
    }

    public static boolean isVivo() {
        if (hasCheckVivo) {
            return isVivo;
        }
        String huaweiFlag = getSystemProperty(KEY_RO_BUILD_VERSION_VIVO_ROM, "");
        if (TextUtils.isEmpty(huaweiFlag)) {
            isVivo = false;
        } else {
            isVivo = true;
        }
        hasCheckVivo = true;
        return isVivo;
    }

    /**
     * 获取系统属性
     *
     * @param key          ro.build.display.id
     * @param defaultValue 默认值
     * @return 系统操作版本标识
     */
    private static String getSystemProperty(String key, String defaultValue) {
        try {
            Class<?> SystemProperties = Class.forName("android.os.SystemProperties");
            Method m = SystemProperties.getMethod("get", String.class, String.class);
            String result = (String) m.invoke(null, key, defaultValue);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return defaultValue;
    }
}

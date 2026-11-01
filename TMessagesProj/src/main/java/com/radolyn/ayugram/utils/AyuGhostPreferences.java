package com.radolyn.ayugram.utils;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

import java.util.concurrent.ConcurrentHashMap;

import tw.nekomimi.nekogram.NekoConfig;

public class AyuGhostPreferences {
    public static final int TYPE_DEFAULT = 0;
    public static final int TYPE_FORCE_BLOCK = 1;
    public static final int TYPE_FORCE_ALLOW = 2;

    public static final String ghostReadExclusionPrefix = "ghostModeReadExclusion_";
    public static final String ghostTypingExclusionPrefix = "ghostModeTypingExclusion_";
    public static final String ghostReadExceptionPrefix = "ghostModeReadException_";
    public static final String ghostTypingExceptionPrefix = "ghostModeTypingException_";
    private static final String plainReadExceptionPrefix = "read_";
    private static final String plainTypingExceptionPrefix = "typing_";

    private static final String PREFS_FILE = "ayughostexclusionsconfig";
    private static final ConcurrentHashMap<Long, Integer> readExceptions = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Integer> typingExceptions = new ConcurrentHashMap<>();
    private static SharedPreferences preferences;

    private static SharedPreferences getPrefs() {
        if (preferences == null) {
            preferences = ApplicationLoader.applicationContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        }
        return preferences;
    }

    public static void setReadException(long dialogId, int type) {
        long key = Math.abs(dialogId);
        int value = normalizeType(type);
        readExceptions.put(dialogId, value);
        SharedPreferences.Editor editor = getPrefs().edit();
        if (value == TYPE_DEFAULT) {
            editor.remove(ghostReadExceptionPrefix + key);
            editor.remove(plainReadExceptionPrefix + dialogId);
        } else {
            editor.putInt(ghostReadExceptionPrefix + key, value);
            editor.putInt(plainReadExceptionPrefix + dialogId, value);
        }
        editor.remove(ghostReadExclusionPrefix + key);
        editor.apply();
        NekoConfig.getPreferences().edit().remove(ghostReadExclusionPrefix + key).apply();
    }

    public static int getReadException(long dialogId) {
        return readExceptions.computeIfAbsent(dialogId, AyuGhostPreferences::loadReadException);
    }

    public static void setTypingException(long dialogId, int type) {
        long key = Math.abs(dialogId);
        int value = normalizeType(type);
        typingExceptions.put(dialogId, value);
        SharedPreferences.Editor editor = getPrefs().edit();
        if (value == TYPE_DEFAULT) {
            editor.remove(ghostTypingExceptionPrefix + key);
            editor.remove(plainTypingExceptionPrefix + dialogId);
        } else {
            editor.putInt(ghostTypingExceptionPrefix + key, value);
            editor.putInt(plainTypingExceptionPrefix + dialogId, value);
        }
        editor.remove(ghostTypingExclusionPrefix + key);
        editor.apply();
        NekoConfig.getPreferences().edit().remove(ghostTypingExclusionPrefix + key).apply();
    }

    public static int getTypingException(long dialogId) {
        return typingExceptions.computeIfAbsent(dialogId, AyuGhostPreferences::loadTypingException);
    }

    public static void setGhostModeReadExclusion(long chatId, boolean value) {
        setReadException(chatId, value ? TYPE_FORCE_ALLOW : TYPE_DEFAULT);
    }

    public static boolean getGhostModeReadExclusion(long chatId) {
        return getReadException(chatId) == TYPE_FORCE_ALLOW;
    }

    public static void setGhostModeTypingExclusion(long chatId, boolean value) {
        setTypingException(chatId, value ? TYPE_FORCE_ALLOW : TYPE_DEFAULT);
    }

    public static boolean getGhostModeTypingExclusion(long chatId) {
        return getTypingException(chatId) == TYPE_FORCE_ALLOW;
    }

    public static boolean shouldBlockWhenGlobalDisabled(int type) {
        return type != TYPE_FORCE_ALLOW;
    }

    public static boolean shouldBlockWhenGlobalEnabled(int type) {
        return type == TYPE_FORCE_BLOCK;
    }

    /**
     * 读取顺序：按绝对值记录的键 → 按带符号会话 id 记录的键 → 旧版布尔键。
     * 带符号的键让 id 相同的用户与频道各自保留取值。
     */
    private static int loadReadException(long dialogId) {
        SharedPreferences prefs = getPrefs();
        long key = Math.abs(dialogId);
        if (prefs.contains(ghostReadExceptionPrefix + key)) {
            return normalizeType(prefs.getInt(ghostReadExceptionPrefix + key, TYPE_DEFAULT));
        }
        if (prefs.contains(plainReadExceptionPrefix + dialogId)) {
            return normalizeType(prefs.getInt(plainReadExceptionPrefix + dialogId, TYPE_DEFAULT));
        }
        if (prefs.getBoolean(ghostReadExclusionPrefix + key, false)
                || NekoConfig.getPreferences().getBoolean(ghostReadExclusionPrefix + key, false)) {
            prefs.edit().putInt(ghostReadExceptionPrefix + key, TYPE_FORCE_ALLOW)
                    .remove(ghostReadExclusionPrefix + key)
                    .apply();
            NekoConfig.getPreferences().edit().remove(ghostReadExclusionPrefix + key).apply();
            return TYPE_FORCE_ALLOW;
        }
        return TYPE_DEFAULT;
    }

    private static int loadTypingException(long dialogId) {
        SharedPreferences prefs = getPrefs();
        long key = Math.abs(dialogId);
        if (prefs.contains(ghostTypingExceptionPrefix + key)) {
            return normalizeType(prefs.getInt(ghostTypingExceptionPrefix + key, TYPE_DEFAULT));
        }
        if (prefs.contains(plainTypingExceptionPrefix + dialogId)) {
            return normalizeType(prefs.getInt(plainTypingExceptionPrefix + dialogId, TYPE_DEFAULT));
        }
        if (prefs.getBoolean(ghostTypingExclusionPrefix + key, false)
                || NekoConfig.getPreferences().getBoolean(ghostTypingExclusionPrefix + key, false)) {
            prefs.edit().putInt(ghostTypingExceptionPrefix + key, TYPE_FORCE_ALLOW)
                    .remove(ghostTypingExclusionPrefix + key)
                    .apply();
            NekoConfig.getPreferences().edit().remove(ghostTypingExclusionPrefix + key).apply();
            return TYPE_FORCE_ALLOW;
        }
        return TYPE_DEFAULT;
    }

    private static int normalizeType(int type) {
        if (type == TYPE_FORCE_BLOCK || type == TYPE_FORCE_ALLOW) {
            return type;
        }
        return TYPE_DEFAULT;
    }
}

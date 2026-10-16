package com.aquaticstudios.aquabook.utils;

import com.aquaticstudios.aquabook.AquaBook;
import com.aquaticstudios.aquabook.news.AfterAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Settings {
    public static String prefix = "";
    public static String dateFormat = "dd/MM/yy";

    public static String backend = "sqlite";
    public static long syncIntervalSeconds = 60L;

    public static int guiRows = 6;
    public static List<Integer> guiSlots = defaultSlots();
    public static String guiSort = "newest";
    public static boolean glowUnread = true;
    public static String readMaterial = "BOOK";
    public static String openBookSound = "ITEM_BOOK_PAGE_TURN";
    public static String pageSound = "UI_BUTTON_CLICK";

    public static int previousSlot = 48;
    public static int closeSlot = 49;
    public static int nextSlot = 50;
    public static String previousMaterial = "ARROW";
    public static String closeMaterial = "BARRIER";
    public static String nextMaterial = "ARROW";

    public static String bookDefaultColor = "BLACK";
    public static long reopenDelay = 1L;
    public static AfterAction afterAction = AfterAction.CLOSE;
    public static boolean footerEnabled = true;
    public static boolean footerOnEveryPage = false;
    public static boolean allowUnlike = false;
    public static boolean likeMarksRead = true;
    public static boolean menuButtonEnabled = true;
    public static boolean menuButtonOnlyWhenRead = true;

    public static boolean autoOpenEnabled = true;
    public static boolean autoOpenOnlyLatest = true;
    public static long autoOpenDelay = 1L;
    public static int autoOpenMax = 1;
    public static long autoOpenGap = 40L;
    public static boolean joinNotify = true;
    public static long joinDelay = 1L;
    public static boolean joinOnlyIfUnread = true;
    public static String joinClickCommand = "/aquabook menu";
    public static String joinSound = "ENTITY_EXPERIENCE_ORB_PICKUP";

    public static void load() {
        prefix = AquaBook.MESSAGES.getString("prefix", "");

        dateFormat = AquaBook.CONFIG.getString("date-format", "dd/MM/yy");

        backend = AquaBook.CONFIG.getString("storage.type", "SQLITE").toLowerCase();
        syncIntervalSeconds = Math.max(10L, AquaBook.CONFIG.getLong("storage.sync-interval-seconds", 60L));

        guiRows = Math.min(6, Math.max(1, AquaBook.CONFIG.getInt("gui.rows", 6)));
        guiSlots = loadSlots();
        guiSort = AquaBook.CONFIG.getString("gui.sort", "newest");
        glowUnread = AquaBook.CONFIG.getBoolean("gui.glow-unread", true);
        readMaterial = AquaBook.CONFIG.getString("gui.item.read-material", "BOOK");
        openBookSound = AquaBook.CONFIG.getString("gui.sounds.open-book", "ITEM_BOOK_PAGE_TURN");
        pageSound = AquaBook.CONFIG.getString("gui.sounds.page", "UI_BUTTON_CLICK");

        previousSlot = AquaBook.CONFIG.getInt("gui.navigation.previous-slot", 48);
        closeSlot = AquaBook.CONFIG.getInt("gui.navigation.close-slot", 49);
        nextSlot = AquaBook.CONFIG.getInt("gui.navigation.next-slot", 50);
        previousMaterial = AquaBook.CONFIG.getString("gui.navigation.previous-material", "ARROW");
        closeMaterial = AquaBook.CONFIG.getString("gui.navigation.close-material", "BARRIER");
        nextMaterial = AquaBook.CONFIG.getString("gui.navigation.next-material", "ARROW");

        bookDefaultColor = AquaBook.CONFIG.getString("book.default-color", "BLACK");
        reopenDelay = Math.max(1L, AquaBook.CONFIG.getLong("book.reopen-delay", 1L));
        afterAction = AfterAction.fromName(AquaBook.CONFIG.getString("book.after-action", "CLOSE"));
        footerEnabled = AquaBook.CONFIG.getBoolean("book.footer.enabled", true);
        footerOnEveryPage = "all".equalsIgnoreCase(AquaBook.CONFIG.getString("book.footer.page", "last"));
        allowUnlike = AquaBook.CONFIG.getBoolean("book.footer.like.allow-unlike", false);
        likeMarksRead = AquaBook.CONFIG.getBoolean("book.footer.like.marks-read", true);
        menuButtonEnabled = AquaBook.CONFIG.getBoolean("book.footer.menu.enabled", true);
        menuButtonOnlyWhenRead = AquaBook.CONFIG.getBoolean("book.footer.menu.only-when-read", true);

        autoOpenEnabled = AquaBook.CONFIG.getBoolean("join.auto-open.enabled", true);
        autoOpenOnlyLatest = AquaBook.CONFIG.getBoolean("join.auto-open.only-latest", true);
        autoOpenDelay = Math.max(1L, AquaBook.CONFIG.getLong("join.auto-open.delay", 1L));
        autoOpenMax = Math.max(1, AquaBook.CONFIG.getInt("join.auto-open.max", 1));
        autoOpenGap = Math.max(1L, AquaBook.CONFIG.getLong("join.auto-open.gap", 40L));
        joinNotify = AquaBook.CONFIG.getBoolean("join.notify", true);
        joinDelay = Math.max(1L, AquaBook.CONFIG.getLong("join.delay", 1L));
        joinOnlyIfUnread = AquaBook.CONFIG.getBoolean("join.only-if-unread", true);
        joinClickCommand = AquaBook.CONFIG.getString("join.click-command", "/aquabook menu");
        joinSound = AquaBook.CONFIG.getString("join.sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
    }

    private static List<Integer> loadSlots() {
        final List<Integer> out = new ArrayList<>();
        final List<?> raw = AquaBook.CONFIG.getList("gui.slots");

        if (raw != null) {
            for (Object entry : raw) {
                if (entry instanceof Number) out.add(((Number) entry).intValue());
            }
        }
        return out.isEmpty() ? defaultSlots() : Collections.unmodifiableList(out);
    }

    private static List<Integer> defaultSlots() {
        final List<Integer> out = new ArrayList<>(21);
        for (int row = 1; row <= 3; row++) {
            for (int column = 1; column <= 7; column++) {
                out.add(row * 9 + column);
            }
        }
        return Collections.unmodifiableList(out);
    }
}

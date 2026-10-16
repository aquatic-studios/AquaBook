package com.aquaticstudios.aquabook.news;

public enum AfterAction {
    CLOSE,

    MENU,

    REOPEN;

    public static AfterAction fromName(String name) {
        if (name == null) return CLOSE;
        try {
            return valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return CLOSE;
        }
    }
}

package com.aquaticstudios.aquabook.news;

import com.aquaticstudios.aquabook.utils.CC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Map;

public final class NewsComponent {
    private final String text;
    private final String hover;
    private final String command;
    private final String suggest;
    private final String url;
    private final String copy;
    private final Integer page;

    private NewsComponent(String text, String hover, String command, String suggest, String url, String copy, Integer page) {
        this.text = text == null ? "" : text;
        this.hover = hover;
        this.command = command;
        this.suggest = suggest;
        this.url = url;
        this.copy = copy;
        this.page = page;
    }

    public static NewsComponent of(String text) {
        return new NewsComponent(text, null, null, null, null, null, null);
    }

    public static NewsComponent of(String text, String hover, String command) {
        return new NewsComponent(text, hover, command, null, null, null, null);
    }

    @SuppressWarnings("unchecked")
    public static NewsComponent fromObject(Object raw) {
        if (raw == null) return null;
        if (raw instanceof String) return of((String) raw);

        Map<String, Object> map = null;
        if (raw instanceof Map) {
            map = (Map<String, Object>) raw;
        } else if (raw instanceof ConfigurationSection) {
            map = ((ConfigurationSection) raw).getValues(false);
        }
        if (map == null) return of(String.valueOf(raw));

        Integer page = null;
        final Object rawPage = map.get("page");
        if (rawPage instanceof Number) page = ((Number) rawPage).intValue();

        return new NewsComponent(
                string(map.get("text")),
                string(map.get("hover")),
                string(map.get("command")),
                string(map.get("suggest")),
                string(map.get("url")),
                string(map.get("copy")),
                page);
    }

    private static String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    public Component build(Map<String, String> placeholders, Player player) {
        Component component = CC.parse(apply(text, placeholders), player);

        if (hover != null && !hover.isEmpty()) {
            final String parsed = apply(hover, placeholders).replace("\\n", "\n");
            component = component.hoverEvent(HoverEvent.showText(CC.parse(parsed, player)));
        }

        final ClickEvent click = buildClick(placeholders);
        if (click != null) component = component.clickEvent(click);

        return component;
    }

    private ClickEvent buildClick(Map<String, String> placeholders) {
        if (url != null && !url.isEmpty()) {
            return ClickEvent.openUrl(apply(url, placeholders));
        }
        if (command != null && !command.isEmpty()) {
            String value = apply(command, placeholders);
            if (!value.startsWith("/")) value = "/" + value;
            return ClickEvent.runCommand(value);
        }
        if (suggest != null && !suggest.isEmpty()) {
            return ClickEvent.suggestCommand(apply(suggest, placeholders));
        }
        if (copy != null && !copy.isEmpty()) {
            return ClickEvent.copyToClipboard(apply(copy, placeholders));
        }
        if (page != null && page > 0) {
            return ClickEvent.changePage(page);
        }
        return null;
    }

    private static String apply(String input, Map<String, String> placeholders) {
        if (input == null) return "";
        String out = input;
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                out = out.replace(entry.getKey(), entry.getValue());
            }
        }
        return out;
    }
}

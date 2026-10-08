package dev.vipaaxx.xmotd;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MotdParser {

    private static final Pattern GRADIENT = Pattern.compile(
            "\\[gradient=([^\\]]+)\\](.*?)\\[/gradient\\]",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern HEX = Pattern.compile("#[0-9a-fA-F]{6}");

    private static final String COLOR_CODES = "0123456789abcdef";
    private static final String FORMAT_CODES = "klmno";

    private MotdParser() {}

    public static String parse(String input) {
        StringBuilder out = new StringBuilder();
        Set<Character> formats = new LinkedHashSet<>();

        Matcher m = GRADIENT.matcher(input);
        int last = 0;
        while (m.find()) {
            parsePlain(input.substring(last, m.start()), out, formats);
            parseGradient(m.group(1), m.group(2), out, formats);
            last = m.end();
        }
        parsePlain(input.substring(last), out, formats);
        return out.toString();
    }

    /** Handles normal text containing & codes. */
    private static void parsePlain(String text, StringBuilder out, Set<Character> formats) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '&' && i + 1 < text.length()) {
                char n = Character.toLowerCase(text.charAt(i + 1));

                if (n == '#' && i + 8 <= text.length() && HEX.matcher(text.substring(i + 1, i + 8)).matches()) {
                    out.append(hex(Integer.parseInt(text.substring(i + 2, i + 8), 16)));
                    formats.clear();
                    i += 7;
                    continue;
                }
                if (COLOR_CODES.indexOf(n) >= 0) {
                    out.append(ChatColor.COLOR_CHAR).append(n);
                    formats.clear();
                    i++;
                    continue;
                }
                if (FORMAT_CODES.indexOf(n) >= 0) {
                    out.append(ChatColor.COLOR_CHAR).append(n);
                    formats.add(n);
                    i++;
                    continue;
                }
                if (n == 'r') {
                    out.append(ChatColor.COLOR_CHAR).append('r');
                    formats.clear();
                    i++;
                    continue;
                }
            }
            out.append(c);
        }
    }

    /** Handles the inside of a [gradient=...] block. */
    private static void parseGradient(String colorSpec, String text, StringBuilder out, Set<Character> formats) {
        List<Integer> colors = parseColors(colorSpec);

        // Collect visible characters along with the formats active on each one.
        // Color codes inside a gradient are ignored; format codes are kept.
        List<Character> chars = new ArrayList<>();
        List<String> charFormats = new ArrayList<>();
        Set<Character> active = new LinkedHashSet<>(formats);

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '&' && i + 1 < text.length()) {
                char n = Character.toLowerCase(text.charAt(i + 1));
                if (n == '#' && i + 8 <= text.length() && HEX.matcher(text.substring(i + 1, i + 8)).matches()) {
                    i += 7;
                    continue;
                }
                if (COLOR_CODES.indexOf(n) >= 0) {
                    i++;
                    continue;
                }
                if (FORMAT_CODES.indexOf(n) >= 0) {
                    active.add(n);
                    i++;
                    continue;
                }
                if (n == 'r') {
                    active.clear();
                    i++;
                    continue;
                }
            }
            chars.add(c);
            StringBuilder f = new StringBuilder();
            for (char fc : active) f.append(ChatColor.COLOR_CHAR).append(fc);
            charFormats.add(f.toString());
        }

        int n = chars.size();
        for (int i = 0; i < n; i++) {
            double t = n == 1 ? 0 : (double) i / (n - 1);
            out.append(hex(interpolate(colors, t)));
            out.append(charFormats.get(i));
            out.append(chars.get(i));
        }

        // Reset so the gradient's last color doesn't bleed into following text,
        // then re-apply formats that were active before the gradient.
        out.append(ChatColor.COLOR_CHAR).append('r');
        for (char fc : formats) out.append(ChatColor.COLOR_CHAR).append(fc);
    }

    private static List<Integer> parseColors(String spec) {
        List<Integer> colors = new ArrayList<>();
        for (String part : spec.split(",")) {
            part = part.trim();
            if (part.isEmpty()) continue;
            if (!part.startsWith("#")) part = "#" + part;
            if (HEX.matcher(part).matches()) {
                colors.add(Integer.parseInt(part.substring(1), 16));
            }
        }
        if (colors.isEmpty()) colors.add(0xFFFFFF);
        if (colors.size() == 1) colors.add(colors.get(0));
        return colors;
    }

    /** Linear interpolation across any number of color stops. */
    private static int interpolate(List<Integer> colors, double t) {
        double scaled = t * (colors.size() - 1);
        int idx = Math.min((int) Math.floor(scaled), colors.size() - 2);
        double local = scaled - idx;
        int a = colors.get(idx);
        int b = colors.get(idx + 1);
        int r = lerp((a >> 16) & 0xFF, (b >> 16) & 0xFF, local);
        int g = lerp((a >> 8) & 0xFF, (b >> 8) & 0xFF, local);
        int bl = lerp(a & 0xFF, b & 0xFF, local);
        return (r << 16) | (g << 8) | bl;
    }

    private static int lerp(int a, int b, double t) {
        return (int) Math.round(a + (b - a) * t);
    }

    /** Builds the section-sign hex format: §x§R§R§G§G§B§B */
    private static String hex(int rgb) {
        String h = String.format("%06x", rgb & 0xFFFFFF);
        StringBuilder sb = new StringBuilder().append(ChatColor.COLOR_CHAR).append('x');
        for (char c : h.toCharArray()) sb.append(ChatColor.COLOR_CHAR).append(c);
        return sb.toString();
    }
}

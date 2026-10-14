package dev.itolkien.captions.extension;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Ordering policy for the native Auto-translate menu. */
public final class CaptionMenuOrder {
    private CaptionMenuOrder() {}
    public interface Labels<T> {
        String language(T track);
        CharSequence label(T track);
    }
    private static final class Entry<T> {
        final T track;
        final String label;
        Entry(T track, String label) { this.track = track; this.label = label; }
    }
    public static <T> List<T> sorted(List<T> tracks, Locale locale, Labels<? super T> labels) {
        if (tracks == null || tracks.size() < 2) return tracks;
        List<Integer> slots = new ArrayList<>();
        List<Entry<T>> languages = new ArrayList<>();
        for (int i = 0; i < tracks.size(); i++) {
            T track = tracks.get(i);
            if (track == null) continue;
            String code = labels.language(track);
            if (code == null || code.isEmpty() || code.endsWith("_OPTION")) continue;
            CharSequence label = labels.label(track);
            if (label == null || label.toString().trim().isEmpty()) continue;
            slots.add(i);
            languages.add(new Entry<>(track, label.toString().trim()));
        }
        if (languages.size() < 2) return tracks;
        Collator collator = Collator.getInstance(locale);
        collator.setStrength(Collator.SECONDARY);
        collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        // List.sort is stable: equivalent display labels keep their native order.
        languages.sort((a, b) -> collator.compare(a.label, b.label));
        List<T> result = new ArrayList<>(tracks);
        for (int i = 0; i < slots.size(); i++) result.set(slots.get(i), languages.get(i).track);
        return result;
    }
}

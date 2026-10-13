package dev.itolkien.captions;

import java.util.List;
import java.util.Locale;

/** Pure selection policy, independent of YouTube's version-specific player internals. */
public final class CaptionResolver {
    private CaptionResolver() {}

    public enum Kind { MANUAL, ASR }
    public enum Action { DIRECT, TRANSLATE, NONE }
    public static final class Track {
        public final String language;
        public final Kind kind;
        public final boolean originalLanguage;
        public Track(String language, Kind kind, boolean originalLanguage) {
            this.language = normalize(language);
            this.kind = kind;
            this.originalLanguage = originalLanguage;
        }
    }
    public static final class Choice {
        public final Action action;
        public final Track source;
        public Choice(Action action, Track source) { this.action = action; this.source = source; }
    }
    public static String normalize(String raw) {
        if (raw == null) return "";
        String s = raw.trim().replace('_', '-').toLowerCase(Locale.ROOT);
        if (s.equals("iw")) return "he";
        if (s.equals("in")) return "id";
        return s;
    }
    public static boolean matches(String a, String b) {
        String x = normalize(a), y = normalize(b);
        if (x.isEmpty() || y.isEmpty()) return false;
        if (x.equals(y)) return true;
        return x.split("-")[0].equals(y.split("-")[0]);
    }
    public static Choice choose(List<Track> tracks, String preferredLanguage, boolean captionsOff) {
        if (captionsOff || tracks == null || tracks.isEmpty() || normalize(preferredLanguage).isEmpty())
            return new Choice(Action.NONE, null);
        Track best = null;
        int bestRank = Integer.MAX_VALUE;
        for (Track t : tracks) {
            if (t == null || t.language.isEmpty() || t.kind == null) continue;
            boolean exact = matches(t.language, preferredLanguage);
            int rank;
            if (exact) rank = t.kind == Kind.MANUAL ? 0 : 1;
            else if (t.originalLanguage) rank = t.kind == Kind.MANUAL ? 2 : 3;
            else rank = t.kind == Kind.MANUAL ? 4 : 5;
            if (rank < bestRank) { bestRank = rank; best = t; }
        }
        if (best == null) return new Choice(Action.NONE, null);
        return new Choice(matches(best.language, preferredLanguage) ? Action.DIRECT : Action.TRANSLATE, best);
    }
}

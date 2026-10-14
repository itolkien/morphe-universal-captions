package dev.itolkien.captions.extension;

import java.util.*;

public final class CaptionMenuOrderTest {
    private static final class Row {
        final String code, label;
        Row(String code, String label) { this.code = code; this.label = label; }
    }
    private static final CaptionMenuOrder.Labels<Row> LABELS = new CaptionMenuOrder.Labels<Row>() {
        public String language(Row row) { return row.code; }
        public CharSequence label(Row row) { return row.label; }
    };
    private static void check(boolean value, String why) {
        if (!value) throw new AssertionError(why);
    }
    private static void expect(List<Row> rows, String... labels) {
        check(rows.size() == labels.length, "no rows lost or added");
        for (int i = 0; i < labels.length; i++)
            check(Objects.equals(rows.get(i).label, labels[i]), "wrong row at " + i + ": " + rows.get(i).label);
    }
    public static void main(String[] args) {
        // This order reproduces the screenshot: language codes differ from display-name order.
        List<Row> original = Arrays.asList(new Row("ga", "Irish"), new Row("gd", "Scottish Gaelic"),
            new Row("gl", "Galician"), new Row("hy", "Armenian"));
        expect(CaptionMenuOrder.sorted(original, Locale.ENGLISH, LABELS),
            "Armenian", "Galician", "Irish", "Scottish Gaelic");
        expect(original, "Irish", "Scottish Gaelic", "Galician", "Armenian");
        List<Row> turkish = Arrays.asList(new Row("ga", "İrlandaca"), new Row("ja", "Japonca"),
            new Row("hr", "Hırvatça"), new Row("ia", "Interlingua"));
        expect(CaptionMenuOrder.sorted(turkish, Locale.forLanguageTag("tr"), LABELS),
            "Hırvatça", "Interlingua", "İrlandaca", "Japonca");
        Row off = new Row("DISABLE_CAPTIONS_OPTION", "Off");
        Row translate = new Row("AUTO_TRANSLATE_CAPTIONS_OPTION", "Auto-translate");
        List<Row> special = Arrays.asList(off, original.get(0), translate, original.get(3));
        List<Row> sorted = CaptionMenuOrder.sorted(special, Locale.ENGLISH, LABELS);
        check(sorted.get(0) == off && sorted.get(2) == translate, "special entries keep exact slots and identity");
        expect(sorted, "Off", "Armenian", "Auto-translate", "Irish");
        Row nativeTrack = new Row("de", "Zulu native label");
        Row generated = new Row("zu", "Alpha generated label");
        sorted = CaptionMenuOrder.sorted(Arrays.asList(nativeTrack, generated), Locale.ENGLISH, LABELS);
        check(sorted.get(0) == generated && sorted.get(1) == nativeTrack,
            "native and generated rows sort together by actual labels while retaining their track objects");
        Row first = new Row("en", "English"), second = new Row("en-GB", "english");
        sorted = CaptionMenuOrder.sorted(Arrays.asList(first, second), Locale.ENGLISH, LABELS);
        check(sorted.get(0) == first && sorted.get(1) == second, "equal labels preserve original order");
        Row unknown = new Row("", "Choose language"), missingLabel = new Row("en", null);
        sorted = CaptionMenuOrder.sorted(Arrays.asList(unknown, nativeTrack, null, generated, missingLabel), Locale.ENGLISH, LABELS);
        check(sorted.get(0) == unknown && sorted.get(2) == null && sorted.get(4) == missingLabel,
            "unknown, null and unlabelled entries remain in place");
        check(sorted.get(1) == generated && sorted.get(3) == nativeTrack, "language slots alone are reordered");
        check(CaptionMenuOrder.sorted(null, Locale.ENGLISH, LABELS) == null, "null list passthrough");
        check(CaptionMenuOrder.sorted(Collections.emptyList(), Locale.ENGLISH, LABELS).isEmpty(), "empty list");
        System.out.println("CaptionMenuOrder: all tests passed");
    }
}

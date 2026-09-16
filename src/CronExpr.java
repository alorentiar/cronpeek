import java.util.ArrayList;
import java.util.List;

/**
 * Parses a standard 5-field cron expression (minute hour dom month dow)
 * and tells whether a given time matches.
 *
 * Field syntax: *, 5, 1-10, every-N (x/n), 1-30/5, and comma lists.
 * Day-of-week accepts 0-7, where both 0 and 7 mean Sunday.
 *
 * Not supported on purpose: @macros, seconds, and the L/W/# extensions.
 */
public class CronExpr {

    private final boolean[] minutes = new boolean[60];
    private final boolean[] hours = new boolean[24];
    private final boolean[] daysOfMonth = new boolean[32];
    private final boolean[] months = new boolean[13];
    private final boolean[] daysOfWeek = new boolean[7];

    // Cron's dom/dow rule: when both are restricted, either one matching is enough.
    private final boolean domWild, dowWild;

    private final String raw;

    public CronExpr(String expr) {
        this.raw = expr.trim();
        String[] parts = raw.split("\\s+");
        if (parts.length != 5) {
            throw new IllegalArgumentException("expected 5 fields, got " + parts.length + ": " + raw);
        }

        parseField(parts[0], 0, 59, v -> minutes[v] = true, "minute");
        parseField(parts[1], 0, 23, v -> hours[v] = true, "hour");
        parseField(parts[2], 1, 31, v -> daysOfMonth[v] = true, "day of month");
        parseField(parts[3], 1, 12, v -> months[v] = true, "month");
        parseField(parts[4], 0, 7, v -> daysOfWeek[v == 7 ? 0 : v] = true, "day of week");

        this.domWild = parts[2].equals("*");
        this.dowWild = parts[4].equals("*");
    }

    private interface Sink {
        void set(int value);
    }

    private static void parseField(String field, int lo, int hi, Sink sink, String name) {
        for (String part : field.split(",")) {
            if (part.isEmpty()) {
                throw new IllegalArgumentException("empty " + name + " field part in '" + field + "'");
            }
            int step = 1;
            String range = part;
            int slash = part.indexOf('/');
            if (slash >= 0) {
                range = part.substring(0, slash);
                try {
                    step = Integer.parseInt(part.substring(slash + 1));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("bad step in '" + part + "'");
                }
                if (step < 1) {
                    throw new IllegalArgumentException("step must be >= 1 in '" + part + "'");
                }
            }

            int from, to;
            if (range.equals("*")) {
                from = lo;
                to = hi;
            } else {
                int dash = range.indexOf('-');
                if (dash > 0) {
                    from = num(range.substring(0, dash), name);
                    to = num(range.substring(dash + 1), name);
                } else {
                    from = num(range, name);
                    // A bare value with a step means "from that value to the max".
                    to = slash >= 0 ? hi : from;
                }
            }

            if (from > to) {
                throw new IllegalArgumentException("reversed range '" + part + "' in " + name);
            }
            if (from < lo || to > hi) {
                throw new IllegalArgumentException(name + " out of range " + lo + "-" + hi + ": '" + part + "'");
            }
            for (int v = from; v <= to; v += step) {
                sink.set(v);
            }
        }
    }

    private static int num(String s, String name) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("not a number in " + name + " field: '" + s + "'");
        }
    }

    public boolean matches(java.time.LocalDateTime t) {
        if (!minutes[t.getMinute()] || !hours[t.getHour()]) {
            return false;
        }
        if (!months[t.getMonthValue()]) {
            return false;
        }

        boolean domHit = daysOfMonth[t.getDayOfMonth()];
        // java.time: MONDAY = 1 ... SUNDAY = 7, cron wants SUNDAY = 0.
        int cronDow = t.getDayOfWeek().getValue() % 7;
        boolean dowHit = daysOfWeek[cronDow];

        if (domWild && dowWild) {
            return true;
        }
        if (domWild) {
            return dowHit;
        }
        if (dowWild) {
            return domHit;
        }
        return domHit || dowHit;
    }

    public String raw() {
        return raw;
    }

    /** Collects the next count matching times, starting one minute after from. */
    public List<java.time.LocalDateTime> next(java.time.LocalDateTime from, int count) {
        List<java.time.LocalDateTime> out = new ArrayList<>();
        java.time.LocalDateTime t = from.withSecond(0).withNano(0).plusMinutes(1);

        // Eight years always contains a leap day, so "29 Feb" resolves from any
        // start date while a genuinely impossible expression still fails fast.
        java.time.LocalDateTime limit = t.plusYears(8);
        while (out.size() < count && t.isBefore(limit)) {
            if (matches(t)) {
                out.add(t);
            }
            t = t.plusMinutes(1);
        }
        return out;
    }
}

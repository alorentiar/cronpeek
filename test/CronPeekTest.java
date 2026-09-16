import java.time.LocalDateTime;
import java.util.List;

/** Plain checks, run with: java -cp build test.CronPeekTest */
public class CronPeekTest {

    private static int passed = 0;

    public static void main(String[] args) {
        everyMinuteMatches();
        stepMinutes();
        hourRangeWithDayList();
        weekdayOnly();
        sundayAsZeroAndSeven();
        domOrDowWhenBothRestricted();
        leapDayExistsButDoesNotHappenEveryYear();
        impossibleExpressionYieldsNothing();
        minutesOfDayBoundaries();
        badExpressionsAreRejected();

        System.out.println(passed + " checks passed");
    }

    private static void everyMinuteMatches() {
        CronExpr c = new CronExpr("* * * * *");
        assertTrue(c.matches(LocalDateTime.of(2026, 9, 17, 0, 0)), "midnight matches * * * * *");
        assertTrue(c.matches(LocalDateTime.of(2026, 9, 17, 13, 47)), "13:47 matches * * * * *");
    }

    private static void stepMinutes() {
        CronExpr c = new CronExpr("*/15 * * * *");
        assertTrue(c.matches(LocalDateTime.of(2026, 9, 17, 9, 0)), "*/15 hits minute 0");
        assertTrue(c.matches(LocalDateTime.of(2026, 9, 17, 9, 45)), "*/15 hits minute 45");
        assertTrue(!c.matches(LocalDateTime.of(2026, 9, 17, 9, 46)), "*/15 skips minute 46");
    }

    private static void hourRangeWithDayList() {
        CronExpr c = new CronExpr("0 9-17 * * 1,3,5");
        // 2026-09-18 is a Friday.
        assertTrue(c.matches(LocalDateTime.of(2026, 9, 18, 14, 0)),
                "Friday 14:00 is inside 9-17 on dow 1,3,5");
        assertTrue(!c.matches(LocalDateTime.of(2026, 9, 18, 8, 0)), "08:00 is outside 9-17");
        assertTrue(!c.matches(LocalDateTime.of(2026, 9, 17, 14, 0)), "Thursday is not in 1,3,5");
    }

    private static void weekdayOnly() {
        CronExpr c = new CronExpr("0 3 * * 1-5");
        assertTrue(c.matches(LocalDateTime.of(2026, 9, 18, 3, 0)), "Friday 03:00 weekday");
        assertTrue(!c.matches(LocalDateTime.of(2026, 9, 19, 3, 0)), "Saturday is not a weekday");
    }

    private static void sundayAsZeroAndSeven() {
        CronExpr zero = new CronExpr("0 12 * * 0");
        CronExpr seven = new CronExpr("0 12 * * 7");
        LocalDateTime sunday = LocalDateTime.of(2026, 9, 20, 12, 0);

        assertTrue(zero.matches(sunday), "dow 0 is Sunday");
        assertTrue(seven.matches(sunday), "dow 7 is Sunday too");
    }

    private static void domOrDowWhenBothRestricted() {
        // Classic gotcha: "15th OR Friday", not "15th AND Friday".
        CronExpr c = new CronExpr("0 0 15 * 5");
        assertTrue(c.matches(LocalDateTime.of(2026, 9, 15, 0, 0)), "the 15th fires even on a Tuesday");
        assertTrue(c.matches(LocalDateTime.of(2026, 9, 18, 0, 0)), "a Friday fires even when not the 15th");
        assertTrue(!c.matches(LocalDateTime.of(2026, 9, 16, 0, 0)), "Wednesday the 16th does not fire");
    }

    private static void leapDayExistsButDoesNotHappenEveryYear() {
        CronExpr c = new CronExpr("0 0 29 2 *");
        List<LocalDateTime> hits = c.next(LocalDateTime.of(2026, 1, 1, 0, 0), 2);
        assertTrue(hits.size() == 2, "two leap days found, got " + hits.size());
        assertTrue(hits.get(0).equals(LocalDateTime.of(2028, 2, 29, 0, 0)), "first leap day is 2028-02-29");
        assertTrue(hits.get(1).equals(LocalDateTime.of(2032, 2, 29, 0, 0)), "second is 2032-02-29");
    }

    private static void impossibleExpressionYieldsNothing() {
        // February never has a 30th, no matter what the search does.
        CronExpr c = new CronExpr("0 0 30 2 *");
        assertTrue(c.next(LocalDateTime.of(2026, 1, 1, 0, 0), 1).isEmpty(),
                "0 0 30 2 * can never run");
    }

    private static void minutesOfDayBoundaries() {
        CronExpr c = new CronExpr("0 0 * * *");
        assertTrue(c.matches(LocalDateTime.of(2026, 9, 17, 0, 0)), "midnight fires");
        assertTrue(!c.matches(LocalDateTime.of(2026, 9, 17, 0, 1)), "00:01 does not");

        CronExpr last = new CronExpr("59 23 * * *");
        assertTrue(last.matches(LocalDateTime.of(2026, 9, 17, 23, 59)), "23:59 fires");
        assertTrue(!last.matches(LocalDateTime.of(2026, 9, 18, 0, 0)), "the next day does not");
    }

    private static void badExpressionsAreRejected() {
        assertRejects("0 0 * *", "four fields");
        assertRejects("61 * * * *", "minute 61 out of range");
        assertRejects("0 24 * * *", "hour 24 out of range");
        assertRejects("0 0 * 13 *", "month 13 out of range");
        assertRejects("0 0 0 * *", "day of month 0 out of range");
        assertRejects("*/0 * * * *", "step zero");
        assertRejects("10-5 * * * *", "reversed range");
        assertRejects("x * * * *", "not a number");
    }

    private static void assertTrue(boolean cond, String what) {
        if (!cond) {
            throw new AssertionError("failed: " + what);
        }
        passed++;
    }

    private static void assertRejects(String expr, String why) {
        try {
            new CronExpr(expr);
        } catch (IllegalArgumentException e) {
            passed++;
            return;
        }
        throw new AssertionError("should have been rejected (" + why + "): " + expr);
    }
}

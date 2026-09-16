import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public class CronPeek {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("EEE yyyy-MM-dd HH:mm");

    public static void main(String[] args) {
        if (args.length == 0 || args[0].equals("-h") || args[0].equals("--help")) {
            printUsage();
            return;
        }

        // Accepts either a quoted single argument or the 5 fields split by the shell.
        String expr;
        int count = 5;
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime base = null;

        int i = 0;
        if (args.length >= 5 && !args[0].startsWith("-")) {
            StringBuilder sb = new StringBuilder();
            for (int k = 0; k < 5; k++) {
                if (k > 0) sb.append(' ');
                sb.append(args[k]);
            }
            expr = sb.toString();
            i = 5;
        } else {
            expr = args[0];
            i = 1;
        }

        for (; i < args.length; i++) {
            String a = args[i];
            if (a.equals("-n") && i + 1 < args.length) {
                count = Integer.parseInt(args[++i]);
            } else if (a.equals("--from") && i + 1 < args.length) {
                base = LocalDateTime.parse(args[++i] + "T00:00");
            } else if (a.equals("--zone") && i + 1 < args.length) {
                zone = ZoneId.of(args[++i]);
            } else {
                System.err.println("unknown option: " + a);
                System.exit(2);
            }
        }

        CronExpr cron;
        try {
            cron = new CronExpr(expr);
        } catch (IllegalArgumentException e) {
            System.err.println("bad expression: " + e.getMessage());
            System.exit(2);
            return;
        }

        if (count < 1) {
            System.err.println("-n must be at least 1");
            System.exit(2);
        }

        LocalDateTime start = base != null ? base : LocalDateTime.now(zone);
        List<LocalDateTime> hits = cron.next(start, count);

        if (hits.isEmpty()) {
            System.out.println("no run in the next 8 years, this expression never fires");
            return;
        }

        System.out.println("expression : " + cron.raw());
        System.out.println("from       : " + FMT.format(start) + " (" + zone + ")");
        System.out.println();
        for (int k = 0; k < hits.size(); k++) {
            LocalDateTime t = hits.get(k);
            String day = t.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            System.out.printf("%2d. %s %s%n", k + 1, day, t.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        }
    }

    private static void printUsage() {
        System.out.println("usage: cronpeek \"<cron expr>\" [-n count] [--from yyyy-MM-dd] [--zone id]");
        System.out.println();
        System.out.println("  -n count        how many runs to show (default 5)");
        System.out.println("  --from date     start day, useful for checking a specific date");
        System.out.println("  --zone id       zone id, default is the system zone");
        System.out.println();
        System.out.println("examples:");
        System.out.println("  cronpeek \"*/15 * * * *\" -n 3");
        System.out.println("  cronpeek \"0 3 * * 1-5\" --zone Asia/Jakarta");
        System.out.println("  cronpeek \"30 1 1 * *\" --from 2028-01-31");
    }
}

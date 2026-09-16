# cronpeek

Small Java CLI that takes a cron expression and prints when it will run next. I kept
having to squint at `*/15 9-17 * * 1-5` and count on my fingers, so this just answers
it directly.

Handles the usual 5-field syntax: `*`, lists, ranges, steps (`*/10`, `1-30/5`). Day of
week is 0-7 with both 0 and 7 as Sunday. No `@reboot`-style macros, no seconds field,
no `L`/`W`/`#`.

## Build

Needs a JDK 17 or newer, nothing else. No Maven or Gradle, the project is small enough
that `javac` is less trouble.

```sh
./build.sh
```

This compiles into `build/` and runs the checks. The checks are plain assertions in
`test/CronPeekTest.java`, no JUnit jar to download.

## Usage

```sh
java -cp build CronPeek "*/15 * * * *" -n 3
```

```
expression : */15 * * * *
from       : Thu 2026-09-17 01:31 (Asia/Shanghai)

 1. Thu 2026-09-17 01:45
 2. Thu 2026-09-17 02:00
 3. Thu 2026-09-17 02:15
```

Options:

- `-n count` how many runs to list, default 5
- `--from yyyy-MM-dd` start from a specific day instead of today
- `--zone id` zone id, defaults to whatever the machine is set to

The expression can be one quoted argument or five separate ones, whichever is less
annoying in your shell.

A couple more:

```sh
java -cp build CronPeek "0 3 * * 1-5" --zone Asia/Jakarta
java -cp build CronPeek "30 1 1 * *" --from 2028-01-31
java -cp build CronPeek "0 0 29 2 *"
```

If the expression can never fire (`0 0 30 2 *` is the classic one) it says so instead
of printing an empty list. Bad input exits with code 2.

## Notes

One thing worth knowing if you read the parser: when both day-of-month and day-of-week
are restricted, cron treats them as OR, not AND. So `0 0 15 * 5` runs on the 15th and on
every Friday. That is real cron behaviour and it surprises people, but matching it is the
whole point of the tool.

## License

MIT

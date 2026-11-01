# Daily posting time

Server managers can choose when and how often the bot posts problems:

```text
/challenge automate id:1 channel:#daily-challenges time:00:00 frequency:7 difficulty:Easy
```

`00:00` is 12 AM (midnight). Use `12:00` for noon or `18:30` for 6:30 PM.
The optional `time` uses 24-hour `HH:mm` format and the challenge's timezone,
which is set when creating the challenge and defaults to `Africa/Casablanca`.
Difficulty remains optional and defaults to Easy.

The optional `frequency` sets how many days per week problems are posted (from 1 to 7, defaulting to 7):
- `1` day/week: Monday
- `2` days/week: Tuesday, Thursday
- `3` days/week: Monday, Wednesday, Friday
- `4` days/week: Monday, Tuesday, Thursday, Friday
- `5` days/week: Monday to Friday (weekdays)
- `6` days/week: Monday to Saturday
- `7` days/week: Every day (default)

Omitting `time` selects `00:00`. Omitting `frequency` selects daily (7 days/week).
Existing automations also default to midnight and 7 days/week after upgrading.
Repeat `/challenge automate` with the channel, new time, frequency, or difficulty to
change the schedule. `/challenge status id:1` shows the saved posting time, frequency, and timezone.

The bot checks for due assignments about every minute by default. Posting happens
on the first check at or after the selected time, so it is not guaranteed to occur
at the exact second. If enabled or restarted after that time, it catches up with
today's problem. It does not backfill older days or repost an announcement already
attempted for today. Changing the time does not create a second problem that day.

When a due assignment has no unused free problem at its selected difficulty,
the bot browses LeetCode's catalog and imports a new problem automatically, then
retries the assignment. No manual problem loading is required. Catalog requests
run outside assignment transactions, and the retry rechecks the current schedule
and automation settings before posting.

Each fetch attempt scans at most three pages of 50 entries and makes at most three
metadata requests. Scans continue on later checks when necessary. Upstream or
import failures wait five minutes before retrying. If the catalog contains no new
free problems at that difficulty, it checks again after six hours. Imported
problems persist across restarts; the catalog cursor restarts from the beginning.
These retries may delay a post, and the bot still catches up only for the current
challenge day. It never changes the requested difficulty or repeats a problem
within a challenge to fill a gap.

The schedule follows local time when timezone offsets change. If daylight saving
skips a selected time, that day's posting time shifts forward by the gap; if the
time occurs twice, the first occurrence is used. Each day still has one assignment.

The deadline remains the next midnight in the challenge timezone, including when
posting later in the day. Submission checks still run about every five minutes.
`/challenge stop-auto id:1` disables daily posting and automatic submission checks.

Rebuild and restart the deployed bot to apply migration V12 and register the new
Discord options (difficulty on create, and frequency on automate):

```bash
docker compose up -d --build bot
```

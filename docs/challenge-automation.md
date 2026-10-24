# Daily posting time

Server managers can choose when the bot posts each day's problem:

```text
/challenge automate id:1 channel:#daily-challenges time:00:00
```

`00:00` is 12 AM (midnight). Use `12:00` for noon or `18:30` for 6:30 PM.
The optional `time` uses 24-hour `HH:mm` format and the challenge's timezone,
which is set when creating the challenge and defaults to `Africa/Casablanca`.
Difficulty remains optional and defaults to Easy.

Omitting `time` selects `00:00`. Existing automations also default to midnight
after upgrading. Repeat `/challenge automate` with the channel and new time to
change the schedule. Include the desired difficulty when reconfiguring.
`/challenge status id:1` shows the saved posting time and timezone.

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

Rebuild and restart the deployed bot to apply migration V10 and register the new
Discord option:

```bash
docker compose up -d --build bot
```

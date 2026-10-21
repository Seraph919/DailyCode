# DailyCode

Practice LeetCode with your Discord server. DailyCode posts daily coding problems,
checks your accepted submissions, and keeps track of your challenge progress.

Use the commands below in a server where the bot is available. Replace `1` with
your challenge ID and choose your own username and channel.

## Connect your LeetCode account

1. Run `/leetcode connect username:YourLeetCodeUsername`.
2. Copy the verification token from the bot's reply into your LeetCode profile's
   **ReadMe**, on its own line, and save it.
3. Run `/leetcode verify` in Discord.

After verification, you can remove the token from your profile. If it expires,
run `/leetcode connect` again with the same username and follow the new reply.

## Join a challenge

Ask your server manager for the challenge ID, then join:

```text
/challenge join id:1
```

You need a verified LeetCode account to join. To see today's problem and deadline:

```text
/challenge today id:1
```

Open the problem link and submit your solution on LeetCode using your linked
account. Aim for an **Accepted** result before the displayed deadline.

## Check your completion

With automation enabled, the bot checks submissions about every five minutes.
You can also ask it to check now:

```text
/challenge check id:1
```

If you just submitted and the bot hasn't found it yet, wait a little and try
again. The bot checks recent accepted submissions, so it may not find older work.

To check a particular challenge day, add its date:

```text
/challenge check id:1 date:2026-10-21
```

## View your progress

```text
/challenge status id:1
```

You'll see your recorded completions, today's status, the automation schedule,
and your challenge history. To see older entries:

```text
/challenge status id:1 page:2
```

Status shows your saved progress. Use `/challenge check` to check for a new
completion. “No completion recorded” means the bot hasn't recorded one yet.

## Set up a challenge for your server

You need **Manage Server** permission to create a challenge or change automation.
Verify your LeetCode account before creating one.

### Create a challenge

```text
/challenge create name:DailyPractice end_date:2026-12-31
```

Choose a last day that is today or later. The challenge starts today unless you
provide `start_date`. Dates use `YYYY-MM-DD`, and the last day counts too.

You can also choose a timezone with `timezone`, such as `Europe/London`.
The default is `Africa/Casablanca`.

Share the challenge ID from the bot's reply so members can join. Use
`/challenge join` yourself if you want to take part.

### Enable daily problems

Choose the text channel where you want the bot to post:

```text
/challenge automate id:1 channel:#daily-challenges
```

The bot posts Easy problems at midnight by default and checks participants'
submissions. Make sure it can view the channel and send messages there.

You don't need to load problems yourself. When it runs out of available problems,
the bot fetches more from LeetCode at your chosen difficulty. It picks free
problems and avoids repeating a problem within the same challenge. If LeetCode
is unavailable, it retries later, so a daily post may be delayed.

Both `difficulty` and `time` are optional. Choose Easy, Medium, or Hard for the
difficulty. To set a daily posting time:

```text
/challenge automate id:1 channel:#daily-challenges time:00:00
```

Use 24-hour time: `00:00` is **12 AM (midnight)**, `12:00` is noon, and `18:30`
is 6:30 PM. Times follow the challenge's timezone. Leaving out `time` selects
midnight.

Allow about a minute after the chosen time for the post to appear. If you enable
automation after today's posting time, the bot posts today's problem unless it
has already announced it. The deadline remains the next midnight, even if you
choose a later posting time.

Repeat `/challenge automate` to change the settings, including the difficulty
and time you want to use. Changing the schedule won't post a second problem for
the same day.

### Stop automation

```text
/challenge stop-auto id:1
```

This stops daily posts and automatic submission checks for that challenge.
You can still view your saved progress and use `/challenge check`.

## Check whether the bot responds

Run `/ping` in your server.

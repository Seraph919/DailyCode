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
account. Aim for an **Accepted** result before the displayed deadline. You can
also solve an older problem later; the bot records it as **Completed (late)**.
Solutions must be submitted after the problem was assigned and after you joined.

`/challenge today` replies privately with a problem card and Join Challenge,
View Status, and LeetCode Problem buttons. It reads the current assignment;
it does not create a problem or change the posting schedule.

Each day ends at midnight in the challenge's timezone. If you ask after midnight
but before the next scheduled post, yesterday's problem stays in your history and today's
problem may not be assigned yet. The waiting card shows the challenge date,
timezone, and scheduled posting time, or explains that automation is disabled.

## Check your completion

With automation enabled, the bot checks all assigned problems in your challenge
about every five minutes, including earlier days. You can also check now:

```text
/challenge check id:1
```

If you just submitted and the bot hasn't found it yet, wait a little and try
again. The bot compares your latest 20 accepted LeetCode submissions against all
assigned problems through today. It keeps recorded completions, but cannot recover
an older submission that is no longer in that feed.
The private result card shows the number of problems checked, new completions,
already recorded completions, and problems with no recent match. Use its
View Status button to see your progress.

To check a particular challenge day, add its date:

```text
/challenge check id:1 date:2026-10-21
```

## View your progress

```text
/challenge status id:1
```

The status card shows your recorded completions, today's status, the automation
schedule, and five history entries per page. Late completions count toward your
total and leaderboard score. To see older entries:

```text
/challenge status id:1 page:2
```

Status shows your saved progress. Use `/challenge check` to check for a new
completion. “No completion recorded” means the bot hasn't recorded one yet.

## View the leaderboard

```text
/challenge leaderboard id:1
/challenge leaderboard id:1 page:2
```

Anyone in the challenge's server can view it, even before joining. The reply is
visible only to you and does not ping participants. Each page shows up to ten
participants, ordered by recorded completed days. Equal scores share a rank
(for example, 1, 1, 3); participants with zero completions are included.

This uses saved progress, without fetching new LeetCode submissions. Use
`/challenge check` to update your own completion first if needed.

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

Daily announcements use a colored Discord embed with the problem title, difficulty,
participant count at posting, verification mode, goal, and deadline. **Join Challenge**
and **View Status** buttons respond privately; **LeetCode Problem** opens the problem.
Joining still requires a verified LeetCode account. Status shows saved progress for
this challenge today, including when clicked on an older announcement. The catalog
does not currently store problem descriptions or tags.

Give the bot **Embed Links** permission in the announcement channel, in addition to
**View Channel** and **Send Messages**. This also applies to existing installations.
New posts use cards after redeployment; previously posted messages stay unchanged.

Allow about a minute after the chosen time for the post to appear. If you enable
automation after today's posting time, the bot posts today's problem unless it
has already announced it. The deadline remains the next midnight, even if you
choose a later posting time.

Repeat `/challenge automate` to change the settings, including the difficulty
and time you want to use. Changing the schedule won't post a second problem for
the same day.

To mention a role's members with each daily problem, add the optional `role`:

```text
/challenge automate id:1 channel:#daily-challenges role:@Coders
```

The role must be mentionable, or the bot must have **Mention Everyone** permission
in the posting channel. Choose a server role other than `@everyone`. Members'
Discord notification settings still apply.

Leaving out `role` sends posts without a role mention. To remove an existing role
mention, run `/challenge automate` again without `role`, including the difficulty
and time you want to keep.

### Stop automation

```text
/challenge stop-auto id:1
```

This stops daily posts and automatic submission checks for that challenge.
You can still view your saved progress and use `/challenge check`.

## Check whether the bot responds

Run `/ping` in your server.

## Running the bot in multiple servers

One running DailyCode instance supports every server where it is installed.
Commands register globally on startup and are available in server channels.
Each server has its own challenges, participants, announcement channels, and
optional role mentions. Your verified LeetCode account follows your Discord user
across servers; you join each challenge separately.

For installation, enable **Guild Install** in the Discord Developer Portal and
configure the `bot` and `applications.commands` scopes. Give the bot permission to
view and send messages and embed links in the chosen announcement channels. Server managers can
then install it using the application's installation link.

`DISCORD_TEST_GUILD_ID` is optional. Leave it blank in production. If set, the bot
also mirrors commands into that development server for immediate updates; it
still registers global commands and accepts commands from other servers. Existing
installations can keep their current value during migration so their older
guild-specific commands get refreshed too.

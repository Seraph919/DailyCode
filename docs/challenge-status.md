# Challenge status

Use `/challenge status id:1` to view your saved progress in a challenge you have
joined. The response is private and includes:

- Total recorded completions.
- Today's status in the challenge's timezone.
- Whether automation is enabled.
- Five assigned days per page, newest first, with problem titles and completion status.

Use `/challenge status id:1 page:2` to see older assignments. Page defaults to 1.
History remains accessible after the challenge ends or is cancelled.

“No completion recorded” means the database has no verified completion; it does
not prove that you missed the problem. Days whose deadline preceded your joining
are labelled “Before you joined.” Dates without assignments are not history rows.

This command does not fetch submissions or change progress. Use
`/challenge check id:1` to check today's submissions, or include `date:YYYY-MM-DD`
for an earlier assignment.

After bringing this branch's changes onto OCI, rebuild and restart the bot:

```bash
docker compose up -d --build bot
```

The bot registers the new subcommand when it connects to Discord. No database
migration is required.

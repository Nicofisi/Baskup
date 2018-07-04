`directory-with-backups` can be either the full path or relative to the root directory of the server

`backup-whitelist` should be `[]` if you want to use `backup-blacklist` instead or don't use any filters at all.
if you do want to use the backup whitelist, this part of the config should like this:
```
backup-whitelist:
  - back-me-up.sk
  - pls-do-it.sk
```

`backup-blacklist` should be `[]` if you want to use `backup-whitelist` instead or don't use any filters at all.
if you do want to use the backup blacklist, this part of the config should like this:
```
backup-blacklist:
  - dont-back-me-up.sk
  - pls-dont-do-it.sk
```

`delete-backups-older-than-days` should be set to `0` if you want to disable the feature, or else
to some number, like `30`.

`config-version` should stay set to `$CurrentConfigVersion`. Please don't touch this

If you need help, join https://discord.gg/0l3WlzBPKX7WNjkf and talk to Nicofisi#4467

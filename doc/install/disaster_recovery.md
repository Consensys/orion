# Disaster Recovery Strategies

## Orion Database

Orion stores all payload information on its internal database. This database is stored on the path defined by the 
`workdir` configuration combined with the path information provided in the `storage` option. If you have a config file 
with the following entries for `workdir` and `storage`:
```
workdir = /foo
storage = leveldb:data
```

You will have a level db database stored in `/foo/data`

If the database is deleted or corrupted, the node will lose all the payloads stored in its local database. It is not 
possible to recover a lost database without a backup.

When using Orion in production, it is recommended that you have a [backup strategy](#backup-strategy:-scheduled-backup) 
and a [data redundancy setup](#data-redundancy:-raid-1-(disk-mirroring)) for the filesystem containing the database.

## Backup Strategy: Scheduled Backup

This is an example of backup strategy that will create copies of Orion's database over time. This strategy was based 
on [this Medium post](https://medium.com/@fotios.floros/linux-backup-script-1722cc9c2bf6).

This backup strategy is made of two pieces:

1. A backup bash script that will save a .tar.gz file with the contents of Orion's database
2. A contrab entry that will run the script repeatedly (e.g. every 5 minutes)

Below there is an example of a backup script:

```
#!/bin/bash

TIME=`date +'%Y-%m-%d_%H-%M-%S'`
FILENAME=orion-backup-$TIME.tar.gz
SRC=db/
DEST=db_backups/

tar -cpzf $DEST/$FILENAME $SRC
```

The script is backing up the folder db/ and will create backups into db_backup/ folder. Each backup will be named with 
a timestamp of the time it was executed.

Example of the backup folder:

```
db_backup
|--- orion-backup-2018-03-13_22-45-01.tar.gz
|--- orion-backup-2018-03-13_22-50-01.tar.gz
|--- orion-backup-2018-03-13_22-55-01.tar.gz
```

Now that you have the script, create a entry on the server crontab to execute this script every 5 minutes.

Add the following entry into the server's crontab (`crontab -e`):

```
*/5 * * * * /bin/bash /path/to/backup_script.sh
```

After that, every 5 minutes the system will run the backup script that will create a snapshot of the database. In case 
of a disaster, a node would only lose the payload in the minutes after the last backup.

## Data Redundancy: RAID 1 (disk mirroring)

Using RAID 1 is a easy way to ensure that you have redundancy in your data. Basically, you will have a **copy of your 
data** written to two or more disks. This copy can be used as a backup source.

Another good reason for using RAID 1 is that it provides **instant failover** should one of the disks fail. This can 
avoid a node losing data because of disk failure.

If you want to read more about RAID 1 and want to learn how to setup it in your server, check out 
[this tutorial](https://www.tecmint.com/create-raid1-in-linux/).
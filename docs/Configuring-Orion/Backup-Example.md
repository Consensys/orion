description: Disaster recovery strategies 
<!--- END of page meta data -->

# Example Disaster Recovery Strategies

!!! danger 
    The following are examples of backup and data redundancy approaches. They are not complete solutions. 
    
    If the database is deleted or corrupted, all payloads for the node are lost. You cannot recover a lost database without a backup.

## Backup Strategy: Scheduled Backup Example

This example backup strategy creates copies of the Orion database over time. The example is based 
on [this Medium post](https://medium.com/@fotios.floros/linux-backup-script-1722cc9c2bf6).

The backup strategy consists of:

1. Backup bash script that saves a `.tar.gz` file containing the Orion database contents
1. `crontab` entry that runs the script repeatedly (for example, every 5 minutes)

### 1. Bash Script

```bash tab="Example Backup Scipt"

#!/bin/bash

TIME=`date +'%Y-%m-%d_%H-%M-%S'`
FILENAME=orion-backup-$TIME.tar.gz
SRC=db/
DEST=db_backups/

tar -cpzf $DEST/$FILENAME $SRC
```

The script is backing up the `db/` directory and creates backups into the `db_backup/` directory. Each backup is named with 
a timestamp of the execution time.

```bash tab="Example Backups"
db_backup
|--- orion-backup-2018-03-13_22-45-01.tar.gz
|--- orion-backup-2018-03-13_22-50-01.tar.gz
|--- orion-backup-2018-03-13_22-55-01.tar.gz
```

### 2. crontab Entry

Create an entry on the server crontab (`crontab -e`) to execute the backup script every 5 minutes:

```
*/5 * * * * /bin/bash /path/to/backup_script.sh
```

The backup script is run every 5 minutes to create a snapshot of the database. In a disaster, the node 
only loses the payloads from the minutes after the last backup.

## Data Redundancy: RAID 1 (Disk Mirroring)

Using [RAID 1](https://www.tecmint.com/create-raid1-in-linux/) ensures you have data redundancy. A copy 
of the data is written to two or more disks. The copy can be used as a backup source.

RAID 1 provides instant failover if a disk fails. Having instant failover can avoid a node losing data because of disk failure.
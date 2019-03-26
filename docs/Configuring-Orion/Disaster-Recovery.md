description: Disaster recovery strategies 
<!--- END of page meta data -->

# Disaster Recovery

!!! danger
    If the database is deleted or corrupted, all payloads for the node are lost. You cannot recover a lost database without a backup.

Orion stores all payload information in an internal database. The database location is specified by the 
`workdir` and `storage` properties in the [configuration file](Configuration-File.md).

If the Orion configuration file has the following entries:
```
workdir = /orionNode
storage = leveldb:data
```

The level db database is located in `/orionNode/data`

When using Orion in production, we recommend that you have a backup strategy and a data redundancy setup.


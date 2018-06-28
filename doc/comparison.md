# Feature comparison with Constellation

This is the list of features implemented by [Constellation](https://github.com/jpmorganchase/constellation) 
and the flag if they have been implemented on Orion.

### API
| Operation | Status |
|---|---|
| Send | Implemented |
| Send Raw | Implemented |
| Receive | Implemented |
| Receive Raw | Implemented |
| Push | Implemented |
| PartyInfo | Implemented |
| Upcheck | Implemented |
| Delete | Pending |
| Resend | Pending |

**Note about Constellation's private IPC api:** the current version of Orion does not support the IPC
 API. Orion only support HTTP API. Different from Constellation in Orion one can call all exposed 
 methods through the HTTP API.
 
 ### Configuration
| Config | Status |
|---|---|
| url | Implemented |
| port | Implemented |
| workdir | Implemented |
| othernodes | Implemented |
| publickeys | Implemented |
| privatekeys | Implemented |
| passwords | Implemented |
| storage | Implemented |
| socket | Pending |
| alwayssendto | Pending |
| verbosity | Pending |
| ipwhitelist | Pending |
| tsl | Pending |
| tslservercert | Pending |
| tslserverchain | Pending |
| tslserverkey | Pending |
| tslservertrust | Pending |
| tslknownclients | Pending |
| tslclientcert | Pending |
| tslclientchain | Pending |
| tslclientkey | Pending |
| tslclienttrust | Pending |
| tslknownservers | Pending |
 
 You can check all the available properties in the [`sample.conf`](https://github.com/ConsenSys/orion/blob/master/src/main/resources/sample.conf) 
 file.
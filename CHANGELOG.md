# Changelog

## Project Deprecation
Now that all primary Orion functionality has been merged into Tessera, Orion is being deprecated, 
and we encourage all users with active projects to use the provided migration instructions, 
documented [here](https://docs.orion.consensys.net/en/latest/Tutorials/Migrating-from-Orion-to-Tessera/). 

We will continue to support Orion users until 30th November 2020. If you have any questions or 
concerns, please reach out to the ConsenSys protocol engineering team in the 
[#orion channel on Discord](https://discord.gg/hYpHRjK) or by [email](mailto:quorum@consensys.net).

## 1.6

### Additions and Improvements

- The methods `/sendraw` and `/receiveraw` are now deprecated. We will remove them in the next minor
version release (expected to be 1.7). 
- Moving to security best practices for docker where we now have a user:group `orion:orion` and will
no longer use `root` as the container user.  
- Binary downloads have transitioned from Bintray to Cloudsmith.  Please ensure you use links in the 
  documentation or release notes.
  Ansible users should ensure they have the latest version of the ansible role.

## Breaking changes

When upgrading to 1.6, ensure you've taken into account the following breaking changes.

### Compatibility with Hyperledger Besu

When upgrading Hyperledger Besu to v1.5, it is required that Orion is upgraded to 
v1.6. Older versions of Orion will no longer work with Besu v1.5.  

### Docker users with volume mounts

To maintain best security practices, we're changing the `user:group` on the Docker container to `orion`.

What this means for you:

* If you are running Orion as a binary, there is no impact.
* If you are running Orion as a Docker container *and* have a volume mount for data,  ensure that the 
permissions on the directory allow other users and groups to r/w. Ideally this should be set to
`orion:orion` as the owner.

Note that the `orion` user only exists within the container not outside it. The same user ID may match
a different user outside the image.

If you’re mounting local folders, it is best to set the user via the Docker `—user` argument. Use the
UID because the username may not exist inside the docker container. Ensure the directory being mounted
is owned by that user.

## 1.5.2 

### Bug fixes 

- Filter the list of URIs when [creating a privacy group](https://github.com/PegaSysEng/orion/pull/369).

Resolves `NodePropagatingToAllPeers` error that was occurring when creating privacy groups with a 
PostgreSQL database.  

## 1.5.1 

### Additions and Improvements 

- Added option to move Orion peer table to persistent storage to support [high availability configurations](https://docs.orion.consensys.net/en/latest/HowTo/High-Availability/) [\#332](https://github.com/PegaSysEng/orion/pull/332)
- Added [environment variables](https://docs.orion.consensys.net/en/latest/Reference/Configuration-File/) to support Orion deployment [\#332](https://github.com/PegaSysEng/orion/pull/332)

## 1.5 

### Additions and Improvements 

* [TLS support to secure Orion to Orion, and Orion to Hyperledger Besu communication](https://docs.orion.consensys.net/en/latest/Concepts/TLS-Communication/). 

### Known Bugs 

- Using `alwayssendto` makes privacy group ID calculation inconsistent [\#OR-357](https://pegasys1.atlassian.net/browse/OR-357)

Workaround - Do not use `alwayssendto`.

## 1.5 RC 

### Additions and Improvements 

- Add endpoint to retrieve the privacy group details and members for a given privacyGroupId [\#307](https://github.com/PegaSysEng/orion/pull/307)
- Client connection endpoint can be configured to accept TLS connections [\#319](https://github.com/PegaSysEng/orion/pull/319)
- Generate docker image of Orion [\#304](https://github.com/PegaSysEng/orion/pull/304)

### Known Bugs 

- Using `alwayssendto` makes privacy group ID calculation inconsistent [\#OR-357](https://pegasys1.atlassian.net/browse/OR-357)

Workaround - Do not use `alwayssendto`.

## 1.4

### Java 11

- From v1.4, Orion requires Java 11. Orion on Java 8 is no longer supported.  

### Additions and Improvements 

- Added Oracle DB support [\#284](https://github.com/PegaSysEng/orion/pull/284) 
- Added PostgreSQL DB support [\#276](https://github.com/PegaSysEng/orion/pull/276)
- Documentation updates include: 
  - Added content on configuring [Oracle](https://docs.orion.consensys.net/en/latest/Configuring-Orion/Using-Oracle/) and [PostgreSQL](https://docs.orion.consensys.net/en/latest/Configuring-Orion/Using-PostgreSQL/) databases
  - Added content on generating [certificates using OpenSSL](https://docs.orion.consensys.net/en/latest/Configuring-Orion/TLS/#generating-certificates-using-openssl)

### Technical Improvements 

- Add license report to distribution packages [\#293](https://github.com/PegaSysEng/orion/pull/293) 
- Include database ddl files in orion distribution [\#292](https://github.com/PegaSysEng/orion/pull/292) 
- Include Oracle JDBC driver in package [\#291](https://github.com/PegaSysEng/orion/pull/291) 
- Default to empty name and description for new privacy groups [\#289](https://github.com/PegaSysEng/orion/pull/289) 
- Random prefix storage fix [\#288](https://github.com/PegaSysEng/orion/pull/288)
- Make everything final [\#285](https://github.com/PegaSysEng/orion/pull/285) 
- Fix spotless problems in AT/OrionFactory [\#280](https://github.com/PegaSysEng/orion/pull/280) 
- Create acceptanceTests for multi-key per node usage [\#279](https://github.com/PegaSysEng/orion/pull/279) 
- Fix PermTrustOption setup [\#278](https://github.com/PegaSysEng/orion/pull/278) 

## 1.3.2 

Stability improvements and bug fixes

## 1.3.1 

### Additions and Improvements 

- Documentation updates include: 
  - Added content on [privacy groups](https://docs.orion.consensys.net/en/latest/Using-Orion/Privacy-Groups/)
  - Added [TLS content](https://docs.orion.consensys.net/en/latest/Configuring-Orion/TLS/)
  - Updated [Client API](https://docs.orion.consensys.net/en/latest/Reference/API-Methods/) reference to include privacy group methods
  
### Technical Improvements 

- Check if legacy group already exists on send [\#269](https://github.com/PegaSysEng/orion/pull/269)
- Updating CircleCI jobs to Java 11 [\#267](https://github.com/PegaSysEng/orion/pull/267) 
- Null checks name and description of privacy group, and associated test [\#265](https://github.com/PegaSysEng/orion/pull/265) (thanks to [josh-richardson](https://github.com/josh-richardson))
- Always return empty list when privacy group id doesn't exist [\#264](https://github.com/PegaSysEng/orion/pull/264) 
- Adding error msg on /receive [\#263](https://github.com/PegaSysEng/orion/pull/263) 
- Avoid Orion node calling itself for discovery through listening interface [\#261](https://github.com/PegaSysEng/orion/pull/261) 
- Upgrade Jackson [\#258](https://github.com/PegaSysEng/orion/pull/258) 
- Change CreatePrivacyGroup and FindPrivacyGroup to return the same type [\#257](https://github.com/PegaSysEng/orion/pull/257) (thanks to [josh-richardson](https://github.com/josh-richardson))
- Change generatePrivacyGroupId to generate hash based on random seed [\#256](https://github.com/PegaSysEng/orion/pull/256) (thanks to [josh-richardson](https://github.com/josh-richardson))

## 1.2 

- Rename privacyGroupId API to createPrivacyGroupId [\#251](https://github.com/PegaSysEng/orion/pull/251) (thanks to [Puneetha17](https://github.com/Puneetha17))
- Improve error message when privacy group not found in /send [\#249](https://github.com/PegaSysEng/orion/pull/249) (thanks to [Puneetha17](https://github.com/Puneetha17))
- Add findPrivacyGroup endpoint [\#247](https://github.com/PegaSysEng/orion/pull/247) (thanks to [Puneetha17](https://github.com/Puneetha17))

## 1.1 

- Rethrow the Exception in SendHandler [\#244](https://github.com/PegaSysEng/orion/pull/244) (thanks to [Puneetha17](https://github.com/Puneetha17))
- Rethrow the Exception in SendHandler after getting privacy group. [\#243](https://github.com/PegaSysEng/orion/pull/243) (thanks to [Puneetha17](https://github.com/Puneetha17))
- Publish source and javadoc to bintray [\#242](https://github.com/PegaSysEng/orion/pull/242) 
- Upgrade bintray plugin [\#241](https://github.com/PegaSysEng/orion/pull/241) 
- Support building on JDK 11 and 12 [\#240](https://github.com/PegaSysEng/orion/pull/240) 
- Keep the generation of privacy group consistent. [\#239](https://github.com/PegaSysEng/orion/pull/239) (thanks to [Puneetha17](https://github.com/Puneetha17))
- Propagate the delete privacy group changes to all peers [\#238](https://github.com/PegaSysEng/orion/pull/238) (thanks to [Puneetha17](https://github.com/Puneetha17))
- Push the created privacy group to all the recipients [\#237](https://github.com/PegaSysEng/orion/pull/237) (thanks to [Puneetha17](https://github.com/Puneetha17))
- Add delete group API in Orion [\#236](https://github.com/PegaSysEng/orion/pull/236) (thanks to [Puneetha17](https://github.com/Puneetha17))
- Enable send API to accept privacy group Id [\#235](https://github.com/PegaSysEng/orion/pull/235) (thanks to [Puneetha17](https://github.com/Puneetha17))
- Updating Vertx and Jackson dependencies [\#233](https://github.com/PegaSysEng/orion/pull/233) 
- Add endpoint to retrieve privacyGroupId from the given list of addresses [\#227](https://github.com/PegaSysEng/orion/pull/227) (thanks to [Puneetha17](https://github.com/Puneetha17))



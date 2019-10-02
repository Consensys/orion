# Changelog

## Java 11

From v1.4, Orion requires Java 11. Orion on Java 8 is no longer supported. 

## 1.4 

### Additions and Improvements 

- Added Oracle DB support [\#284](https://github.com/PegaSysEng/orion/pull/284) 
- Added PostgreSQL DB support [\#276](https://github.com/PegaSysEng/orion/pull/276)
- Documentation updates include: 
  - Added content on configuring [Oracle](https://docs.orion.pegasys.tech/en/latest/Configuring-Orion/Using-Oracle/) and [PostgreSQL](https://docs.orion.pegasys.tech/en/latest/Configuring-Orion/Using-PostgreSQL/) databases
  - Added content on generating [certificates using OpenSSL](https://docs.orion.pegasys.tech/en/latest/Configuring-Orion/TLS/#generating-certificates-using-openssl)

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
  - Added content on [privacy groups](https://docs.orion.pegasys.tech/en/latest/Using-Orion/Privacy-Groups/)
  - Added [TLS content](https://docs.orion.pegasys.tech/en/latest/Configuring-Orion/TLS/)
  - Updated [Client API](https://docs.orion.pegasys.tech/en/latest/Reference/API-Methods/) reference to include privacy group methods
  
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

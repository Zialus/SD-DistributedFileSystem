# DFS-SD

## MetadataServer

The MetadataServer should be started without any arguments

## StorageServer

```bash
java StorageServer <localPath> <globalPath> <rmiHostName>
```

For localPath the user should pass as an argument the desired directory in the machine that the server will be running on

For globalPath the user should pass as an argument the sub-tree that that directory will be responsible for in the MetaDataServer. 

## Client

```bash
java Client <apps.conf> <rmiHostName>
```

Apps.conf is the file where the user stores his configurations for which app opens files ending on an particular extension.
# DFS-SD


[![Codacy Badge](https://api.codacy.com/project/badge/Grade/e981321e422f4e3a80206f89de2efbea)](https://www.codacy.com/app/raulmendesferreira/SD-DistributedFileSystem?utm_source=github.com&utm_medium=referral&utm_content=Zialus/SD-DistributedFileSystem&utm_campaign=badger)

## MetadataServer

``` bash
java fcup.MetadataServer
```

The MetadataServer should be started without any arguments

## StorageServer

``` bash
java fcup.StorageServer <localPath> <globalPath> <rmiHostName>
```

For localPath the user should pass as an argument the desired directory in the machine that the server will be running on.

For globalPath the user should pass as an argument the sub-tree that that directory will be responsible for in the MetaDataServer.

So far rmiHostName has only been tested as localhost.

## Client

``` bash
java fcup.Client <apps.conf> <rmiHostName>
```

Apps.conf is the file where the user stores his configurations for which app opens files ending on an particular extension.

So far rmiHostName has only been tested as localhost.

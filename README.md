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

## Renovate auto-merge

Renovate merges dependency PRs itself after their checks pass; it does not use
GitHub's platform auto-merge. It will not merge when checks are failing or
pending, or when no checks are reported. This applies to major updates too.

Before enabling this configuration, protect `master` with a branch protection
rule or ruleset that requires PRs, requires branches to be up to date, and
requires all PR checks to pass. Select every matrix check in **Build and Test**
and **Build with ErrorProne**, as well as **Sonarcloud**, **semgrep/ci**, both
CodeQL **Analyze** checks, and **Validate GitHub Actions and YAML**. Ensure
Renovate cannot bypass the rule. Required checks protect against a merge before
all expected checks have been reported. Adjust the required checks when workflows
or matrix entries change.

Verify the rule using Renovate PRs with passing, failing, and pending checks.
Only the passing PR should merge. Coverity Scan is scheduled or manually run,
not a PR check, so it cannot gate these merges.

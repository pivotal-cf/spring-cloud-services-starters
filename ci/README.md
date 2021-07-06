#Spring Cloud Services Starters CI Pipeline

## Releasing

### Releases

The original pipeline was decomposed into different jobs so that we could recover from each of them manually

### Fly

The pipeline can be deployed to Concourse using the following script:

```$bash
$ ./ci/scripts/set-pipeline.sh
```

### Release commands

If you don't want to click, you can trigger each job using the CLI:

To release a milestone:

```$bash
$ fly -t scs trigger-job -j release-test/stage-milestone
$ fly -t scs trigger-job -j release-test/promote-milestone
```

To release an RC:

```$bash
$ fly -t scs trigger-job -j release-test/stage-rc
$ fly -t scs trigger-job -j release-test/promote-rc
```

To release a GA:

```$bash
$ fly -t scs trigger-job -j release-test/stage-release
$ fly -t scs trigger-job -j release-test/promote-release
$ fly -t scs trigger-job -j release-test/distribute-release
$ fly -t scs trigger-job -j release-test/sync-to-maven-central
```

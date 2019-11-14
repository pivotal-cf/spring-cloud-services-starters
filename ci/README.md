#Spring Cloud Services Starters CI Pipeline

## Running CI pipeline locally
CircleCI allows pipelines to be run locally, this is very useful for development purposes

- Install circle command line tool to run local builds:
```$bash
curl -o /usr/local/bin/circleci https://circle-downloads.s3.amazonaws.com/releases/build_agent_wrapper/circleci && \
chmod +x /usr/local/bin/circleci
```

- Run script to execute pipeline
```$bash
ci/scripts/run-local-ci-job.sh
```

## Releasing

./pipeline.yml is a release pipeline inspired by the one created by the Spring boot team https://github.com/spring-projects/spring-boot/blob/master/ci/

It is composed of three different groups:

- The first group is a basic build so that we can always be sure which build we will be getting when releasing, followed by the acceptance tests.
- The second group is the releases.
- The last group is the CI image used by the different tasks and the source can be found here: `ci/images/release-ci-image/Dockerfile`.

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

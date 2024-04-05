# CentralPublisher
Simple publishing to Sonatype Central Publishing Portal.

Augments the built-in `maven-publish` plugin.

### Usage:
```groovy
publishing {
    // ...
}
centralPublishing {
    // Your credentials, Store these somewhere safe!
    credentials {
        username = 'username'
        password = 'password'
    }
    // Valid values:
    // 'USER_MANAGED' -> Stops the deployment at VALIDATED, awaiting approval inside the Portal.
    // 'AUTOMATIC' -> Full automated rollout.
    publishingType = 'USER_MANAGED'
    // The publication you wish to publish.
    forPublication publishing.publications.MyPublication
}
```

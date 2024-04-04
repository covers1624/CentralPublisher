package net.covers1624.gcp;

import org.gradle.api.DefaultTask;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.artifacts.PublishException;
import org.gradle.api.credentials.PasswordCredentials;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.internal.publication.MavenPublicationInternal;
import org.gradle.api.publish.maven.internal.publisher.MavenNormalizedPublication;
import org.gradle.api.tasks.*;

import java.util.concurrent.Callable;

/**
 * Created by covers1624 on 3/4/24.
 */
@UntrackedTask (because = "This task uploads to Maven Central. There is nothing to cache.")
public abstract class PublishToCentralTask extends DefaultTask {

    private final Property<MavenPublicationInternal> publication = getProject().getObjects().property(MavenPublicationInternal.class);
    private final Property<String> publishingType = getProject().getObjects().property(String.class);
    private final Property<PasswordCredentials> credentials = getProject().getObjects().property(PasswordCredentials.class);

    public PublishToCentralTask() {
        getInputs()
                .files((Callable<FileCollection>) () -> {
                    MavenPublicationInternal publication = (MavenPublicationInternal) getPublication();
                    return publication != null ? publication.getPublishableArtifacts().getFiles() : null;
                })
                .withPropertyName("publication.publishableFiles")
                .withPathSensitivity(PathSensitivity.NAME_ONLY);
    }

    @Internal
    public MavenPublication getPublication() {
        return publication.get();
    }

    public Property<String> getPublishingType() {
        return publishingType;
    }

    @Nested
    public Property<PasswordCredentials> getCredentials() {
        return credentials;
    }

    @TaskAction
    public void publish() {
        MavenPublicationInternal pub = publication.getOrNull();
        if (pub == null) throw new InvalidUserDataException("Property 'publication' is required.");
        MavenNormalizedPublication normalPub = pub.asNormalisedPublication();

        PasswordCredentials credentials = getCredentials().getOrNull();
        if (credentials == null) throw new InvalidUserDataException("Property 'credentials' is required.");
        if (credentials.getUsername() == null) throw new InvalidUserDataException("Property 'credentials.username' is required.");
        if (credentials.getPassword() == null) throw new InvalidUserDataException("Property 'credentials.password' is required.");

        String publishingType = getPublishingType().getOrNull();
        if (publishingType == null) throw new InvalidUserDataException("Property 'publishingType' is required.");

        try {
            SimplePublicationValidator.validatePublication(normalPub);
            CentralPublisher.publish(normalPub, credentials, publishingType, getTemporaryDir().toPath());
        } catch (Throwable ex) {
            throw new PublishException("Failed to publish " + normalPub.getName() + " to central publishing portal.");
        }
    }

    public void setPublication(MavenPublication publication) {
        if (publication != null && !(publication instanceof MavenPublicationInternal)) {
            throw new InvalidUserDataException("Expected MavenPublicationInternal. Got: " + publication.getClass().getName());
        }
        this.publication.set(((MavenPublicationInternal) publication));
    }
}

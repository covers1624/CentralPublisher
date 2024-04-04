package net.covers1624.gcp;

import org.gradle.api.Action;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.api.credentials.PasswordCredentials;
import org.gradle.api.provider.Provider;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.plugins.PublishingPlugin;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.Nullable;

import static org.codehaus.groovy.runtime.StringGroovyMethods.capitalize;

/**
 * Created by covers1624 on 3/4/24.
 */
public abstract class CentralPublishExtension {

    private final Project project;
    private final PasswordCredentials credentials = new PasswordCredentialsImpl();

    public String publishingType = "USER_MANAGED";

    public CentralPublishExtension(Project project) {
        this.project = project;
    }

    public PasswordCredentials getCredentials() {
        return credentials;
    }

    public void credentials(Action<PasswordCredentials> creds) {
        creds.execute(credentials);
    }

    public void forPublication(MavenPublication publication) {
        String taskName = "publish" + capitalize(publication.getName()) + "PublicationToCentralPublishingPortal";
        if (project.getTasks().getNames().contains(taskName)) {
            throw new InvalidUserDataException("Already configured this publication. " + publication.getName());
        }
        TaskProvider<PublishToCentralTask> publishTask = project.getTasks().register(taskName, PublishToCentralTask.class, task -> {
            task.setGroup(PublishingPlugin.PUBLISH_TASK_GROUP);
            task.getPublishingType().value(project.provider(() -> publishingType));
            task.getCredentials().value(project.provider(this::getCredentials));
            task.setPublication(publication);
        });
        project.getTasks().named(GradleCentralPublisherPlugin.CENTRAL_PUBLISH_LIFECYCLE).configure(t -> t.dependsOn(publishTask));
    }

    // @formatter:off
    private static final class PasswordCredentialsImpl implements PasswordCredentials {
        private @Nullable String username;
        private @Nullable String password;
        @Override public @Nullable String getUsername() { return username; }
        @Override public @Nullable String getPassword() { return password; }
        @Override public void setUsername(@Nullable String username) { this.username = username; }
        @Override public void setPassword(@Nullable String password) { this.password = password; }
    }
    // @formatter:on
}

package net.covers1624.gcp;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.publish.plugins.PublishingPlugin;

/**
 * Created by covers1624 on 3/4/24.
 */
public class GradleCentralPublisherPlugin implements Plugin<Project> {

    public static final String CENTRAL_PUBLISH_LIFECYCLE = "publishToMavenCentral";

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(MavenPublishPlugin.class);
        project.getTasks().register(CENTRAL_PUBLISH_LIFECYCLE, t -> {
            t.setDescription("Publishes all configured publications to the maven central publisher portal.");
            t.setGroup(PublishingPlugin.PUBLISH_TASK_GROUP);
        });
        project.getTasks().named(PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME).configure(e -> e.dependsOn(CENTRAL_PUBLISH_LIFECYCLE));
        project.getExtensions().create("centralPublishing", CentralPublishExtension.class, project);
    }
}

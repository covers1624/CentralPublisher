package net.covers1624.gcp;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.gradle.api.publish.maven.InvalidMavenPublicationException;
import org.gradle.api.publish.maven.MavenArtifact;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.internal.publisher.MavenNormalizedPublication;
import org.gradle.api.publish.maven.internal.publisher.ValidatingMavenPublisher;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Heavily simplified implementation of {@link ValidatingMavenPublisher}.
 * <p>
 * Created by covers1624 on 3/4/24.
 */
class SimplePublicationValidator {

    private static final String VALID_REGEX = "[A-Za-z0-9_\\-.]+";

    public static void validatePublication(MavenNormalizedPublication pub) {
        validatePom(pub);
        validateArtifacts(pub);
    }

    private static void validatePom(MavenNormalizedPublication pub) {
        Model model;
        File pomFile = pub.getPomArtifact().getFile();
        try (FileReader reader = new FileReader(pomFile)) {
            model = new MavenXpp3Reader().read(reader);
            model.setPomFile(pomFile);
        } catch (IOException | XmlPullParserException ex) {
            throw new InvalidMavenPublicationException(pub.getName(), "POM file is malformed.", ex);
        }

        // The following pom properties must be valid identifiers for maven, and equal the publication data:
        // - Artifact Id
        // - Group, only if there is no parent.
        // - Version, only if there is no parent.

        validateIdentifier(pub, "Artifact Id", pub.getArtifactId(), model.getArtifactId(), true);
        validateIdentifier(pub, "Group Id", pub.getGroupId(), model.getGroupId(), model.getParent() == null);

        if (pub.getVersion().isEmpty()) throw new InvalidMavenPublicationException(pub.getName(), "Version can't be empty.");
        validateIdentifier(pub, "Version", pub.getVersion(), model.getVersion(), model.getParent() == null);
        validateFileNameable(pub, "Version", pub.getVersion());
    }

    private static void validateArtifacts(MavenNormalizedPublication pub) {
        List<MavenArtifact> artifacts = new ArrayList<>(pub.getAllArtifacts());
        for (int i = 0; i < artifacts.size(); i++) {
            MavenArtifact artifact = artifacts.get(i);
            String ext = artifact.getExtension();
            String cls = artifact.getClassifier();
            if (ext.isEmpty()) throw new InvalidMavenPublicationException(pub.getName(), "Artifact extension can't be empty.");
            validateFileNameable(pub, "Artifact extension", ext);
            if (cls != null) {
                if (cls.isEmpty()) throw new InvalidMavenPublicationException(pub.getName(), "Artifact classifier can't be empty if present.");
                validateFileNameable(pub, "Artifact classifier", cls);
            }
            validateIsFileAndExists(pub, artifact.getFile());
            // Ensure the artifact isn't a duplicate of any others.
            // We only need to check the remaining artifacts, as this check is 2 way.
            for (int j = i + 1; j < artifacts.size(); j++) {
                MavenArtifact other = artifacts.get(j);
                if (Objects.equals(ext, other.getExtension()) && Objects.equals(cls, other.getClassifier())) {
                    throw new InvalidMavenPublicationException(pub.getName(), "Multiple artifacts with the same extension and classifier. " + ext + " " + cls);
                }
            }
        }
    }

    private static void validateIsFileAndExists(MavenNormalizedPublication pub, File file) {
        if (file == null || !file.exists()) throw new InvalidMavenPublicationException(pub.getName(), "Artifact file " + file + " does not exist.");
        if (file.isDirectory()) {
            throw new InvalidMavenPublicationException(pub.getName(), "Artifact file " + file + " must be a file. Not a directory.");
        }
    }

    private static void validateIdentifier(MavenNormalizedPublication pub, String what, String publicationValue, String modelValue, boolean requireEqual) {
        if (!publicationValue.matches(VALID_REGEX)) throw new InvalidMavenPublicationException(pub.getName(), "Invalid " + what + "." + publicationValue);
        if (requireEqual && !publicationValue.equals(modelValue)) throw new InvalidMavenPublicationException(pub.getName(), what + " does not match pom. " + publicationValue + " " + modelValue);
    }

    private static void validateFileNameable(MavenNormalizedPublication pub, String what, String value) {
        if (value == null || value.isEmpty()) return;

        int u;
        for (int i = 0; i < value.length(); i += Character.charCount(u)) {
            u = value.codePointAt(i);
            if (Character.isISOControl(u)) throw new InvalidMavenPublicationException(pub.getName(), String.format("%s contains ISC control character. \\u%04x", what, u));
        }
    }
}

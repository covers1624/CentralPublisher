package net.covers1624.gcp;

import org.gradle.api.artifacts.PublishException;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.credentials.PasswordCredentials;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.publish.maven.InvalidMavenPublicationException;
import org.gradle.api.publish.maven.MavenArtifact;
import org.gradle.api.publish.maven.internal.publisher.MavenNormalizedPublication;
import org.gradle.internal.Factory;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by covers1624 on 3/4/24.
 */
public class CentralPublisher {

    private static final Logger LOGGER = Logging.getLogger(CentralPublisher.class);

    public static void publish(MavenNormalizedPublication publication, PasswordCredentials credentials, String publishingType, Path tempDir) {
        ModuleComponentIdentifier ident = publication.getProjectIdentity();
        // At least I think it doesn't support them.
        if (ident.getVersion().toUpperCase().endsWith("-SNAPSHOT")) {
            throw new InvalidMavenPublicationException(publication.getName(), "Central publishing portal does not support snapshot artifacts.");
        }
        Path bundle = tempDir.resolve(ident.getModule() + "-" + ident.getVersion() + ".zip");
        LOGGER.info("Creating bundle for {}:{}:{} at {}", ident.getGroup(), ident.getModule(), ident.getVersion(), bundle);

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(bundle))) {
            for (MavenArtifact artifact : publication.getAllArtifacts()) {
                writeToZip(zos, ident, artifact);
            }
        } catch (IOException ex) {
            throw new PublishException("Failed to create bundle.", ex);
        }
        LOGGER.info("Bundle created.");

        LOGGER.info("Uploading bundle..");
        String bundleId;
        try {
            bundleId = SonatypeApi.uploadBundle(credentials, bundle, publishingType);
        } catch (IOException e) {
            throw new PublishException("Failed to upload bundle.", e);
        }
        LOGGER.lifecycle("Uploaded to Central with bundle id: {}", bundleId);
    }

    private static void writeToZip(ZipOutputStream zos, ModuleComponentIdentifier ident, MavenArtifact artifact) throws IOException {
        writeToZip(
                zos,
                new ZipEntry(fullNameAndPath(ident.getGroup(), ident.getModule(), ident.getVersion(), artifact.getClassifier(), artifact.getExtension())),
                artifact.getFile()
        );
        writeHashesToZip(zos, ident, artifact);
    }

    private static void writeHashesToZip(ZipOutputStream zos, ModuleComponentIdentifier ident, MavenArtifact artifact) throws IOException {
        writeHashToZip(zos, ident, artifact, "MD5");
        writeHashToZip(zos, ident, artifact, "SHA-1");
        writeHashToZip(zos, ident, artifact, "SHA-256");
        writeHashToZip(zos, ident, artifact, "SHA-512");
    }

    private static void writeHashToZip(ZipOutputStream zos, ModuleComponentIdentifier ident, MavenArtifact artifact, String alg) throws IOException {
        String ext = alg.toLowerCase(Locale.ROOT).replace("-", "");
        writeToZip(
                zos,
                new ZipEntry(fullNameAndPath(ident.getGroup(), ident.getModule(), ident.getVersion(), artifact.getClassifier(), artifact.getExtension() + "." + ext)),
                () -> new ByteArrayInputStream(hash(artifact, alg))
        );
    }

    private static void writeToZip(ZipOutputStream zos, ZipEntry entry, File file) throws IOException {
        writeToZip(zos, entry, () -> Files.newInputStream(file.toPath()));
    }

    private static void writeToZip(ZipOutputStream zos, ZipEntry entry, StreamSupplier supplier) throws IOException {
        LOGGER.info(" Adding {} to zip.", entry.getName());
        zos.putNextEntry(entry);
        try (InputStream is = supplier.open()) {
            byte[] buf = new byte[4096];
            int len;
            while ((len = is.read(buf)) != -1) {
                zos.write(buf, 0, len);
            }
        }
        zos.closeEntry();
    }

    private static byte[] hash(MavenArtifact artifact, String alg) throws IOException {
        return Utils.hashFile(alg, artifact.getFile().toPath())
                .getBytes(StandardCharsets.UTF_8);
    }

    private static String fullNameAndPath(String group, String artifact, String version, @Nullable String classifier, String extension) {
        return group.replace('.', '/')
               + "/" + artifact
               + "/" + version
               + "/" + artifact
               + "-" + version
               + (classifier != null ? "-" + classifier : "")
               + "." + extension;
    }

    private interface StreamSupplier {

        InputStream open() throws IOException;
    }
}

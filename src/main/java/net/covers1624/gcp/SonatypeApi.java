package net.covers1624.gcp;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.gradle.api.credentials.PasswordCredentials;
import org.jetbrains.annotations.ApiStatus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;

/**
 * Created by covers1624 on 3/4/24.
 */
@ApiStatus.Internal
class SonatypeApi {

    private static final String API = "https://central.sonatype.com/api";

    public static String uploadBundle(PasswordCredentials credentials, Path bundle, String publishingType) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(API + "/v1/publisher/upload?publishingType=" + publishingType);
            post.addHeader("Authorization", "Bearer " + authBase64(credentials));
            post.setEntity(MultipartEntityBuilder.create()
                    .addPart("bundle", new FileBody(bundle.toFile(), ContentType.APPLICATION_OCTET_STREAM))
                    .build()
            );
            try (CloseableHttpResponse response = client.execute(post)) {
                String body = readBody(response);
                if (response.getStatusLine().getStatusCode() != 201) {
                    throw new IOException("Failed to upload. Got: " + response.getStatusLine() + "\n" + body);
                }

                return body;
            }
        }
    }

    private static String authBase64(PasswordCredentials credentials) {
        return Base64.getEncoder().encodeToString((credentials.getUsername() + ":" + credentials.getPassword()).getBytes(StandardCharsets.UTF_8));
    }

    private static String readBody(CloseableHttpResponse response) throws IOException {
        HttpEntity entity = response.getEntity();
        if (entity == null) return "";

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (InputStream is = entity.getContent()) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = is.read(buf)) != -1) {
                os.write(buf, 0, len);
            }
        }
        return os.toString();
    }
}

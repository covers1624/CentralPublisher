package net.covers1624.gcp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by covers1624 on 3/4/24.
 */
class Utils {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    public static String hashFile(String alg, Path file) throws IOException {
        MessageDigest digest = getDigest(alg);
        addToDigest(digest, file);
        return finishHash(digest);
    }

    public static MessageDigest getDigest(String alg) {
        try {
            return MessageDigest.getInstance(alg);
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("Expected hashing algorithm " + alg + " to exist.", ex);
        }
    }

    public static String finishHash(MessageDigest digest) {
        byte[] hash = digest.digest();
        StringBuilder sb = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            sb.append(HEX[(b >> 4) & 0xF]);
            sb.append(HEX[b & 0xF]);
        }
        return sb.toString();
    }

    public static void addToDigest(MessageDigest digest, Path file) throws IOException {
        try (InputStream is = Files.newInputStream(file)) {
            byte[] buf = new byte[2048];
            int len;
            while ((len = is.read(buf)) != -1) {
                digest.update(buf, 0, len);
            }
        }
    }
}

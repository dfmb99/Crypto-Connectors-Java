package bitmex.utils;

import bitmex.Bitmex;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.logging.Logger;

import static org.apache.commons.codec.digest.HmacAlgorithms.HMAC_SHA_256;

public class Auth {
    private final static Logger LOGGER = Logger.getLogger(Bitmex.class.getName());

    /**
     * Builds a hmac signature given a key and data
     *
     * @param key  - key to be used in the hmac signature
     * @param data - data to be used in the hmac signature
     * @return result of the hmac signature encryption as String
     */
    public static String encode_hmac(String key, String data) {
        try {
            Mac sha256_HMAC = Mac.getInstance(HMAC_SHA_256.getName());
            SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA_256.getName());
            sha256_HMAC.init(secret_key);
            return Hex.encodeHexString(sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            LOGGER.severe(e.getMessage());
            System.exit(1);
            return null;
        }
    }

    /**
     * Generates expires field to authenticate
     * @return expires as long
     */
    public static long generate_expires() {
        return Instant.now().getEpochSecond() + 3600;
    }
}

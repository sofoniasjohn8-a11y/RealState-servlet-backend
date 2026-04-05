import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

/**
 * Small token utility producing a compact HMAC-SHA256 signed token (JWT-like).
 * This is a minimal implementation for demo purposes only.
 */
public class TokenUtil {
    // In a real app store this secret outside source code (env var, config, vault)
    private static final String SECRET = System.getProperty("app.token.secret", "change_this_secret");

    public static String createToken(int userId, String username, long ttlSeconds) {
        try {
            long exp = Instant.now().getEpochSecond() + ttlSeconds;
            String header = toJson(Map.of("alg", "HS256", "typ", "JWT"));
            String payload = toJson(Map.of("sub", String.valueOf(userId), "username", username, "exp", String.valueOf(exp)));
            String encodedHeader = base64UrlEncode(header.getBytes(StandardCharsets.UTF_8));
            String encodedPayload = base64UrlEncode(payload.getBytes(StandardCharsets.UTF_8));
            String signingInput = encodedHeader + "." + encodedPayload;
            String sig = base64UrlEncode(hmacSha256(signingInput.getBytes(StandardCharsets.UTF_8), SECRET.getBytes(StandardCharsets.UTF_8)));
            return signingInput + "." + sig;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create token", e);
        }
    }

    public static boolean verifyToken(String token) {
        try {
            if (token == null) return false;
            String[] parts = token.split("\\.");
            if (parts.length != 3) return false;
            String signingInput = parts[0] + "." + parts[1];
            byte[] expected = hmacSha256(signingInput.getBytes(StandardCharsets.UTF_8), SECRET.getBytes(StandardCharsets.UTF_8));
            byte[] sig = base64UrlDecode(parts[2]);
            if (!java.security.MessageDigest.isEqual(expected, sig)) return false;
            // check exp
            String payloadJson = new String(base64UrlDecode(parts[1]), StandardCharsets.UTF_8);
            String expStr = extractJsonValue(payloadJson, "exp");
            if (expStr == null) return false;
            long exp = Long.parseLong(expStr);
            return Instant.now().getEpochSecond() < exp;
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] hmacSha256(byte[] data, byte[] key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    private static String base64UrlEncode(byte[] b) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static byte[] base64UrlDecode(String s) {
        return Base64.getUrlDecoder().decode(s);
    }

    // Very small JSON helpers (not a full JSON encoder)
    private static String toJson(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(escape(e.getKey())).append('"').append(':').append('"').append(escape(e.getValue())).append('"');
        }
        sb.append('}');
        return sb.toString();
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String extractJsonValue(String body, String key) {
        if (body == null) return null;
        String search = '"' + key + '"';
        int idx = body.indexOf(search);
        if (idx == -1) return null;
        int colon = body.indexOf(':', idx);
        if (colon == -1) return null;
        int start = body.indexOf('"', colon);
        if (start == -1) return null;
        int end = body.indexOf('"', start + 1);
        if (end == -1) return null;
        return body.substring(start + 1, end);
    }
    
}

package eiss.jwt;

import io.jsonwebtoken.*;

import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.util.*;
import java.util.regex.PatternSyntaxException;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Wrapper for decoding JWTs with convenience methods for accessing
 * eiss specific user, group and role attributes.
 *
 * import eiss.jwt.Jwt;
 * import eiss.jwt.ExpiredTokenException;
 *
 * try {
 *     Jwt jwt = new Jwt();
 *     jwt.decode(token);
 *     // or jwt.decodeAuthHeader(authHeader);
 *
 *     String user = Jwt.getUser();
 *     String group = Jwt.getGroup();
 *     String role = Jwt.getRole();
 *     Date expires = Jwt.getExpiration();
 * }
 * catch(ExpiredTokenException ex) {
 *     // token has expired
 * }
 * catch(IllegalArgumentException ex) {
 *     // invalid token
 * }
 */
public class Jwt {

    private String user;
    private String group;
    private String role;
    private Date expires;
    private Map<String, Object> claims;

    private void reset() {
        user = group = role = "";
        expires = null;
        claims = new HashMap<>();
    }

    public Jwt() {
        reset();
    }

    public String getUser() { return user; }
    public String getGroup() { return group; }
    public String getRole() { return role; }
    public Date getExpiration() { return expires; }
    Map<String, Object> getClaims() { return claims; }

    /**
     * Decode the contents of header containing a JWT (Bearer scheme)
     * @param key The server secret key
     * @param header Contents of the Authorization header
     * @throws ExpiredTokenException If the token is expired
     * @throws IllegalArgumentException If the token is invalid
     */
    public void decodeAuthHeader(String key, String header)
        throws ExpiredTokenException, IllegalArgumentException
    {
        final String msg = "Invalid Authorization header";
        try {
            String[] parts = header.split(" ");
            if (parts.length < 2) throw new IllegalArgumentException(msg);
            decode(key, parts[1]);
        }
        catch (PatternSyntaxException ex) {
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * Decode a JWT
     * @param key The server secret key
     * @throws ExpiredTokenException If the token is expired
     * @throws IllegalArgumentException If the token is invalid
     */
    public void decode(String key, String token)
        throws ExpiredTokenException, IllegalArgumentException
    {
        if (expires != null) {
            reset(); // clear from previous
        }

        try {
            //byte[] keyBytes = Base64.getDecoder().decode(key);
            byte[] keyBytes = DatatypeConverter.parseBase64Binary(key);

            Claims tokenClaims = Jwts.parser()
                    .setSigningKey(keyBytes)
                    .parseClaimsJws(token).getBody();

            expires = tokenClaims.getExpiration();
            claims.put("exp", expires.getTime());
            claims.put("iss", tokenClaims.getIssuer());
            claims.put("id", tokenClaims.getId());

            String subject = tokenClaims.getSubject();
            if (subject == null) {
                subject = "";
            }

            String scope = "";
            Object sobj = tokenClaims.get("scope");
            if (sobj != null) {
                scope = (String)sobj;
            }

            claims.put("sub", subject);
            claims.put("scope", scope);

            role = !scope.isEmpty() ? scope : "user";
            String[] parts = subject.split("/");
            if (parts.length == 2) {
                group = parts[0];
                user = parts[1];
            }
        } catch (ExpiredJwtException ex) {
            throw new ExpiredTokenException("Token has expired");
        } catch (SignatureException | MalformedJwtException ex) {
            throw new IllegalArgumentException(ex.getMessage());
        }
    }

    /**
     * Generate a token. Mainly used for tests
     * @param key server key (String)
     * @param user authorized user (String)
     * @param group authorized group (String)
     * @param role users role (String)
     * @return an encoded JWT
     */
    public static String createToken(String key, String user, String group, String role, long expireMsec)
    {
        SignatureAlgorithm alg = SignatureAlgorithm.HS256;
        Date now = new Date();
        Date exp = new Date(now.getTime() + expireMsec * 1000);
        String id = java.util.UUID.randomUUID().toString();

        //byte[] keyBytes = Base64.getDecoder().decode(key);
        byte[] keyBytes = DatatypeConverter.parseBase64Binary(key);


        Key signingKey = new SecretKeySpec(keyBytes, alg.getJcaName());

        String subject = group + "/" + user;
        Map<String, Object> scope = new HashMap<>();
        scope.put("scope", role);
        scope.put("sub", subject);
        scope.put("iss", "http://eiss.ipkeys.com");
        scope.put("id", id);

        JwtBuilder builder = Jwts.builder()
            .setId(id)
            .setIssuedAt(now)
            .setClaims(scope)
            .setExpiration(exp)
            .signWith(alg, signingKey);

        builder.setExpiration(exp);
        return builder.compact();
    }

    public static String createToken(String key, String user, String group, String role) {
        return createToken(key, user, group, role, 60*10);
    }

    //--------------------------------------------------------------------------
    // Create or decode a token via the cloudven line
    //--------------------------------------------------------------------------
    public static void main(String[] args) throws Exception {
        final String usage =
            "usage:\n\n" +
            "  jwt decode <token>\n" +
            "  jwt create user group role [expiresMillis] (default 60*10*1000 = 10 min)";

        // this is currently the key in ~/eiss/auth/serverkey.txt
        // keys generated by ~/eiss/auth/genkey.sh
        StringBuilder key = new StringBuilder();

        String eissHome = System.getenv("EISS_HOME");
        if (eissHome == null) {
            System.out.println("EISS_HOME needs to be set to locate key");
            System.exit(1);
        }

        if (!eissHome.endsWith("/")) {
            eissHome = eissHome + '/';
        }

        String keyFile = eissHome + "config/serverkey.txt";
        System.out.println("Using key file: " + keyFile);

        Files.readAllLines(Paths.get(keyFile), UTF_8).forEach(key::append);

        if (args.length == 2 && args[0].toUpperCase().equals("DECODE")) {
            try {
                Jwt jwt = new Jwt();
                jwt.decode(key.toString(), args[1]);
                System.out.println("user   : " + jwt.getUser());
                System.out.println("group  : " + jwt.getGroup());
                System.out.println("role   : " + jwt.getRole());
                System.out.println("expires: " + jwt.getExpiration());
                System.out.println("-------- claims --------");
                System.out.println("id    : " + jwt.getClaims().get("id"));
                System.out.println("iss   : " + jwt.getClaims().get("iss"));
                System.out.println("sub   : " + jwt.getClaims().get("sub"));
                System.out.println("scope : " + jwt.getClaims().get("scope"));
                System.out.println("exp   : " + jwt.getClaims().get("exp"));
            } catch (ExpiredTokenException | IllegalArgumentException ex) {
                System.out.println("ERROR:" + ex.getMessage());
            }
        } else if ((args.length == 4 || args.length == 5 ) && args[0].toUpperCase().equals("CREATE")) {
            String token = "";
            if (args.length == 4) {
                token = Jwt.createToken(key.toString(), args[1], args[2], args[3]);
            } else {
                long millis;
                try {
                    millis = Integer.parseInt(args[4]);
                    if (millis < 0L) {
                        System.out.println("\nERROR: milliseconds must be >= 0");
                        return;
                    }
                    token = Jwt.createToken(key.toString(), args[1], args[2], args[3], millis);
                } catch (NumberFormatException ex) {
                    System.out.println("\nERROR: milliseconds must be a number");
                }
            }

            Jwt jwt = new Jwt(); // parse to show expiration
            try {
                jwt.decode(key.toString(), token);
                String msg =
                    String.format("------------------[ token expires: %s ]-------------------",
                        jwt.getExpiration().toString());
                System.out.println(msg);
                System.out.println(token);
            } catch (Exception ex) {
                System.out.println(ex.toString());
            }
        } else {
            System.out.println(usage);
        }
    }

}

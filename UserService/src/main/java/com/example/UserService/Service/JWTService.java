package com.example.UserService.Service;

import com.example.UserService.constant.SecurityConst;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.StringJoiner;
import java.util.function.Function;
@Service
public class JWTService {
    public String generateToken(Authentication authentication){
        SecretKey key = (SecretKey) getStringToKey(SecurityConst.JWT_KEY);
//        String authorities = (String) claims.get("authorities");
//        if (authorities.startsWith("[") && authorities.endsWith("]")) {
//            authorities = authorities.substring(1, authorities.length() - 1);
//        }
//        claims.put("sub", "vu@");
//        claims.put("email", "vu@");
//        claims.put("authorities", authorities);
        return Jwts.builder()
                .subject(authentication.getName())
                .claim("email",authentication.getName())
                .claim("authorities",buildRole(authentication))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() +1000 * 60 * 3)) //3 minutes
                .signWith(key)
                .compact();
    }

    /**
     *
     * change authority in payload claims ,from list to string eg:  [ student,teacher] -> "student teacher"
     */
    private String buildRole(Authentication authentication) {
        StringJoiner joiner = new StringJoiner(" ");
        if (!authentication.getAuthorities().isEmpty()) {
            authentication.getAuthorities().forEach(authority ->
                    joiner.add(authority.getAuthority())
            );
        }
        return joiner.toString();
    }
    /**
     * get secret key as input
     * @param key
     * @return
     */
    public Key getStringToKey(String key){
        return Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8));
    }
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    private Claims extractAllClaims(String token) {
        return Jwts
                .parser()
                .verifyWith((SecretKey) getStringToKey(SecurityConst.JWT_KEY))
                .build().
                parseSignedClaims(token).
                getPayload();
    }
    public String getEmailFromToken(String token){
        SecretKey key = (SecretKey) getStringToKey(SecurityConst.JWT_KEY);
        Claims claims=Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    public boolean isTokenExpired(String token){
        return extractExpiration(token).before(new Date());
    }
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String email = getEmailFromToken(token);
        return (email.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }
}
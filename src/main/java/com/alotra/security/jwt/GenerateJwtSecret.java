package com.alotra.security.jwt;

import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.SignatureAlgorithm;
import javax.crypto.SecretKey;

public class GenerateJwtSecret {
    public static void main(String[] args) {
        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        String base64Key = Encoders.BASE64.encode(key.getEncoded());
        System.out.println("🔑 JWT Secret (Base64): " + base64Key);
    }
    
}
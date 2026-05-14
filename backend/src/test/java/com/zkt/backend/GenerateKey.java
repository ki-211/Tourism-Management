package com.zkt.backend;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Base64;

public class GenerateKey {
    public static void main(String[] args) {
        // 生成一个用于HS256算法的安全密钥
        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS256);//eYAK/679sgdyRw7Y4CiT4yZ528fbvzEN1FTZB4Csrx4=
        // 将密钥Base64编码成字符串
        String base64Key = Base64.getEncoder().encodeToString(key.getEncoded());
        System.out.println("Base64 encoded secret key: " + base64Key);
    }
}

package org.android.onviflibrary;

import android.text.TextUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class OnvifDigestInformation {
    private String username;
    private String password;
    private String uriPath;
    private String digestHeader;

    public OnvifDigestInformation(String username, String password, String uriPath, String digestHeader) {
        this.username = username;
        this.password = password;
        this.uriPath = uriPath;
        this.digestHeader = digestHeader;
    }

    public String getAuthorizationHeader() {
        extractDigest();
        String ha1 = md5((String) TextUtils.concat(username, ":", realm, ":", password));
        String ha2 = md5((String) TextUtils.concat("POST", ":", uriPath));
        String response = md5((String) TextUtils.concat(ha1, ":", nonce, ":", nc, ":", cnonce, ":", qop, ":", ha2));
        return (String) TextUtils.concat(
                "Digest username=\"", username, "\", realm=\"", realm, "\", nonce=\"", nonce, "\", uri=\"", uriPath, "\", response=\"", response,
                "\", cnonce=\"", cnonce, "\", nc=", nc, ", qop=\"", qop, "\"");
    }

    private String md5(String string) {
        String HEX_CHARS = "0123456789abcdef";
        byte[] bytes;
        try {
            bytes = MessageDigest.getInstance("MD5")
                    .digest(string.getBytes());
            StringBuilder result = new StringBuilder(bytes.length * 2);

            for (byte b : bytes) {
                int i = b & 0xFF;
                result.append(HEX_CHARS.charAt(i >> 4 & 0x0f));
                result.append(HEX_CHARS.charAt(i & 0x0f));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String realm = "realm";
    private String nonce = "nonce";
    private String qop = "auth";
    private String cnonce = "a1b390a149f9085d64598b75f3a9e0f1";
    private String nc = "00000001";

    private void extractDigest() {
        String substring = digestHeader.substring(7);
        String[] split = substring.split(",");
        for (String s : split) {
            String[] pair = s.split("=");
            String key = pair[0].trim();
            String value = pair[1].replaceAll("\"", "").trim();
            if (TextUtils.isEmpty(key)) {
                continue;
            }

            switch (key) {
                case "realm":
                    realm = value;
                    break;
                case "nonce":
                    nonce = value;
                    break;
                case "qop":
                    qop = value;
                    break;
            }
        }
    }
}

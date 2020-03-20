package org.android.onviflibrary;

import android.util.Base64;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

public abstract class OnvifRequest {
    public static final String COMMAND
            = "<GetCapabilities xmlns=\"http://www.onvif.org/ver10/device/wsdl\"/>";

    /*
        <?xml version="1.0" encoding="utf-8"?>
        <s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope">
     */
    private static final String DOCUMENT_START = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
            "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\">";

    /*
        </s:Envelope>
     */
    private static final String DOCUMENT_END = "</s:Envelope>";

    /*
     * <s:Body xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema">
     */
    private static final String BODY_START = "<s:Body xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">";

    /*
     * </s:Body>
     */
    private static final String BODY_END = "</s:Body>";

    private static final String FORMAT_HEADER = "<s:Header>" +
            "<wsse:Security xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">" +
            "<wsse:UsernameToken>" +
            // username
            "<wsse:Username>%s</wsse:Username>" +
            // password
            " <wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordDigest\">%s</wsse:Password>" +
            // Nonce
            "<wsse:Nonce>%s</wsse:Nonce>" +
            // format-time: yyyy-MM-ddTHH:mm:ssZ
            "<wsu:Created>%s</wsu:Created>" +
            "</wsse:UsernameToken>" +
            "</wsse:Security>" +
            "</s:Header>";

    private static final String FORMAT_URL
            = "http://%s"   // host
            + "%s";         // path
    private static final String FORMAT_URL_WITH_PORT
            = "http://%s"   // host
            + ":" + "%d"    // port
            + "%s";         // path
    public static final int DEFAULT_PORT = 80;
    private final String url;
    private final String host;
    private final String path;
    private final int port;
    private final String username;
    private final String password;
    private boolean wsUsernameToken;

    private static final String CONTENT_TYPE = "application/soap+xml;charset=utf-8";

    private Map<String, String> headers = new HashMap<>();

    public OnvifRequest(String url, String username, String password, boolean wsUsernameToken) throws MalformedURLException {
        this.url = url;
        URL u = new URL(url);
        this.host = u.getHost();
        this.path = u.getPath();
        this.port = u.getPort();
        this.username = username;
        this.password = password;
        this.wsUsernameToken = wsUsernameToken;
        headers.put("Content-Type", CONTENT_TYPE);
    }

    public OnvifRequest(String host, int port, String path, String username, String password, boolean wsUsernameToken) throws MalformedURLException {
        this(
                port == DEFAULT_PORT ? String.format(Locale.CHINA, FORMAT_URL, host, path) :
                        String.format(Locale.CHINA, FORMAT_URL_WITH_PORT, host, port, path),
                username, password, wsUsernameToken);
    }


    public String getUrl() {
        return url;
    }

    public String getContent() {
        StringBuilder sBuilder = new StringBuilder();

        sBuilder.append(DOCUMENT_START);
        if (wsUsernameToken) {
            String utcTime = getUTCTime();
            Random random = new Random();
            String nonce = String.valueOf(random.nextInt());
            // HEADER
            sBuilder.append(String.format(
                    Locale.CHINA, FORMAT_HEADER,
                    username,
                    getEncryptedPassword(nonce, utcTime, password),
                    getEncryptedNonce(nonce), utcTime));
        }
        sBuilder.append(BODY_START);
        sBuilder.append(getCommand());
        sBuilder.append(BODY_END);
        sBuilder.append(DOCUMENT_END);

        return sBuilder.toString();
    }

    private static String getEncryptedNonce(String nonce) {
        return Base64.encodeToString(nonce.getBytes(), Base64.NO_WRAP);
    }

    private static String getEncryptedPassword(String nonce, String utcTime, String password) {
        String beforeEncryption = nonce + utcTime + password;
        byte[] encryptedRaw;
        try {
            MessageDigest SHA1 = MessageDigest.getInstance("SHA1");
            SHA1.reset();
            SHA1.update(beforeEncryption.getBytes());
            encryptedRaw = SHA1.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }

        return Base64.encodeToString(encryptedRaw, Base64.NO_WRAP);
    }

    private static String getUTCTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-d'T'HH:mm:ss'Z'", Locale.CHINA);
        sdf.setTimeZone(new SimpleTimeZone(2, "UTC"));
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        return sdf.format(cal.getTime());
    }


    public void setWs_UsernameTokenContent(boolean enable) {
        wsUsernameToken = enable;
    }

    protected abstract String getCommand();

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void appendHeaders(String key, String value) {
        headers.put(key, value);
    }

    public String getDigestHeader(String digestHeader) {
        return new OnvifDigestInformation(username, password, path, digestHeader).getAuthorizationHeader();
    }
}

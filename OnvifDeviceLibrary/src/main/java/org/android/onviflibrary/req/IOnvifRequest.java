package org.android.onviflibrary.req;

import java.util.Map;

public interface IOnvifRequest {

    String getUrl();

    String getContent();

    Map<String, String> getHeaders();

    void appendHeaders(String key, String value);

    String getDigestHeader(String digestHeader);

    String getWS_UsernameTokenContent();

}

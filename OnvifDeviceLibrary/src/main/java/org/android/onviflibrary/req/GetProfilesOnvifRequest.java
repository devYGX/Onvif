package org.android.onviflibrary.req;

import org.android.onviflibrary.OnvifRequest;

import java.net.MalformedURLException;

public class GetProfilesOnvifRequest extends OnvifRequest {
    public static final String COMMAND
            = "<GetProfiles xmlns=\"http://www.onvif.org/ver10/media/wsdl\"></GetProfiles>";

    public static final String NAMESPACE
            = "http://www.onvif.org/ver10/media/wsdl";

    public GetProfilesOnvifRequest(String url, String username, String password, boolean wsUsernameToken) throws MalformedURLException {
        super(url, username, password, wsUsernameToken);
    }

    @Override
    protected String getCommand() {
        return COMMAND;
    }
}

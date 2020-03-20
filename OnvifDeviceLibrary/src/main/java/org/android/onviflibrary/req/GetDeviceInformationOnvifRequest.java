package org.android.onviflibrary.req;

import org.android.onviflibrary.OnvifRequest;

import java.net.MalformedURLException;

public class GetDeviceInformationOnvifRequest extends OnvifRequest {
    public static final String COMMAND
            = "<GetDeviceInformation xmlns=\"http://www.onvif.org/ver10/device/wsdl\">" + "</GetDeviceInformation>";

    public static final String NAMESPACE
            = "http://www.onvif.org/ver10/device/wsdl";

    public GetDeviceInformationOnvifRequest(String url, String username, String password, boolean wsUsernameToken) throws MalformedURLException {
        super(url, username, password, wsUsernameToken);
    }

    @Override
    protected String getCommand() {
        return COMMAND;
    }
}

package org.android.onviflibrary.req;

import org.android.onviflibrary.OnvifRequest;

import java.net.MalformedURLException;

public class GetServiceOnvifRequest extends OnvifRequest {
    public static final String COMMAND
            = "<GetServices xmlns=\"http://www.onvif.org/ver10/device/wsdl\">"
            + "<IncludeCapability>false</IncludeCapability>"
            + "</GetServices>";

    public static final String NAMESPACE
            = "http://www.onvif.org/ver10/device/wsdl";

    public GetServiceOnvifRequest(String url) throws MalformedURLException {
        super(url, null, null, false);
    }


    @Override
    protected String getCommand() {
        return COMMAND;
    }
}

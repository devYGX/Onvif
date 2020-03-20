package org.android.onviflibrary.req;

import org.android.onviflibrary.OnvifRequest;

import java.net.MalformedURLException;

public class GetCapabilitiesOnvifRequest extends OnvifRequest {

    /*
      <GetCapabilities xmlns="http://www.onvif.org/ver10/device/wsdl">
      <Category>Media</Category>
    </GetCapabilities>
     */
    private static final String COMMAND = "<GetCapabilities xmlns=\"http://www.onvif.org/ver10/device/wsdl\">" +
            "</GetCapabilities>";
    private static final String PATH = "/";

    public GetCapabilitiesOnvifRequest(String url, String username, String password) throws MalformedURLException {
        super(url, username, password, false);
    }

    @Override
    protected String getCommand() {
        return COMMAND;
    }
}

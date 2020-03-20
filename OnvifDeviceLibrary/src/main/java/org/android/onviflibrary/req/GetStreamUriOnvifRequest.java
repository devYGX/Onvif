package org.android.onviflibrary.req;

import org.android.onviflibrary.OnvifRequest;

import java.net.MalformedURLException;
import java.util.Locale;

public class GetStreamUriOnvifRequest extends OnvifRequest {
    /*public static final String COMMAND
            = "<GetStreamUri xmlns=\"http://www.onvif.org/ver10/media/wsdl\">"
            + "<ProfileToken>"
            + "%s"
            + "</ProfileToken>"
            + "<Protocol>RTSP</Protocol>"
            + "</GetStreamUri>";*/
    // TP-LINK
    public static final String COMMAND
            = "<GetStreamUri xmlns=\"http://www.onvif.org/ver10/media/wsdl\">" +
            "<StreamSetup>" +
            "<Stream xmlns=\"http://www.onvif.org/ver10/schema\">RTP-Unicast</Stream>" +
            "<Transport xmlns=\"http://www.onvif.org/ver10/schema\">" +
            "<Protocol>UDP</Protocol>" +
            "</Transport>" +
            "</StreamSetup>" +
            "<ProfileToken>%s</ProfileToken>" +
            "</GetStreamUri>";

    public static final String NAMESPACE
            = "http://www.onvif.org/ver10/media/wsdl";
    private final String profileToken;

    public GetStreamUriOnvifRequest(String url, String username, String password, boolean wsUsernameToken, String profileToken) throws MalformedURLException {
        super(url, username, password, wsUsernameToken);
        this.profileToken = profileToken;
    }


    @Override
    protected String getCommand() {
        return String.format(Locale.CHINA, COMMAND, profileToken);
    }
}

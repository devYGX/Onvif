package org.android.onviflibrary.requester;

import org.android.onviflibrary.IHttpRequester;
import org.android.onviflibrary.OnvifRequest;
import org.android.onviflibrary.OnvifResponse;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class DefaultOnvifRequester implements IHttpRequester<OnvifRequest, OnvifResponse> {
    private static final String TAG = "DefaultOnvifRequester";

    @Override
    public void syncStart() {

    }

    @Override
    public OnvifResponse accept(OnvifRequest req) throws Exception {
        OnvifResponse response = new OnvifResponse();
        response.setRequest(req);
        HttpURLConnection urlConnection = null;
        InputStream is = null;
        try {
            URL url = new URL(req.getUrl());
            final HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            urlConnection = httpURLConnection;

            for (Map.Entry<String, String> entry : req.getHeaders().entrySet()) {
                httpURLConnection.setRequestProperty(entry.getKey(), entry.getValue());
            }
            httpURLConnection.setRequestProperty("Connection", "close");
            httpURLConnection.setRequestProperty("Accept-Encoding", "none");
            httpURLConnection.setConnectTimeout(30 * 1000);
            httpURLConnection.setReadTimeout(30 * 1000);
            httpURLConnection.setRequestMethod("POST");

            OutputStream os = httpURLConnection.getOutputStream();
            os.write(req.getContent().getBytes());
            os.flush();
            int responseCode = -1;
            responseCode = httpURLConnection.getResponseCode();
            response.setSuccess(responseCode == 200);
            response.setCode(responseCode);
            response.setResponseMessage(httpURLConnection.getResponseMessage());
            Map<String, List<String>> headerFields = httpURLConnection.getHeaderFields();
            for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
                List<String> value = entry.getValue();
                if (value == null || value.size() == 0) continue;
                if (value.size() == 1)
                    response.putHeader(entry.getKey(), value.get(0));
                else {
                    StringBuilder sBuilder = new StringBuilder();
                    int size = value.size();
                    sBuilder.append("[");
                    for (int i = 0; i < size; i++) {
                        if (i > 0) sBuilder.append(", ");
                        sBuilder.append(value.get(i));
                    }
                    sBuilder.append("]");
                    response.putHeader(entry.getKey(), String.valueOf(value));
                }
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (responseCode != 200) {
                is = httpURLConnection.getErrorStream();
                byte[] buf = new byte[1024];
                int len = -1;
                while ((len = is.read(buf)) != -1) {
                    baos.write(buf, 0, len);
                    baos.flush();
                }
                response.setErrorMessage(baos.toString());
                return response;
            }

            is = httpURLConnection.getInputStream();
            byte[] buf = new byte[1024];
            int len = -1;
            while ((len = is.read(buf)) != -1) {
                baos.write(buf, 0, len);
                baos.flush();
            }
            response.setMessage(baos.toString());
            return response;
        } finally {
            if (urlConnection != null) {
                try {
                    urlConnection.disconnect();
                } catch (Exception ignored) {
                }
            }
        }

    }

    @Override
    public void syncStop() {

    }
}

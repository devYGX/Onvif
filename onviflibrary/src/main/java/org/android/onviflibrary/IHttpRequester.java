package org.android.onviflibrary;

public interface IHttpRequester<Req, Resp> {

    void syncStart();

    Resp accept(Req req)  throws Exception;

    void syncStop();
}

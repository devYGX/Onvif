package org.android.onviflibrary.finder;

import android.os.Handler;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author YGX
 * @see OnvifFinder
 */
public class OnvifFinder {
    /*
    <?xml version="1.0" encoding="utf-8"?>

    <Envelope xmlns="http://www.w3.org/2003/05/soap-envelope" xmlns:tds="http://www.onvif.org/ver10/device/wsdl">
      <Header>
        <wsa:MessageID xmlns:wsa="http://schemas.xmlsoap.org/ws/2004/08/addressing">uuid:84d61264-fad6-469e-a418-99509d3e172c</wsa:MessageID>
        <wsa:To xmlns:wsa="http://schemas.xmlsoap.org/ws/2004/08/addressing">urn:schemas-xmlsoap-org:ws:2005:04:discovery</wsa:To>
        <wsa:Action xmlns:wsa="http://schemas.xmlsoap.org/ws/2004/08/addressing">http://schemas.xmlsoap.org/ws/2005/04/discovery/Probe</wsa:Action>
      </Header>
      <Body>
        <Probe xmlns="http://schemas.xmlsoap.org/ws/2005/04/discovery" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema">
          <Types>tds:Device</Types>
          <Scopes/>
        </Probe>
      </Body>
    </Envelope>
     */
    private static final String ONVIF_FINDER_CONTENT =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<Envelope xmlns:tds=\"http://www.onvif.org/ver10/device/wsdl\" xmlns=\"http://www.w3.org/2003/05/soap-envelope\">" +
                    "<Header><wsa:MessageID xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\">uuid:84d61264-fad6-469e-a418-99509d3e172c</wsa:MessageID><wsa:To xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\">urn:schemas-xmlsoap-org:ws:2005:04:discovery</wsa:To><wsa:Action xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\">http://schemas.xmlsoap.org/ws/2005/04/discovery/Probe</wsa:Action></Header>" +
                    "<Body><Probe xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns=\"http://schemas.xmlsoap.org/ws/2005/04/discovery\"><Types>tds:Device</Types><Scopes /></Probe></Body>" +
                    "</Envelope>";


    private static final String TAG = "OnvifFinder";
    private static final long DEFAULT_TIMEOUT = 20 * 1000;
    private final String target;
    private final long timeoutMillis;
    private int count;
    private static final int DEFAULT_FINDER_PORT = 3702;
    private static final String DEFAULT_TARGET = "239.255.255.250";

    public OnvifFinder(String target, int count, long timeoutMillis) {
        this.target = target;
        this.timeoutMillis = timeoutMillis;
        this.count = count;
    }

    public OnvifFinder(String target, int count) {
        this(target, count, DEFAULT_TIMEOUT);
    }

    public OnvifFinder(String target) {
        this(target, Integer.MAX_VALUE, DEFAULT_TIMEOUT);
    }

    public OnvifFinder() {
        this(DEFAULT_TARGET, Integer.MAX_VALUE, DEFAULT_TIMEOUT);
    }

    @WorkerThread
    public void find(OnvifFinderCallback finderCallback) {
        find(null, null, finderCallback);
    }

    @WorkerThread
    public void find(List<OnvifDiscoverer> discovererList, OnvifFinderCallback finderCallback) {
        try {
            doFind(discovererList, null, finderCallback);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @WorkerThread
    public void find(List<OnvifDiscoverer> discovererList, Handler subscribeHandler, OnvifFinderCallback finderCallback) {
        try {
            doFind(discovererList, subscribeHandler, finderCallback);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @WorkerThread
    public void find(Handler subscribeHandler, OnvifFinderCallback finderCallback) {
        try {
            doFind(null, subscribeHandler, finderCallback);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private List<OnvifDiscoverer> doFind(List<OnvifDiscoverer> discovererList,
                                         Handler subscribeHandler,
                                         OnvifFinderCallback finderCallback) throws IOException {
        if (finderCallback != null) {
            if (subscribeHandler != null) {
                subscribeHandler.post(finderCallback::onFindStart);
            } else {
                finderCallback.onFindStart();
            }
        }
        if (discovererList == null) {
            discovererList = new ArrayList<>();
        }
        final List<OnvifDiscoverer> onvifDiscovererList = discovererList;
        DatagramChannel datagramChannel = null;
        Selector selector = null;
        try {
            datagramChannel = DatagramChannel.open();
            datagramChannel.configureBlocking(false);
            // 可以不用绑定端口为3702,
            // datagramChannel.socket().bind(new InetSocketAddress(3702));

            selector = Selector.open();
            datagramChannel.register(selector, SelectionKey.OP_READ);
            ByteBuffer recvBuffer = ByteBuffer.allocate(8192);
            long start = System.currentTimeMillis();
            datagramChannel.send(
                    ByteBuffer.wrap(ONVIF_FINDER_CONTENT.getBytes()),
                    new InetSocketAddress(target, DEFAULT_FINDER_PORT));
            read:
            while (start + timeoutMillis >= System.currentTimeMillis()) {
                int select = selector.select(100);
                if (select <= 0) {
                    continue;
                }

                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();
                    if (selectionKey.isReadable()) {
                        iterator.remove();
                        DatagramChannel recvDatagramChannel = (DatagramChannel) selectionKey.channel();
                        recvBuffer.clear();
                        InetSocketAddress address = (InetSocketAddress) recvDatagramChannel.receive(recvBuffer);
                        recvBuffer.flip();
                        CharBuffer charBuffer = Charset.defaultCharset().decode(recvBuffer);
                        OnvifDiscoverer onvifDiscoverer = decodeOnvifDiscoverer(address.getHostName(), charBuffer.toString());
                        if (onvifDiscoverer == null) {
                            continue;
                        }
                        onvifDiscovererList.add(onvifDiscoverer);
                        if (finderCallback != null) {
                            if (subscribeHandler != null) {
                                subscribeHandler.post(
                                        () -> finderCallback.onFindDiscoverer(onvifDiscoverer, onvifDiscovererList));
                            } else {
                                finderCallback.onFindDiscoverer(onvifDiscoverer, onvifDiscovererList);
                            }
                        }
                        if (onvifDiscovererList.size() >= count) {
                            break read;
                        }
                    }
                }
            }
            if (finderCallback != null) {
                finderCallback.onFindEnd(onvifDiscovererList);
            }
            return onvifDiscovererList;
        } catch (Exception e) {
            if (finderCallback != null) {
                finderCallback.onFindThrowable(e);
            }
            throw e;
        } finally {

            if (datagramChannel != null) {
                try {
                    datagramChannel.disconnect();
                } catch (IOException ignored) {
                }
                try {
                    datagramChannel.close();
                } catch (IOException ignored) {
                }
            }

            if (selector != null) {
                try {
                    selector.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    @WorkerThread
    public List<OnvifDiscoverer> find(List<OnvifDiscoverer> discovererList) throws Exception {
        find(discovererList, null);
        return discovererList;
    }

    @WorkerThread
    public List<OnvifDiscoverer> find() throws Exception {
        return doFind(null, null, null);
    }


    /**
     * <wsa:EndpointReference>
     * <wsa:Address>urn:uuid:00110300-6afb-8301-ac36-001103006afb</wsa:Address>
     * </wsa:EndpointReference>
     * <d:Types>dn:NetworkVideoTransmitter</d:Types>
     * <d:Scopes>onvif://www.onvif.org/type/video_encoder onvif://www.onvif.org/type/audio_encoder onvif://www.onvif.org/type/ptz onvif://www.onvif.org/hardware/HW0100302 onvif://www.onvif.org/location/country onvif://www.onvif.org/name/IPC1000 onvif://www.onvif.org/Profile/Streaming</d:Scopes>
     * <d:XAddrs>http://192.168.1.87:2000/onvif/device_service</d:XAddrs>
     *
     * @param xmlString
     */
    private OnvifDiscoverer decodeOnvifDiscoverer(String inetHost, String xmlString) {
        String uuid = null;
        String address = null;
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xmlPullParser = factory.newPullParser();
            xmlPullParser.setInput(new StringReader(xmlString));
            int eventType = xmlPullParser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType != XmlPullParser.START_TAG) {
                    eventType = xmlPullParser.next();
                    continue;
                }
                String tagName = xmlPullParser.getName();
                // <wsa:Address>urn:uuid:00110300-6afb-8301-ac36-001103006afb</wsa:Address>
                if ("Address".equals(tagName)) {
                    xmlPullParser.next();
                    uuid = xmlPullParser.getText().trim();
                }
                //  <d:XAddrs>http://192.168.1.87:2000/onvif/device_service</d:XAddrs>
                else if ("XAddrs".equals(tagName)) {
                    xmlPullParser.next();
                    address = xmlPullParser.getText().trim();
                } else {
                    eventType = xmlPullParser.next();
                    continue;
                }
                if (!TextUtils.isEmpty(uuid) && !TextUtils.isEmpty(address)) {
                    return new OnvifDiscoverer(inetHost, uuid, address);
                }
                eventType = xmlPullParser.next();

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}

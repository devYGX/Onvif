Onvif
======

创建时间 | 2019-08-26 | |
--- | --- | ---
版本号 | 1.0.0
修改时间 | 修改内容
2018-08-26 | 添加:<br/>[OnvifLibrary](#OnvifLibrary)

## <span id="目录">目录</span>
=======

* [OnvifLibrary](#OnvifLibrary)
	* [说明](#OnvifLibrary说明)
	* [搜索当前网段IPC设备](#OnvifFinder_API)
		* [调用API](#调用API)
		* [OnvifDiscoverer类](#OnvifDiscoverer_API)
		* [OnvifFinder类](#OnvifFinder)
	* [登录IPC并获取设备信息](#OnvifDevice_API)
	* [测试结果](#测试结果)

=======


### <span id="OnvifLibrary">OnvifLibrary</span>

#### <span id="OnvifLibrary说明">说明</span>

提供给Android端使用的Onvif协议的类库, 用于搜索当前网段内IPC设备; 连接Onvif IPC, 获取设备信息, RTSP流地址等信息


[返回目录](#目录)

#### <span id="OnvifFinder_API">搜索当前网段IPC设备</span>

##### <span id="调用API">调用API</span>

使用默认组播地址搜索当前网段内的IPC设备

	// 创建一个使用默认参数的Onvif Finder
	OnvifFinder onvifFinder = new OnvifFinder();

	try{
		// 查找设备, 直接返回结果; find需要在非UI线程调用
		List<OnvifDiscoverer> onvifDiscovererList = onvifFinder.find();
	}catch(Exception e){
		// 参数错误, 或者IO错误等等
	}

	// 查找设备, 通过回调返回结果; find需要在非UI线程调用
	onvifFinder.find(new OnvifFinderCallback() {
			/**
			 * 开始查找设备时
			 */
            @Override public void onFindStart() {}

			/**
			 * 找到一个设备时;
			 */
            @Override public void onFindDiscoverer(OnvifDiscoverer discoverer, List<OnvifDiscoverer> discovererList) {}

			/**
			 * 查找设备出错; onFindEnd和onFindThrowable只会调用一个, 表示查询的结束
			 */
            @Override public void onFindThrowable(Throwable t) {}

			/**
			 * 查找设备结束; onFindEnd和onFindThrowable只会调用一个, 表示查询的结束
			 * 这里的discovererList不会为null, 但是它的size可能会为0
			 */
            @Override public void onFindEnd(List<OnvifDiscoverer> discovererList) {}
        });

[返回目录](#目录)

##### <span id="OnvifDiscoverer_API">OnvifDiscoverer类说明</span>

OnvifDiscoverer说明

	public class OnvifDiscoverer {

	    private String host;		// IPC的ip地址
	    private String uuid;		// IPC的UUID
	    private String address;		// IPC的device_service地址
	
	    public OnvifDiscoverer(String host,String uuid, String address) {
	        this.uuid = uuid;
	        this.address = address;
	        this.host = host;
	    }
		...
	}


[返回目录](#目录)

##### <span id="OnvifFinder_API">OnvifFinder类说明</span>

OnvifFinder是一个Onvif IPC设备的寻找器, 它重载了多个构造方法和find方法来允许用户自定义自己的查询方式

构造器参数</br>
参数 | 参数类型 | 参数说明
target | String | 目标地址;<br/> 默认是``239.255.255.250``这个组播地址;<br/> 也可以传指定的IP地址;
count | int | 获取的设备个数;<br/> 默认是Integer.MAX_VALUE, 表示获取越多越好;</br> 当获取到的IPC设备个数大于等于count后, 就算还没达到timeoutMillis, find()方法也会立即结束执行;
returnMillis | long | 返回时间, 表示阻塞获取IPC设备多少毫秒后, 结束获取并返回获取到的设备列表; 默认值是20 * 000, 20秒;

find方法

	// 1. 同步获取, 如果获取过程发生异常, 则会抛出异常
    List<OnvifDiscoverer> onvifDiscovererList = onvifFinder.find();

	// 2. 同步获取, 参可以传入一个集合进行进行保存;
    List<OnvifDiscoverer> reusableList = new ArrayList<>();
    // onvifDiscovererList就是reusableList
    List<OnvifDiscoverer> onvifDiscovererList = onvifFinder.find(reusableList);

	// 3. 通过回调获取
	onvifFinder.find(new OnvifFinderCallback() {

            @Override public void onFindStart() {}

            @Override public void onFindDiscoverer(OnvifDiscoverer discoverer, List<OnvifDiscoverer> discovererList) {}

            @Override public void onFindThrowable(Throwable t) {}

            @Override public void onFindEnd(List<OnvifDiscoverer> discovererList) {}
        });

	// 4. 通过回调获取, 并且指定了回调的Handler
	Handler subscribeHandler = new Handler(Looper.getMainLooper());
    onvifFinder.find(subscribeHandler, new OnvifFinderCallback() {...});

	// 5. 通过回调获取, 复用集合;
    onvifFinder.find(reusableList, new OnvifFinderCallback(){ ... });

	// 6. 通过回调获取, 复用集合, 指定回调的Handler
    onvifFinder.find(reusableList, new OnvifFinderCallback(){ ... });

	// 复用集合, 指定回调的线程,
    onvifFinder.find(reusableList, subscribeHandler, new OnvifFinderCallback(){ ... });

[返回目录](#目录)

#### <span id="OnvifDevice_API">登录IPC并获取设备信息</span>

登录IPC

	// String host     = "your ipc host";
	// String username = "your ipc username";
	// String password = "your ipc password";
	try{
		OnvifDevice device = new OnvifDevice.Builder()
                 .host(host)
                 .username(username)
                 .password(password)
                 .login();
	}catch(Exception e){
		
		if(e instanceof AuthenticateException){
			// 用户名或密码验证错误
		}

		else if(e instanceof ParseException){
			// 解析报文出错; 有可能是摄像头不支持标准的Onvif协议, 建议使用Onvif Device Test Tools工具测试摄像头
		}

		else{
			// 其他错误; 建议检查网络, 以及使用Onvif Device Test Tools工具测试摄像头
		}
	}

登录成功

	// 登录成功后可以获取IPC的设备信息(有些厂商的IPC, 以下值可能为空), 如下: 以TP-LINK这款IPC设备为例, 值如下;
	String fwVersion = device.getFwVersion();				// 1.0.4 Build 180713 Rel.69411n
	String hwID = device.getHwID();							// 1.0
	String manufacturerName = device.getManufacturerName(); // TP-Link
	String modelName = device.getModelName();				// TL-IPC525K(P)-WD4
	String serialNumber = device.getSerialNumber();			// 12344xxx

	// 获取IPC的mediaProfile
	List<MediaProfile> mediaProfiles = device.getMediaProfiles();
    for (MediaProfile mediaProfile : mediaProfiles) {
		// 获取rtsp url; 例如: rtsp://192.168.1.86:554/stream1
        String mediaStreamUri = mediaProfile.getMediaStreamUri();

		// 获取携带username和password的rtsp url, 例如:
		// rtsp://admin:password@192.168.1.86:554/stream1
		// 有些网络摄像头需要rtsp中携带用户名和密码才可以拉取流, 例如海康位置, TP-LINK的两款摄像头
        String onvifMediaStreamUri = mediaProfile.getOnvifMediaStreamUri();

        MediaProfile.VideoEncoderConfiguration videoEncoderConfiguration = mediaProfile.getVideoEncoderConfiguration();
		// 获取视频编码, H264或H265
        String enoding = videoEncoderConfiguration.getEnoding();
        MediaProfile.VideoResolution resolution = videoEncoderConfiguration.getResolution();
		
		// 获取视频的分辨率;
		// 需要注意的是这里获取的分辨率也不一定准确; 例如华安摄像头, 获取到分辨率是1280*720, 实际拉取到的流分辨率却是:1920*1080
        int width = resolution.getWidth();
        int height = resolution.getHeight();
        MediaProfile.VideoRateControl videoRateControl = videoEncoderConfiguration.getVideoRateControl();
		// 获取比特率
        int bitrateLimit = videoRateControl.getBitrateLimit();
        int encodingInterval = videoRateControl.getEncodingInterval();
		// 获取帧率
        int frameRateLimit = videoRateControl.getFrameRateLimit();
    }


[返回目录](#目录)

#### <span id="测试结果">测试结果</span>

获取Onvif设备时需要兼容三个问题(暂时只发现这三个):<br/>

1. 绝大部分IPC使用的device_service url是:http://ip:80/onvif/device_service, 也有的不是;
2. 有一些摄像头不必输入正确的用户名和密码, 也可以获取到设备信息, rtsp视频流地址; 有一些又必须要输入正确的用户名和密码;
3. 在使用RTSP视频流地址时, 有些摄像头不必携带用户名和密码也能拉取到视频流; 有些则必须携带用户名和密码;

针对以上三个问题, OnvifLibrary已经给出了解决方案:<br/>

1. 先尝试使用 http://ip:80/onvif/device_service 去登录摄像头; 如果登录失败; 再通过OnvifFinder去获取摄像头的device service url(OnvifDisconverer#address), 目前已知可以兼容6款摄像头;
2. 先尝试使用无用户名和密码登录; 根据IPC的响应结果选择是否使用用户名和密码登录; 目前已兼容6款摄像头
3. 建议在拉流的时候采用格式: rtsp://{usn}:{psw}@{rtspurl}, 例如: rtsp://admin:password@192.168.1.86:554/stream1


以下是测试的6款摄像头及兼容情况<br/>

摄像头型号  | 密码和用户名是否必须正确 
华安  | 否
火力牛  | 否
大华  | 否
海康威视 | 是
TP-LINK | 是 
Network Degital Camera  | 否

[返回目录](#目录)

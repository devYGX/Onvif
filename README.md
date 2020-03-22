Onvif
======

创建时间 | 2019-08-26 | 
--- | --- 
版本号 | 1.0.1
修改时间 | 修改内容
2018-08-26 | 添加:<br/>[OnvifDeviceLibrary](#OnvifDeviceLibrary)

## <span id="目录">目录</span>
=======

* [OnvifDeviceLibrary](#)
	* [使用说明](#OnvifDeviceLibrary使用说明)
	* [详细说明](OnvifDeviceLibrary/README.md)

* [OnvifPlayerLibrary][#OnvifPlayerLibrary]
    * [使用说明](#OnvifPlayerLibrary使用说明)
=======


### <span id="OnvifDeviceLibrary使用说明">OnvifDeviceLibrary使用说明</span>

通过抓包分析Onvif Device Test Tool软件分析协议后编写出的，提供给安卓端使用的查询同网段内网路摄像头， 根据输入的IP, 用户名， 密码登陆网络摄像头信息的类库

* 查找当前网段内的网络摄像头
        

    // 耗时操作，需要放在子线程
    List<OnvifDiscoverer> onvifDiscoverers = new OnvifFinder().find();

* 登陆指定IP, 用户名，密码的网络摄像头


    OnvifDevice onvifDevice = new OnvifDevice.Builder()
                    .host(host)
                    .username(username)
                    .password(password)
                    .login();

### <span id="OnvifPlayerLibrary使用说明">OnvifPlayerLibrary使用说明</span>


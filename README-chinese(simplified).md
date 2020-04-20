[English](https://github.com/linxot/dhub-plugin-demo/blob/master/README-english.md)
[繁体](https://github.com/linxot/dhub-plugin-demo/blob/master/README-chinese(traditional).md)

[TOC]

## 前言
    1.指令下发需要收到对应的ACK才能结束指令。
    2.没有指令要下发时，需要下发一个无事指令，保证后台收到设备上报的数据和使设备更快进去休眠模式。
    3.设备有数据上报时，才会打开指令接收窗口，这时才能下发指令
    4.在resources路径下可以修改设备密钥[secret.properties]
      默认密钥:appkey=xxxxx


## 一、导入maven引用
     
```java
    <dependency>
        <groupId>com.linxot.dhub.plugin</groupId>
        <artifactId>com.linxot.dhub.plugin.codec</artifactId>
        <version>1.0-SNAPSHOT</version>
        <scope>system</scope>
        <systemPath>${jar.dic}/libs/com.linxot.dhub.plugin.codec-1.0-SNAPSHOT.jar</systemPath>
    </dependency>
    <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>fastjson</artifactId>
            <version>1.2.67</version>
    </dependency>
```

## 二、使用说明


### 1、上行数据
#### ---  解密调用：
> 对设备上报的数据payload进制解密、解码，获取到JSONObject（三、指令和报文对应表 ）：

```java
PayLoadDataDecode payLoadDataDecode = new PayLoadDataDecode(deviceDomain.getDevEui());

//payload 数据解密
JSONObject result = payLoadDataDecode.decode(body);
```

方法解释：


```java
 public PayLoadDataDecode(String devEui);
 
 public JSONObject decode(String value);
```

参数 | 描述
---|---
value | 要解密的数据
devEui | 设备的devEUI

#### --- 示例场景：
1. oneNET平台推送过来的数据如下：
   
```json
{
    "msg":{
        "at":1551052412074,
        "imei":"0000000",
        "type":1,
        "ds_id":"3300_0_5750",
        "value":"c0ced68f09234e9e4581fdfa1edab74f10ad9e4c",
        "dev_id":111111
    },
    "msg_signature":"xxxx==",
    "nonce":"xxx"
}
```

2.  获取msg.value的值，调用decode（）方法
   

```java
payLoadDataDecode.decode(msg.value);
```
3. 即可获取json格式的数据

```
{
    "records":[
        {
            "temperature":248,
            "humidity":749,
            "time":1555407784
        }
    ],
    "state":0,
    "type":"recordPoint",
    "deviceId":"1540352201318927",
    "platform":"linxot"
}
```


###  2、下行指令
#### --- 编码调用
>  应用收到指令之后，需要将它进行编码转换后，还需要编码后的数据进行加密（加密步骤下下面），设备才能识别该指令




```java
    //创建job
    Command command = new Command();
        //报文类型
        command.setCmdType("settingParam");
        //JSONObject 下面描述
        command.setItem(JSONObject);
        //参数类型
        command.setRange("item");
    //声明编码对象
    PayLoadDataEncode payLoadDataEncode = new PayLoadDataEncode();
    //编码操作
    EncodeResult encodeResult = payLoadDataEncode.encode(command);
```

    
    

---

Command （发送指令对象）

参数  |类型|是否必填| 描述
---|---|---|---
cmdType | string|是|指令类型
range | string |是| 参数类型
queueId | string |否| 指令ID
platform | string |否| 厂商简称
item | JSONObject |是| 参数对象（详情看协议文档）


---

EncodeResult （编码后指令数据格式）

参数  |类型|是否必填| 描述
---|---|---|---
deviceId | string| 否|设备Id
platform | string| 否|厂商简称
body | byte[]| 是|编码后的数据
payload | string| 是|编码后的16进制数据
ack | string| 是|对应指令的ack

#### --- 加密调用
>     成功编码后会得定一个byte数组或16进制字符串，以下算法对编码后的指令数据进行加密


```java
//初始化
 PayLoadDataEncode payLoadDataEncode = new PayLoadDataEncode();
 String payload = payLoadDataEncode.payloadEncrypt(cmd,
                deviceDomain.getDevEui(),
                seq,
                1);
```
方法解释：

```java
public String payloadEncrypt(String value, String devEui, int seq, int protocol) ;
```



参数 | 描述
---|---
value | 要加密的数据
devEui | 设备的devEui
seq | 报文counter，报文数的累计值,（每次向设备下发指令都要将这个counter+1）
protocol | 报文版本，如未特别说明默认：1

#### --- 示例场景：
下行指令会分开两步实现：
>  1.将下发指令进行编码后存储到消息队列或缓存。
    
    
```
//要下发下发告警规则 
{
    "deviceId":"xxxxx",
    "data":[
        {
            "userId":0,
            "temperatureMax":26,
            "temperatureMin":20,
            "humidityMax":64,
            "humidityMin":60
        }
    ]
}


```

```java
//创建job
Command command = new Command();
//报文类型
command.setCmdType("settingThreshold");
//JSONObject 上面的josn数据
command.setItem(JSONObject);
//参数类型
command.setRange("item");
//声明编码对象
PayLoadDataEncode payLoadDataEncode = new PayLoadDataEncode();
//编码操作
EncodeResult encodeResult = payLoadDataEncode.encode(command);

...
//将encodeResult.payload或payload.body保存到消息队列或缓存

```

> 2.应用收到设备的数据后，在设备接收数据的窗口内，将数据加密后下发给设备。

 

```java
//取encodeResult.payload或payload.body的16进制,进行加密
 String payload = payLoadDataEncode.payloadEncrypt(encodeResult.payload,
                deviceDomain.getDevEui(),
                seq,
                1);

//调用运营商平台下发指令的API， 例如oneNET http://API_ADDRESS/nbiot/execute
//请求示例:
{
    "args": payload
}
```

    
## 三、指令和报文对应表 
###   下行指令(Command)
####     1、cmdType="settingThreshold"：设置告警规则
    item 示例：
    [
               { 
               "userId":0,
                "temperature_max":260,
                "temperature_min":200,
                "humidity_max":640,
                "humidity_min":600
               }
               
    ]
    

参数 | 类型|是否必填|描述
---|---|---|---
userId  | long|是|这里默认：0
temperature_max | int|是|温度下限。取值范围：-450 ~ 1250，32767-表示该值不设置
temperature_min | int|是|温度下限。取值范围：-450 ~ 1250，32767-表示该值不设置
humidity_max | int|是|温度下限。取值范围：-450 ~ 1250，32767-表示该值不设置
humidity_min | int|是|温度下限。取值范围：-450 ~ 1250，32767-表示该值不设置

####     2、cmdType="settingParam"：设置设备参数
    item 示例：
    [
               {
                "deviceId":"1586859946579722",
                "recordPointTime":570,
                "temperatureThreshold":30,
                "humidityThreshold":30,
                "timeThreshold":30
            }
               
    ]
    

参数 | 类型|是否必填|描述
---|---|---|---
recordPointTime | int|是|历史数据上报间隔。单位：分，默认：9.5小时
timeThreshold | int|是|间隔如果数据是0，就是默认的门限 来保存数据，如果是1-60 就是1-60分钟保存一个数据。
temperatureThreshold | int|是|温度记录门限。单位：±0.1℃，默认：±0.5℃
humidityThreshold | int|是|湿度记录门限。单位：±0.1%RH，默认：±3%
    
    
####     3、cmdType="queryRecordPoint"：查询历史数据
    item 示例：
    [
            {
                "deviceId":"xxxxxxxx"
            }
    ]

参数 | 类型|是否必填|描述
---|---|---|---
deviceId  | string|是|设备ID

    
####     4、cmdType="queryParam"：查询设备参数
    item 示例：
    [
    {
        "deviceId":"1529993764014110",
        "param":[
            "recordPointTime",
            "weatherTime",
            "temperatureThreshold",
            "humidityThreshold"
        ]
    }
    ]

参数 | 类型|是否必填|描述
---|---|---|---
deviceId  | string|是|设备ID
deviceId  | list|是|要查询的参数：<br>recordPointTime=历史数据上报间隔<br>weatherTime=天气预报的请求间隔<br>temperatureThreshold=温度采集阀值<br>humidityThreshold=湿度采集阀值
    
    
####     5、cmdType="queryInstallation"：查询安装状态
    item 示例：
    [
            {
                "deviceId":"xxxxxxxx"
            }
    ]

参数 | 类型|是否必填|描述
---|---|---|---
deviceId  | string|是|设备ID


####     6、cmdType="queryStatus"：查询状态
    item 示例：
    [
            {
                "deviceId":"xxxxxxxx"
            }
    ]

参数 | 类型|是否必填|描述
---|---|---|---
deviceId  | string|是|设备ID

####     7、cmdType="downLinkFF"：无事报文
    item 示例：
    [
            {
                "deviceId":"xxxxxxxx"
            }
    ]

参数 | 类型|是否必填|描述
---|---|---|---
deviceId  | string|是|设备ID


###   上行报文
####     1、81-心跳报文


```
{
    "changeAppkeyWarn":false,
    "sensorWarn":false,
    "dateFormat":"2020-04-13 09:54:49",
    "installWarn":true,
    "eepromWriteWarn":false,
    "ipUpdateWarn":false,
    "type":"status",
    "deviceId":"1003555153489448312",
    "platform":"linxot",
    "voltage":3650,
    "recordTime":1586742889,
    "creatTime":"2020-04-13 09:54:51.156",
    "temperature":247,
    "humidity":306,
    "lbsItems":[
        {
            "mnc":11,
            "mcc":460,
            "lac":518,
            "cid":4912979
        }
    ],
    "lbs":"46011,0206,4AF753",
    "signal":20,
    "protocolVersion":1,
    "timestamp":1586771689,
    "alarms":[{
        "userId":0,
        "alarmSize":1,
        "alarmMsg":[{
            "alarmCode":1,
            "alarmRecordTime":100000,
            "alarmTemperature":243,
            "alarmHumidity":601
        }]
    }]
}
```
参数 | 类型|是否必填|描述
---|---|---|---
deviceId  | string|是|设备ID
type  | string|是|报文类型 这里固定-"status"
sensorWarn  | boolean|是|传感器感应告警
dateFormat  | string|是|CST时间
recordTime  | string|是|CST时间
installWarn  | string|是|安装感应告警
voltage  | string|是|电池电压，单位：0.1V  （协议版本号1的2650+val*1000/255；单位 mV）
temperature  | string|是|当前温度。单位：0.1℃，取值范围：-450 ~ 1250
humidity  | string|是|当前湿度。单位：0.1RH, 取值范围：0 ~ 1000
signal  | string|是|信号强度。99-无效，参见NB模组定义
timestamp  | string|是|状态时戳（报文生成时刻）
cid  | long|否|基站Id
lac|long|否|基站信息
alarms|list|否|告警信息
alarmSize|int|否|告警数
userId|long|否|用户ID
alarmMsg|list|否|告警详情
alarmCode|int|否|告警类型 <br> 	0 —— 温度超上限告警产生;<br>	1 —— 温度超上限告警恢复;<br> 2 —— 温度超下限告警产生;<br> 3 —— 温度超下限告警恢复;<br> 4 —— 湿度超上限告警产生;<br> 5 —— 湿度超上限告警恢复;<br> 6 —— 湿度超下限告警产生;<br> 7 —— 湿度超下限告警恢复;
alarmRecordTime|long|否|告警时间
alarmTemperature|int|否|告警温度
alarmHumidity|int|否|告警湿度
protocolVersion  | int|是|协议版本



####     2、83-settingThreshold的ACK报文

```
{
    "type":"settingThreshold",
    "deviceId":"1003555153489448312",
    "platform":"linxot",
    "protocolVersion":1,
    "status":0
}
```

参数 | 类型|是否必填|描述
---|---|---|---
deviceId  | string|是|设备ID
type  | string|是|报文类型 这里固定-"settingThreshold"
status  | int|是|0-成功，其它-错误码
protocolVersion  | int|是|协议版本



####     3、85-recordPoint的历史数据报文

```
{
    "type":"recordPoint",
    "deviceId":"1003555153489448312",
    "platform":"linxot",
    "status":0,
    "protocolVersion":1,
    "records":[
        {
            "recordTime":1586716814,
            "temperature":-130,
            "humidity":875,
            "time":1586745614
        }
    ]
}
```

参数 | 类型|是否必填|描述
---|---|---|---
deviceId  | string|是|设备ID
type  | string|是|报文类型 这里固定-"recordPoint"
status  | int|是|后续数据标识<br>用于历史数据很多需要多次上报的情况：<br>0-后续没有数据需要上报。<br>1-后续还有数据需要上报
records  | list|是|温湿度历史数据
recordTime  | long|是|该组温湿度记录的时间
temperature  | int|是|温度 单位:0.1℃，取值范围：-450 ~ 1250
humidity  | int|是|湿度  单位:0.1RH, 取值范围：0 ~ 1000
protocolVersion  | int|是|协议版本

*备注：
如果status=1时，平台需要下发查询历史数据报文（queryRecordPoint）；直到status=0；*
---

####     4、86-installation的ACK报文

```
{
    "type":"installation",
    "deviceId":"1003555153489448312",
    "platform":"linxot",
    "state":0,
    "sensorState":0,
    "protocolVersion":1,
    "recordTime":1000000
}
```

参数 | 类型|是否必填|描述
---|---|---|---
deviceId  | string|是|设备ID
type  | string|是|报文类型 这里固定-"installation"
state  | int|是|安装状态：0-入位，1-脱离 
sensorState  | int|否|传感器感应情况： 0-无感应，1-有感应
recordTime  | long|是|状态时戳（报文生成时刻）
protocolVersion  | int|是|协议版本

####     5、C6-settingInterval的ACK报文

```
{
    "type":"settingInterval",
    "deviceId":"1003555153489448312",
    "platform":"linxot",
    "protocolVersion":1,
    "status":0
}
```

参数 | 类型|是否必填|描述
---|---|---|---
deviceId  | string|是|设备ID
type  | string|是|报文类型 这里固定-"settingInterval"
status  | int|是|0-成功，其它-错误码 
protocolVersion  | int|是|协议版本

####     6、C9-queryNbDetails的ACK报文

```
{
    "type":"queryNbDetails",
    "deviceId":"1003555153489448312",
    "platform":"linxot",
    "imei":"44444",
    "simNum":"55555",
    "version":"11111",
    "protocolVersion":1,
    "imsi":"8888"
}
```

参数 | 类型|是否必填|描述
---|---|---|---
deviceId  | string|是|设备ID
type  | string|是|报文类型 这里固定-"queryNbDetails"
imei  | string|是|设备imei号
simNum  | string|是|SIM卡号 
version  | string|是|模组版本信息
imsi  | string|是|imsi号
protocolVersion  | int|是|协议版本

####     7、CA-queryNbSignal的ACK报文

```
{
    "type":"queryNbSignal",
    "deviceId":"1003555153489448312",
    "platform":"linxot",
    "signalValue":17,
    "ber":55,
    "protocolVersion":1,
    "sinr":11
}
```

参数 | 类型|是否必填|描述
---|---|---|---
deviceId  | string|是|设备ID
type  | string|是|报文类型 这里固定-"queryNbSignal"
signalValue  | int|是|0- 信号强度小于等于-113dBm；<br> 1- 信号强度为 -111dBm ；<br>2~30 - 信号强度为-109至-53dBm；<br>31- 信号强度大于等于-51dBm ；<br>99 Not known or not detectable
ber  | int|是|0-7 99
sinr  | int|是|-127-无效，其它值有效
protocolVersion  | int|是|协议版本

####     8、CD-queryFirmwareVersion的ACK报文

```
{
    "type":"queryFirmwareVersion",
    "deviceId":"1003555153489448312",
    "platform":"linxot",
    "protocolVersion":1,
    "firmwareVersion":"44444",
    "softVersion":"55555"
}
```

参数 | 类型|是否必填|描述
---|---|---|---
deviceId  | string|是|设备ID
type  | string|是|报文类型 这里固定-"queryFirmwareVersion"
firmwareVersion  | string|是|硬件版本号
softVersion  | string|是|软件版本号
protocolVersion  | int|是|协议版本

####     9、D0-settingParam的ACK报文

```
{
    "type":"settingParam",
    "deviceId":"1003555153489448312",
    "platform":"linxot",
    "protocolVersion":1,
    "status":0
}
```

参数 | 类型|是否必填|描述
---|---|---|---
deviceId  | string|是|设备ID
type  | string|是|报文类型 这里固定-"settingParam"
status  | int|是|0-成功，其它-错误码 
protocolVersion  | int|是|协议版本，





---



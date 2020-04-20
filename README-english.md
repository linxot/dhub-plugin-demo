
[中文](https://github.com/linxot/dhub-plugin-demo/blob/master/README-chinese(simplified).md)
[繁体](https://github.com/linxot/dhub-plugin-demo/blob/master/README-chinese(traditional).md)


## Preface
    1. Need to receive the corresponding ack to end the command.
    2. When there is no command to send, a no-action command needs to be sent to ensure that the background receives the data reported by the device and enables the device to go into sleep mode faster.
    3. The device has data report, the device opens the command receiving window, then the command can be issued.
    4. In the [secret.properties] file under the resources path, modify the key of the device.


## 一、maven-dependency
     
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

## 二、Instruction


### 1、Upload Data
#### ---  Decrypt：
> **Decrypt** and **Decode** the data reported by the device .

```java
PayLoadDataDecode payLoadDataDecode = new PayLoadDataDecode(deviceDomain.getDevEui());

//payload Decrypt
JSONObject result = payLoadDataDecode.decode(body);
```

Method：


```java
 public PayLoadDataDecode(String devEui);
 
 public JSONObject decode(String value);
```

parameter | description
---|---
value | data to decrypt
devEui | devEUI

#### --- example：
1. The data pushed by oneNET  is as follows：
   
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

2.   The value of [msg.value] and call the decode().
   

```java
payLoadDataDecode.decode(msg.value);
```
3. return data in JSON format.

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


###  2、Downlink
#### --- encoder
>  After the command encoding conversion, the encoded command is required to be encrypted




```java
    //create command
    Command command = new Command();
        //
        command.setCmdType("settingParam");
        //
        command.setItem(JSONObject);
        //
        command.setRange("item");
    //create obj
    PayLoadDataEncode payLoadDataEncode = new PayLoadDataEncode();
    //encode
    EncodeResult encodeResult = payLoadDataEncode.encode(command);
```
---

Command

parameter |type |required | description
---|---|---|---
cmdType | string | Yes | Command type
range | string | Yes| Parameter type
queueId | string | No | Command ID
platform | string | NO | platform
item | JSONObject | Yes | command details

---
EncodeResult

parameter  |type|required | description
---|---|---|---
deviceId | string| No|device Id
platform | string| No|platform
body | byte[]| Yws|Encoded data
payload | string| Yes|Encoded hex data
ack | string| Yes|Ack of corresponding command

####  Encryption
>     A byte array or hexadecimal string is obtained after a successful encoding. The encoded command data is encrypted below.


```java
//create obj
 PayLoadDataEncode payLoadDataEncode = new PayLoadDataEncode();
 String payload = payLoadDataEncode.payloadEncrypt(cmd,
                deviceDomain.getDevEui(),
                seq,
                1);
```
Method：

```java
public String payloadEncrypt(String value, String devEui, int seq, int protocol) ;
```



parameter | description
---|---
value | data to encrypt
devEui | devEui
seq | message counter<br> the cumulative value of message number (this counter + 1 is required for each command sent to the device)
protocol | protocol version,  default: 1

#### --- example：
The command will be implemented in two steps：
>  1.Store the issued command to message queue or cache after coding.
    
    
```
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
//
Command command = new Command();
//
command.setCmdType("settingThreshold");
//JSONObject 
command.setItem(JSONObject);
//
command.setRange("item");
//create obj
PayLoadDataEncode payLoadDataEncode = new PayLoadDataEncode();
//
EncodeResult encodeResult = payLoadDataEncode.encode(command);

...
//encodeResult.payload or payload.body save to message queue or cache

```

> 2.The device report data, in the window where the device receives data, the data is encrypted and sent to the device.
 

```java
//encodeResult.payload OR payload.body hex
 String payload = payLoadDataEncode.payloadEncrypt(encodeResult.payload,
                deviceDomain.getDevEui(),
                seq,
                1);

//Call the API of command issued by the operator platform, http://API_ADDRESS/nbiot/execute
//request example:
{
    "args": payload
}
```

    
## 三、Command and messages
###   Command
####     1、cmdType="settingThreshold"：Set alarm rules
     example：
    
    {
    "deviceId":"xxxxxx",
    "data":[
        {
            "userId":0,
            "temperature_max":260,
            "temperature_min":200,
            "humidity_max":640,
            "humidity_min":600
        }
        ]
    }

    

parameter  |type|required | description
---|---|---|---
userId  | long|Yes|default：0
temperature_max | int|Yes| Value range: - 450 ~ 1250，32767 - indicates that the value is not set
temperature_min | int|Yes| Value range: - 450 ~ 1250，32767 - indicates that the value is not set
humidity_max | int|Yes| Value range: 0 ~ 1000，32767 - indicates that the value is not set
humidity_min | int|Yes|Value range:0 ~ 1000，32767 - indicates that the value is not set

####     2、cmdType="settingParam"：set device parameters
     example：
    
    {
        "deviceId":"1586859946579722",
        "recordPointTime":570,
        "temperatureThreshold":30,
        "humidityThreshold":30,
        "timeThreshold":30
    }
               

parameter  |type|required | description
---|---|---|---
recordPointTime | int|Yes|historical data reporting interval. <br>Unit: minute, default: 570 min
timeThreshold | int|Yes|default: value=0，<br>Then would save the data through the temperature and humidity threshold.<br> Value range:1-60 Unit: min。
temperatureThreshold | int|Yes|temperature recording threshold.<br> Unit: ± 0.1 ℃, default: ± 0.5 ℃
humidityThreshold | int|Yes|humidity recording threshold. <br>Unit: ± 0.1% RH, default: ± 3%
    
    
####     3、cmdType="queryRecordPoint"：query historical data
     example：
    
        {
            "deviceId":"xxxxxxxx"
        }

parameter  |type|required | description
---|---|---|---
deviceId  | string|Yes|deviceId

    
####     4、cmdType="queryParam"：query device parameters
    example：
    
    {
        "deviceId":"1529993764014110",
        "param":[
            "recordPointTime",
            "weatherTime",
            "temperatureThreshold",
            "humidityThreshold"
        ]
    }
    
parameter  |type|required | description
---|---|---|---
deviceId  | string|Yes|deviceId
param  | list|Yes|param to query: <br> recordPointTime = historical data reporting interval <br> weatherTime = weather forecast request interval <br> temperatureThreshold = temperature collection threshold <br> humidityThreshold = humidity collection threshold
    
####     5、cmdType="queryInstallation"：query installation status
    example：
    
    {
         "deviceId":"xxxxxxxx"
    }
    
parameter  |type|required | description
---|---|---|---
deviceId  | string|Yes|deviceId

####     6、cmdType="queryStatus"：query status
    example：
     
    {
        "deviceId":"xxxxxxxx"
    }
    
parameter  |type|required | description
---|---|---|---
deviceId  | string|Yes|query status

####     7、cmdType="downLinkFF"：nothing message
    example：
    
    {
        "deviceId":"xxxxxxxx"
    }
    
parameter  |type|required | description
---|---|---|---
deviceId  | string|Yes|deviceId

####     8、cmdType="shutdown"：shutdown
    example：
    
    {
         "deviceId":"xxxxxxxx"
    }
    
parameter  |type|required | description
---|---|---|---
deviceId  | string|Yes|deviceId


###   Uplink-message
####     1、81-status

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

parameter  |type|required | describe
---|---|---|---
deviceId  | string|Yes|deviceId
type  | string|Yes|default:"status"
sensorWarn  | boolean|Yes|the sensor warning
dateFormat  | string|Yes|CST time
recordTime  | string|Yes|CST time
installWarn  | string|Yes|installation warning
voltage  | string|Yes|battery voltage, unit: 0.1V <br> (2650 +Val*1000/255 of protocolVersion=1, unit: MV)
temperature  | string|Yes| temperature.<br> Unit: 0.1 ℃, value range: - 450 ~ 1250
humidity  | string|Yes| humidity.<br> Unit: 0.1rh, value range: 0 ~ 1000
signal  | string|Yes|signal strength; 99- invalid
timestamp  | string|Yes| timestamp (message generation time)
cid  | long|No|station Id
lac|long|No|station information
alarms|list|No|alarm
alarmSize|int|No|alarm size
userId|long|No|userId
alarmMsg|list|No|alarm detail
    alarmCode|int|No|alarm Type <br> 	0 —— temperature over upper limit alarm;<br>	1 —— temperature over upper limit alarm recovery;<br> 2 —— temperature over lower limit alarm;<br> 3 —— temperature over lower limit alarm recovery;<br> 4 —— humidity over upper limit alarm;<br> 5 —— humidity over upper limit alarm recovery;<br> 6 —— humidity over lower limit alarm;<br> 7 —— humidity over lower limit alarm recovery;
alarmRecordTime|long|No|alarm record time
alarmTemperature|int|No|alarm temperature
alarmHumidity|int|No|alarm humidity
protocolVersion  | int|Yes|protocol version



####     2、83-settingThreshold-ACK

```
{
    "type":"settingThreshold",
    "deviceId":"1003555153489448312",
    "platform":"linxot",
    "protocolVersion":1,
    "status":0
}
```

parameter  |type|required | description
---|---|---|---
deviceId  | string|Yes|deviceId
type  | string|Yes|default:-"settingThreshold"
status  | int|Yes|0-success, others-error
protocolVersion  | int|Yes|protocol version

####     3、85-recordPoint-historical data

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

parameter  |type|required | description
---|---|---|---
deviceId  | string|Yes|deviceId
type  | string|Yes|default:"recordPoint"
status  | int|Yes|subsequent data identification<br>Used when historical data needs to be reported multiple times：<br>0-No data in the future。<br>1-Subsequent data
records  | list|Yes|historical data of temperature and humidity
recordTime  | long|Yes|record time
temperature  | int|Yes|temperature <br> unit: 0.1 ℃, value range: - 450 ~ 1250
humidity  | int|Yes|humidity <br> unit: 0.1rh, value range: 0 ~ 1000
protocolVersion  | int|Yes|protocol version

    *Remarks:
    If status = 1, the platform needs to send query record point command(queryRecordPoint) until status = 0;*
---

####     4、86-installation-ACK

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

parameter  |type|required | description
---|---|---|---
deviceId  | string|Yes|deviceId
type  | string|Yes|default:"installation"
state  | int|Yes|installation status: 0-in, 1-out 
sensorState  | int|No|sensor induction: 0-no induction, 1-induction
recordTime  | long|Yes|record time
protocolVersion  | int|Yes|protocol version

####     5、C6-settingInterval-ACK

```
{
    "type":"settingInterval",
    "deviceId":"1003555153489448312",
    "platform":"linxot",
    "protocolVersion":1,
    "status":0
}
```

parameter  |type|required | description
---|---|---|---
deviceId  | string|Yes|deviceId
type  | string|Yes|default:"settingInterval"
status  | int|Yes|0-success, others-error 
protocolVersion  | int|Yes|protocol version

####     6、C9-queryNbDetails-ACK

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

parameter  |type|required | describe
---|---|---|---
deviceId  | string|Yes|deviceId
type  | string|Yes|default:"queryNbDetails"
imei  | string|Yes|imei num
simNum  | string|Yes|sim num
version  | string|Yes|module version information
imsi  | string|Yes|imsi num
protocolVersion  | int|Yes|protocol version

####     7、CA-queryNbSignal-ACK

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

parameter  |type|required | description
---|---|---|---
deviceId  | string|Yes|deviceId
type  | string|Yes|default:"queryNbSignal"
signalValue  | int|Yes|0: signal=-113dBm；<br> 1: signal= -111dBm ；<br>2~30 :signal=-109~-53dBm；<br>31:signal=-51dBm ；<br>99:Not known or not detectable
ber  | int|Yes|0-7 ;99
sinr  | int|Yes|-127=invalid, other values are valid
protocolVersion  | int|Yes|protocol version

####     8、CD-queryFirmwareVersion-ACK

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

parameter  |type|required | describe
---|---|---|---
deviceId  | string|Yes|deviceId
type  | string|Yes|default:"queryFirmwareVersion"
firmwareVersion  | string|Yes|firmwareVersion
softVersion  | string|Yes|softVersion
protocolVersion  | int|Yes|protocol version

####     9、D0-settingParam-ACK

```
{
    "type":"settingParam",
    "deviceId":"1003555153489448312",
    "platform":"linxot",
    "protocolVersion":1,
    "status":0
}
```

parameter  |type|required | describe
---|---|---|---
deviceId  | string|Yes|deviceId
type  | string|Yes|default:"settingParam"
status  | int|Yes|0-success, others-error
protocolVersion  | int|Yes|protocol version，

---




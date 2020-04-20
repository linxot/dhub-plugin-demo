## 前言
    1.指令下發需要收到對應的ACK才能結束指令。
    2.沒有指令要下發時，需要下發一個無事指令，保證後臺收到設備上報的數據和使設備更快進去休眠模式。
    3.設備有數據上報時，才會打開指令接收視窗，這時才能下發指令
    4.在resources路徑下可以修改設備金鑰[secret.properties]
      默認金鑰：appkey=xxxxxx


## 一、導入maven引用
     
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

## 二、使用說明


### 1、上行數據
#### ---  解密調用：
> 對設備上報的數據payload進制解密、解碼，獲取到JSONObject（三、指令和報文對應表）：

```java
PayLoadDataDecode payLoadDataDecode = new PayLoadDataDecode(deviceDomain.getDevEui());

//payload 数据解密
JSONObject result = payLoadDataDecode.decode(body);
```

方法解釋：


```java
 public PayLoadDataDecode(String devEui);
 
 public JSONObject decode(String value);
```

參數|描述
---|---
value | 要解密的數據
devEui | 設備的devEUI

#### --- 示例場景：
1. oneNET平臺推送過來的數據如下：
   
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

2.  獲取msg.value的值，調用decode（）方法
   

```java
payLoadDataDecode.decode(msg.value);
```
3. 即可獲取json格式的數據

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
#### --- 編碼調用
>  應用收到指令之後，需要將它進行編碼轉換後，還需要編碼後的數據進行加密（加密步驟下下麵），設備才能識別該指令




```java
    //創建command
    Command command = new Command();
        //報文類型
        command.setCmdType("settingParam");
        //JSONObject 下面描述
        command.setItem(JSONObject);
        //參數類型
        command.setRange("item");
    //聲明編碼對象
    PayLoadDataEncode payLoadDataEncode = new PayLoadDataEncode();
    //編碼操作
    EncodeResult encodeResult = payLoadDataEncode.encode(command);
```

    
    

---

Command （發送指令對象）

參數|類型|是否必填|描述
---|---|---|---
cmdType | string|是|指令類型
range | string |是| 參數類型
queueId | string |否| 指令ID
platform | string |否| 厂商简称
item | JSONObject |是| 參數對象（三）


---

EncodeResult （編碼後指令數據格式）

參數|類型|是否必填|描述
---|---|---|---
deviceId | string| 否|設備Id
platform | string| 否|廠商簡稱
body | byte[]| 是|編碼後的數據
payload | string| 是|編碼後的16進制數據
ack | string| 是|對應指令的ack

#### --- 加密調用
>     成功編碼後會得定一個byte數組或16進制字串，以下算灋對編碼後的指令數據進行加密


```java
//初始化
 PayLoadDataEncode payLoadDataEncode = new PayLoadDataEncode();
 String payload = payLoadDataEncode.payloadEncrypt(cmd,
                deviceDomain.getDevEui(),
                seq,
                1);
```
方法解釋：

```java
public String payloadEncrypt(String value, String devEui, int seq, int protocol) ;
```



參數|描述
---|---
value | 要加密的數據
devEui | 設備的devEui
seq | 報文counter，報文數的累計值，（每次向設備下發指令都要將這個counter+1）
protocol | 報文版本，如未特別說明默認：1

#### --- 示例場景：
下行指令會分開兩步實現：
>  1.將下發指令進行編碼後存儲到消息隊列或緩存。
    
    
```
//要下發下發告警規則 
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
//創建command
Command command = new Command();
//報文類型
command.setCmdType("settingThreshold");
//JSONObject 上面的josn数据
command.setItem(JSONObject);
//參數類型
command.setRange("item");
//聲明編碼對象
PayLoadDataEncode payLoadDataEncode = new PayLoadDataEncode();
//編碼操作
EncodeResult encodeResult = payLoadDataEncode.encode(command);

...
//將encodeResult.payload或payload.body保存到消息隊列或緩存

```

> 2.應用收到設備的數據後，在設備接收數據的視窗內，將資料加密後下發給設備。

 

```java
//取encodeResult.payload或payload.body的16进制,进行加密
 String payload = payLoadDataEncode.payloadEncrypt(encodeResult.payload,
                deviceDomain.getDevEui(),
                seq,
                1);

//調用運營商平臺下發指令的API， 例如oneNET http://API_ADDRESS/nbiot/execute
//请求示例:
{
    "args": payload
}
```

    
## 三、指令和報文對應表 
###   下行指令(Command)
####     1、cmdType="settingThreshold"：設定告警規則
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
    

參數|類型|是否必填|描述
---|---|---|---
userId  | long|是|這裡默認：0
temperature_max | int|是|溫度下限。取值範圍：-450 ~ 125032767-表示該值不設定
temperature_min | int|是|溫度下限。取值範圍：-450 ~ 125032767-表示該值不設定
humidity_max | int|是|溫度下限。取值範圍：-450 ~ 125032767-表示該值不設定
humidity_min | int|是|溫度下限。取值範圍：-450 ~ 125032767-表示該值不設定

####     2、cmdType="settingParam"：設定設備參數
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
    

參數|類型|是否必填|描述
---|---|---|---
recordPointTime | int|是|歷史資料上報間隔。組織：分，默認：9.5小時
timeThreshold | int|是|間隔如果數據是0，就是默認的門限來保存數據，如果是1-60就是1-60分鐘保存一個數據。
temperatureThreshold | int|是|溫度記錄門限。組織：±0.1℃，默認：±0.5℃
humidityThreshold | int|是|濕度記錄門限。組織：±0.1%RH，默認：±3%
    
    
####     3、cmdType="queryRecordPoint"：査詢歷史資料
    item 示例：
    [
            {
                "deviceId":"xxxxxxxx"
            }
    ]

參數|類型|是否必填|描述
---|---|---|---
deviceId  | string|是|設備ID

    
####     4、cmdType="queryParam"：査詢設備參數
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

參數|類型|是否必填|描述
---|---|---|---
deviceId  | string|是|設備ID
deviceId  | list|是|要査詢的參數：<br>recordPointTime=歷史資料上報間隔<br>weatherTime=天氣預報的請求間隔<br>temperatureThreshold=溫度採集閥值<br>humidityThreshold=濕度採集閥值
    
    
####     5、cmdType="shutdown"：關機
    item 示例：
    [
            {
                "deviceId":"xxxxxxxx"
            }
    ]

參數|類型|是否必填|描述
---|---|---|---
deviceId  | string|是|設備ID


####     6、cmdType="queryStatus"：査詢狀態
    item 示例：
    [
            {
                "deviceId":"xxxxxxxx"
            }
    ]

參數|類型|是否必填|描述
---|---|---|---
deviceId  | string|是|設備ID

####     7、cmdType="downLinkFF"：無事報文
    item 示例：
    [
            {
                "deviceId":"xxxxxxxx"
            }
    ]

參數|類型|是否必填|描述
---|---|---|---
deviceId  | string|是|設備ID


###   上行報文
####     1、81-心跳報文


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
參數|類型|是否必填|描述
---|---|---|---
deviceId  | string|是|設備ID
type  | string|是|報文類型，這裡固定-“status”
sensorWarn  | boolean|是|感測器感應告警
dateFormat  | string|是|CST时间
recordTime  | string|是|CST时间
installWarn  | string|是|安裝感應告警
voltage  | string|是|電池電壓，組織：0.1V<br>（協定版本號1的2650+val*1000/255；組織mV）
temperature  | string|是|當前溫度。組織：0.1℃，取值範圍：-450 ~ 1250
humidity  | string|是|當前濕度。組織：0.1RH，取值範圍：0 ~ 1000
signal  | string|是|信號強度。99-無效，參見NB模組定義
timestamp  | string|是|狀態時戳（報文生成時刻）
cid  | long|否|基站Id
lac|long|否|基站資訊
alarms|list|否|告警資訊
alarmSize|int|否|告警數
userId|long|否|用戶ID
alarmMsg|list|否|告警詳情
alarmCode|int|否|告警類型<br> 0——溫度超上限告警產生；<br> 1——溫度超上限告警恢復；<br> 2——溫度超下限告警產生；<br> 3——溫度超下限告警恢復；<br> 4——濕度超上限告警產生；<br> 5——濕度超上限告警恢復；<br> 6——濕度超下限告警產生；<br> 7——濕度超下限告警恢復；
alarmRecordTime|long|否|告警時間
alarmTemperature|int|否|告警溫度
alarmHumidity|int|否|告警濕度
protocolVersion  | int|是|協定版本



####     2、83-settingThreshold的ACK報文

```
{
    "type":"settingThreshold",
    "deviceId":"1003555153489448312",
    "platform":"linxot",
    "protocolVersion":1,
    "status":0
}
```

參數|類型|是否必填|描述
---|---|---|---
deviceId  | string|是|設備ID
type  | string|是|報文類型這裡固定-“settingThreshold”
status  | int|是|0-成功，其它-錯誤碼
protocolVersion  | int|是|協定版本



####     3、85-recordPoint的歷史資料報文

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

參數|類型|是否必填|描述
---|---|---|---
deviceId  | string|是|設備ID
type  | string|是|報文類型，這裡固定-“recordPoint”
status  | int|是|後續數據標識<br>用於歷史資料很多需要多次上報的情况：<br>0-後續沒有數據需要上報。<br>1-後續還有數據需要上報
records  | list|是|溫濕度歷史資料
recordTime  | long|是|該組溫濕度記錄的時間
temperature  | int|是|溫度組織：0.1℃，取值範圍：-450 ~ 1250
humidity  | int|是|濕度組織：0.1RH，取值範圍：0 ~ 1000
protocolVersion  | int|是|協定版本

*備註：
如果status=1時，平臺需要下發査詢歷史資料報文（queryRecordPoint）；直到status=0；*
---

####     4、4E-shutdown的ACK報文

```
{
    "type":"shutdown",
    "deviceId":"1003555153489448312",
    "platform":"linxot",
    "protocolVersion":1,
    "status":0
    
}
```

參數|類型|是否必填|描述
---|---|---|---
deviceId  | string|是|設備ID
type  | string|是|報文類型這裡固定-“shutdown”
status  | int|是|0-成功，其它-錯誤碼
protocolVersion  | int|是|協定版本


####     5、C6-settingInterval的ACK報文

```
{
    "type":"settingInterval",
    "deviceId":"1003555153489448312",
    "platform":"linxot",
    "protocolVersion":1,
    "status":0
}
```

參數|類型|是否必填|描述
---|---|---|---
deviceId  | string|是|設備ID
type  | string|是|報文類型這裡固定-"settingInterval"
status  | int|是|0-成功，其它-錯誤碼
protocolVersion  | int|是|協定版本

####     6、C9-queryNbDetails的ACK報文

```
{
    "type":"queryNbDetails",
    "deviceId":"1003555153489448312",
    "platform":"linxot",
    "protocolVersion":1,
    "imei":"44444",
    "simNum":"55555",
    "version":"11111",
    "imsi":"8888"
}
```

參數|類型|是否必填|描述
---|---|---|---
deviceId  | string|是|設備ID
type  | string|是|報文類型這裡固定-"queryNbDetails"
imei  | string|是|設備imei號
simNum  | string|是|SIM卡號 
version  | string|是|模組版本資訊
imsi  | string|是|imsi號
protocolVersion  | int|是|協定版本

####     7、CA-queryNbSignal的ACK報文

```
{
    "type":"queryNbSignal",
    "deviceId":"1003555153489448312",
    "platform":"linxot",
    "signalValue":17,
    "protocolVersion":1,
    "ber":55,
    "sinr":11
}
```

參數|類型|是否必填|描述
---|---|---|---
deviceId  | string|是|設備ID
type  | string|是|報文類型這裡固定-"queryNbSignal"
signalValue  | int|是|0-信號強度小於等於-113dBm；<br> 1-信號強度為-111dBm；<br>2~30 -信號強度為-109至-53dBm；<br>31-信號強度大於等於-51dBm；<br>99 Not known or not detectable
ber  | int|是|0-7 99
sinr  | int|是|-127-無效，其它值有效
protocolVersion  | int|是|協定版本

####     8、CD-queryFirmwareVersion的ACK報文

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

參數|類型|是否必填|描述
---|---|---|---
deviceId  | string|是|設備ID
type  | string|是|報文類型這裡固定-"queryFirmwareVersion"
firmwareVersion  | string|是|硬體版本號
softVersion  | string|是|軟件版本號
protocolVersion  | int|是|協定版本

####     9、D0-settingParam的ACK報文

```
{
    "type":"settingParam",
    "deviceId":"1003555153489448312",
    "platform":"linxot",
    "protocolVersion":1,
    "status":0
}
```

參數|類型|是否必填|描述
---|---|---|---
deviceId  | string|是|設備ID
type  | string|是|報文類型這裡固定-"settingParam"
status  | int|是|0-成功，其它-錯誤碼 
protocolVersion  | int|是|協定版本





---


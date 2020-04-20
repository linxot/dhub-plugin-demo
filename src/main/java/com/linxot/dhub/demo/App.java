package com.linxot.dhub.demo;

import com.alibaba.fastjson.JSONObject;
import com.linxot.dhub.simple.PayLoadDataDecode;
import com.linxot.dhub.simple.PayLoadDataEncode;
import com.linxot.dhub.simple.code.Command;
import com.linxot.dhub.simple.code.EncodeResult;

/**
 * Created by chenjinlin on 2020/4/17.
 */
public class App {

    private static String devEui = "AL00021945002261";

    public static void main(String[] args) {

        //上行数据
        String value = "C15115A5B20800D467BE3B782084BBBE9EAFAB1905FA45EB0115F18AFD27C61CAEB18B208AE7403AFF3DA77395";
        upload(value);

        //下行指令
        Command command = new Command();
        command.setCmdType("settingParam");
        command.setRange("item");
        command.setPlatform("linxot");

        /**
         * {
         "deviceId":"1586859946579722",
         "recordPointTime":570,
         "temperatureThreshold":30,
         "humidityThreshold":30,
         "timeThreshold":30
         }
         */
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("recordPointTime", 570);
        jsonObject.put("temperatureThreshold", 30);
        jsonObject.put("humidityThreshold", 30);
        jsonObject.put("timeThreshold", 30);

        command.setItem(jsonObject);
        down(command);
    }

    /**
     * 上行数据
     *
     * @param value
     */
    public static void upload(String value) {
        PayLoadDataDecode payLoadDataDecode = new PayLoadDataDecode(devEui);
        System.out.println("解密解码后的数据 =" + payLoadDataDecode.decode(value).toJSONString());
    }

    /**
     * 下发指令
     *
     * @param command
     */
    public static void down(Command command) {
        PayLoadDataEncode payLoadDataEncode = new PayLoadDataEncode();
        EncodeResult encodeResult = payLoadDataEncode.encode(command);
        String value = payLoadDataEncode.payloadEncrypt(encodeResult.getPayload(), devEui, 0, 1);
        System.out.println("编码加密后的数据=" + value);
    }
}

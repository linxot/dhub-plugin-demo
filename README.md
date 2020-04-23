# dhub-plugin-demo
**使用教程**
<br>
[中文简体](https://github.com/linxot/dhub-plugin-demo/blob/master/README-chinese(simplified).md)
<br>
[中文繁体](https://github.com/linxot/dhub-plugin-demo/blob/master/README-chinese(traditional).md)
<br>
[English](https://github.com/linxot/dhub-plugin-demo/blob/master/README-english.md)


> device reboot

![image](https://github.com/linxot/dhub-plugin-demo/blob/master/src/main/resources/img/pic2.png)

1. Every time the device is turned on, first request the timing.
2. The server receives the device timing request, generates the timing command and sends it to the device.
3. After the device timing is completed, the software and hardware version number message will be reported to request configuration parameters.
4. After receiving the software and hardware version, the server generates commands of configuration parameter [settingparam] and alarm rule [settingthreshold] and sends them to the device.
5. After receiving the command of [settingparam], the device will report the corresponding ACK and respond to the execution result.
6. Generate the command of alarm rule [settingthreshold] and send it to the device.
7. After receiving the command of [settingthreshold], the device will report the corresponding ACK and respond to the execution result.
8. After receiving the ACK, the server has no command to issue. It needs to issue the no matter command [downlinkff].
9. Equipment reports status information message.
10. When the server receives the status message, there is no command to be issued, and it needs to issue the no matter command [downlinkff].


[ntp] data: device adjust time.

[upload:queryFirmwareVersion]:The device does not persist configuration parameters, need to issue configuration parameters from the server every time  reboot.



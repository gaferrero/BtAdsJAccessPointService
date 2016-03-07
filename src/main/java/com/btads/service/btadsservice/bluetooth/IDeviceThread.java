package com.btads.service.btadsservice.bluetooth;

/**
 * Created by Gustavo on 28/10/2015.
 */
public interface IDeviceThread {

    void finishDeviceSend(String remoteAddress);

    String getDeviceId();
}

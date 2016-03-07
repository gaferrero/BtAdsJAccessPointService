package com.btads.service.btadsservice.bluetooth;

import com.btads.service.btadsservice.RestWrapper;
import com.btads.service.btadsservice.rest.BtAdsConnector;
import com.intel.bluetooth.BlueCoveImpl;

import javax.bluetooth.*;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Gustavo on 28/10/2015.
 */
public class ScannerMultiModeThread extends ScannerThread {

	    

    public ScannerMultiModeThread(String deviceId) throws BluetoothStateException {
    	super(deviceId);
    }

    
    @Override
    protected void addToSendQueue(RemoteDevice remDev, String url){
    	DeviceThread.addToSendQueue(remDev, url);     	
    }

    
    @Override
    protected void finishSearchComplete(){    	    	    	
    	scanningSemaphore.release();
    }
}

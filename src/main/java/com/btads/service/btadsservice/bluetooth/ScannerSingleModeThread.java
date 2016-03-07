package com.btads.service.btadsservice.bluetooth;

import com.btads.service.btadsservice.RestWrapper;

import javax.bluetooth.*;

import org.apache.log4j.chainsaw.Main;


import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by Gustavo on 28/10/2015.
 */
public class ScannerSingleModeThread extends ScannerThread {

	private List<String> discoveryURLs = new ArrayList<String>();
	private Properties mainProperties;

    public ScannerSingleModeThread(String deviceId, Properties mainProperties) throws BluetoothStateException {    	
    	super(deviceId);
    	this.mainProperties = mainProperties;        
    }

    
    @Override
    protected void addToSendQueue(RemoteDevice remDev, String url){
		synchronized(discoveryURLs){
			discoveryURLs.add(url);
		}    	
    }

    @Override
    protected void finishSearchComplete(){
    	
    	logger.debug("Sending to devices");
    	for (String url:discoveryURLs){
    		logger.info(String.format("Sending file to %s", url));
    		Send2DeviceThread send2DeviceThread = 
    				new Send2DeviceThread(url, deviceId, 
    						scanningSemaphore, stackId, mainProperties);
    		int resultSend = send2DeviceThread.sendSynchronous();
    		logger.info(String.format("Send Status %s", resultSend));
    		
    		//TODO: Send the response to the server
    		
    	}
    	
    	scanningSemaphore.release();
    }
    
}

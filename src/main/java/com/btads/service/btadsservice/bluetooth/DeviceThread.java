package com.btads.service.btadsservice.bluetooth;

import com.btads.service.btadsservice.RestWrapper;
import com.btads.service.btadsservice.rest.BtAdsConnector;
import com.btads.service.btadsservice.rest.FileInfoCustom;
import com.intel.bluetooth.BlueCoveImpl;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.microedition.io.Connector;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.ResponseCodes;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

/**
 * Created by Gustavo on 28/10/2015.
 */
public class DeviceThread extends Thread {

	private final static Logger logger = Logger.getLogger(DeviceThread.class);
	    
    private RemoteDevice remoteDevice;    
    private Operation putOperation;
    private ClientSession clientSession;
    private HeaderSet hsOperation;
    
    private static BlockingQueue<String> queue= new LinkedBlockingQueue<String>(); 
    private Semaphore multipleSendersSemaphore; 
    private String deviceId;
    private Object stackId;
    private Properties mainProperties;

    public DeviceThread(String deviceId, Properties mainProperties, Object devId){        
        this.mainProperties = mainProperties;
        this.deviceId = deviceId;
        
        int maxSenders= Integer.valueOf(LocalDevice.getProperty ("bluetooth.connected.devices.max")); 
    	logger.info("bluetooth.connected.devices.max= " + maxSenders); 
    	
    	multipleSendersSemaphore= new Semaphore(maxSenders);
    	
    }
    
    public static void addToSendQueue(RemoteDevice rd, String url) { 
    	try { 
    		logger.debug(String.format("Adding URL %s to the queue", url));
    		synchronized(queue){
	    	    if (!queue.contains(url)){ 
	    	    	queue.put(url); 
	    	    } 
    		}
    	}catch (Exception e) { 
    	    logger.error(e);
    	} 
    }



    private Object initBtStack(){
    	
    	Object devId=null; 
    	//BlueCoveImpl.setConfigProperty("bluecove.deviceID", deviceId); 
    	try { 
    	    LocalDevice.getLocalDevice(); 
    	} catch (BluetoothStateException e2) { 
    	    // we get exception when stack was already initialized. Let just attach to it 		
    	    logger.error(e2);
 
    	}finally { 
    	    try { 
    	    	devId = BlueCoveImpl.getThreadBluetoothStackID(); 
    	    } catch (BluetoothStateException e) { 
    	    	logger.error(e);
    	    } 
    	} 
    	int maxSenders= Integer.valueOf(LocalDevice.getProperty ("bluetooth.connected.devices.max")); 
    	logger.info("bluetooth.connected.devices.max= " + maxSenders); 
    	
    	multipleSendersSemaphore= new Semaphore(maxSenders);
    	
    	return devId;
    }

    @Override
    public void run() {
    	/*
        FileInfoCustom fileInfoCustom = connector.getNextFile(remoteDevice.getBluetoothAddress());
        String fileName = fileInfoCustom.getFileName();
        */    	
        
    	logger.info("running SenderThread " + this.getName() + " on device " + deviceId); 
		Object devId = initBtStack();
    	//initBtStack();
		while (true){ 
		    try { 
				// Determine if another send can be started. If not, wait for a send to finish. 
				multipleSendersSemaphore.acquireUninterruptibly();
				String url = null;
				synchronized(queue){
					url= !queue.isEmpty() ? queue.take() : null;
				}
				
				if (url != null){
					Thread s2d= new Send2DeviceThread(url, 
							deviceId, multipleSendersSemaphore, 
							devId, mainProperties); 
					s2d.start(); 
				}
			
				Thread.sleep(500);
		    } catch (Exception e) { 
		    	logger.error(e); 
		    			    	
		    } finally {
		    	multipleSendersSemaphore.release(); 
		    }
		    		    
		}     	    
    }


}

package com.btads.service.btadsservice.bluetooth;

import com.btads.service.btadsservice.App;
import com.btads.service.btadsservice.RestWrapper;
import com.intel.bluetooth.BlueCoveImpl;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.LocalDevice;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

/**
 * Created by Gustavo on 23/10/2015.
 */
public class BtAdsBlueTooth extends Thread {

	protected final static Logger logger = Logger.getLogger(BtAdsBlueTooth.class);
	
	protected final Object lock = new Object();
    protected String[] bluetoothDevices;
    protected boolean stopProcess;
    protected Properties mainProperties;

    protected BtAdsBlueTooth(){

    }

    public BtAdsBlueTooth(Properties mainProperties){
        this.mainProperties = mainProperties;
    }

    
    @Override
    public void run(){
    	
    	logger.info("Starting local bluetooth devices");
    	if (!connectToBluetooth()){
    		logger.error("There are not bluetooth devices or are not configured");
    		return;
    	}
    	
    	synchronized(lock){	    	
	    		    	
    		startInquireDevice(bluetoothDevices[0]);
			
    		for (int i=1; i < bluetoothDevices.length; i++){
    			DeviceThread deviceThread = new DeviceThread(bluetoothDevices[1], 
    					mainProperties, lock);
    		}	    	
    	}
    }


        
    public void kill(){
    	logger.info("Stoping the bluetooth sender process");
    	synchronized(lock){
    		
    	}
    }

    protected boolean connectToBluetooth() {

        logger.info("Connecting to local bluetooth devices");

        BlueCoveImpl.useThreadLocalBluetoothStack();


        bluetoothDevices = null;
        try {
            Vector vector = BlueCoveImpl.getLocalDevicesID();
            bluetoothDevices = new String[vector.size()];

            vector.toArray(bluetoothDevices);

            logger.info(String.format("Total devices connected to this computer %d", vector.size()));

        } catch (BluetoothStateException ex) {
            logger.error(ex);
        }

        return bluetoothDevices != null && bluetoothDevices.length > 0;
    }

    protected void startInquireDevice(String bluetoothDevice){
        logger.info(String.format("Initializing a new Search Thread for %s local bluetooth", bluetoothDevices[0]));

        ScannerThread scannerThread = ScannerFactory.createScannerFactory(bluetoothDevice,
                mainProperties);
        scannerThread.run();
    }


        
}

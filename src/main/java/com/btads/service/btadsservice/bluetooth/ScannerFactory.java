package com.btads.service.btadsservice.bluetooth;

import java.util.Properties;

import javax.bluetooth.BluetoothStateException;

import org.apache.log4j.Logger;

import com.btads.service.btadsservice.RestWrapper;
import com.intel.bluetooth.BlueCoveImpl;

public class ScannerFactory {
	
	protected final static Logger logger = Logger.getLogger(ScannerFactory.class);

	public static ScannerThread createScannerFactory(String deviceId,
			Properties mainProperties){
		Boolean modeMultiDevice;
		try {
			modeMultiDevice = BlueCoveImpl.getLocalDevicesID().size() > 1;
		
			if (modeMultiDevice){
				return new ScannerMultiModeThread(deviceId);
			} else {
				return new ScannerSingleModeThread(deviceId, mainProperties);
			}
		} catch (BluetoothStateException e) {
			logger.error(e);
		}
		
		return null;
	}
	
	
}

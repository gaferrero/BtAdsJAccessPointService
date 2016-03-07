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
public abstract class ScannerThread extends Thread implements DiscoveryListener, IDeviceThread {

	protected final static Logger logger = Logger.getLogger(ScannerThread.class);
	
    protected final int PHONE_CLASS_DEVICE = 512;
    
    protected DiscoveryAgent discoveryAgent; 
    protected Set<RemoteDevice> currentInquiryknownDevices= new HashSet<RemoteDevice>(); 
    protected Map<Integer, RemoteDevice> transactionId2deviceName= new Hashtable<Integer, RemoteDevice>(); 
    
    protected Semaphore maxServiceSearchesSemaphore= null; 
    protected Semaphore scanningSemaphore= new Semaphore(1); 
    protected AtomicInteger remainingDevices=new AtomicInteger(0);
    protected String deviceId;    
    protected volatile Thread blinker;    
    protected Object stackId;
    
    private final Object lock = new Object();        
    
    public ScannerThread(String deviceId) throws BluetoothStateException {
    	
    	logger.info(String.format("Start scanning from %s for mobile devices", deviceId));
    	
    	int maxServiceSearches = Integer.parseInt(LocalDevice.getProperty ("bluetooth.sd.trans.max")); 

    	logger.info("maxServiceSearches bluetooth.sd.trans.max= "+ maxServiceSearches); 
		
    	maxServiceSearchesSemaphore= new Semaphore(maxServiceSearches); 
		discoveryAgent = LocalDevice.getLocalDevice().getDiscoveryAgent(); 
		
		this.deviceId= deviceId;                  
    }


    protected Object initBtStack(String deviceId){ 
    	
    	logger.info(String.format("Initializing %s device", deviceId));	        
    	
    	Object devId = null;
    	try { 
    		BlueCoveImpl.setConfigProperty("bluecove.deviceID", deviceId);
    	    LocalDevice.getLocalDevice(); 
    	} catch (Exception e2) {  
    	    logger.error(e2); 
    	    try { 
    	    	devId = BlueCoveImpl.getThreadBluetoothStackID(); 
    	    	BlueCoveImpl.setThreadBluetoothStackID(devId); 
    	    } catch (Exception e) { 
    	    	logger.error(e);     			
    	    } 
    	} 
    	
    	
    	logger.debug("Initializing Debug Thread");
    	
    	return devId;
    }


    @Override
    public void run() {
    	stackId = initBtStack(deviceId); 
    			   
		try { 
			Thread thisThread = Thread.currentThread();
	    	blinker = thisThread;
	    	while (blinker == thisThread){ 
		    	logger.debug("scanningSemaphore.acquire() " + scanningSemaphore + "...."); 
		    	scanningSemaphore.acquire(); 
		    	
		    	logger.debug("scanningSemaphore.acquire() " + scanningSemaphore + "...done"); 
		    	startInquiry(); 
		    } 
		}catch (Exception e) { 
		    logger.error(e);		     
		} 
		
		logger.info(String.format("Finishing listening on device %s", deviceId));
    }
    
    private void startInquiry() {
    	try { 
    	    logger.info("Starting inq.."); 
    	    
    	    discoveryAgent.startInquiry(DiscoveryAgent.GIAC, this); 
    	    
    	    logger.info("Starting inq.. done"); 
    	}catch (Exception e) { 
    	    logger.error(e); 
    	} 
    }
    
    
    public void kill(){
    	logger.info("Stoping the bluetooth sender process");
    	blinker = null;	
    }




    public void deviceDiscovered(RemoteDevice remoteDevice, DeviceClass deviceClass) {    	        
        try { 
    	    logger.info("Dev discovered " + remoteDevice.getFriendlyName (false) + " : " + 
    	    		remoteDevice.getBluetoothAddress()); 
    	     
    	    int majorDeviceClass = deviceClass.getMajorDeviceClass(); 
    	    if (PHONE_CLASS_DEVICE == majorDeviceClass){ 
    	    	logger.debug("inquiry " + remoteDevice.getBluetoothAddress()+ " : " + majorDeviceClass); 
    	    	currentInquiryknownDevices.add(remoteDevice); 
    	    } else { 
    	    	logger.debug("ignoring non phone " + remoteDevice.getBluetoothAddress()); 
    	    } 
    	} catch (Exception e) { 
    	    logger.error(e);
    	}

    }

    public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
    	try { 
    	    logger.info("servicesDiscovered transId=" + transID); 
    	    RemoteDevice remDev = transactionId2deviceName.get(transID);    	        	   
    	    
    	    for (int i = 0; i < servRecord.length; i++) { 
	    		String url = servRecord[i].getConnectionURL (ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false); 
	    		if (url == null) continue; 
	    		
	    		logger.debug("Adding transId " + transID + " to sendQ - URL " + url);
	    		this.addToSendQueue(remDev, url);
	    		return;
    	    } 
    	    logger.info("Ignoring " + remDev + ":doesn't support required service "); 
    	}catch (Exception e) { 
    	    logger.error(e);
    	} 
    }
    
    
    protected abstract void addToSendQueue(RemoteDevice remDev, String url);

    public void serviceSearchCompleted(int transID, int respCode) {
    	maxServiceSearchesSemaphore.release(); 
    	if (DiscoveryListener.SERVICE_SEARCH_COMPLETED != respCode){ 
    	    logger.debug("serviceSearchCompleted !normal: " + respCode + ". transId= " + transID); 
    	} 
    	if (0 == remainingDevices.decrementAndGet()){ 
    	    logger.debug("Scanner thread " + scanningSemaphore + ": scanningSemaphore.release()..."); 
    	    
    	    currentInquiryknownDevices.clear(); 
    	    
    	    finishSearchComplete();
    	    
    	    //scanningSemaphore.notify(); 
    	    logger.debug("Scanner thread " + scanningSemaphore + ": scanningSemaphore.release()... Done");    	        	   
    	} 
    }
    
    protected abstract void finishSearchComplete();

    public void inquiryCompleted(int status) {
    	try { 
    	    logger.info("inquiryCompleted " + status); 
    	    
    	    if (DiscoveryListener.INQUIRY_COMPLETED != status){ 
    	    	logger.error("inquiryCompleted failed" ); 
    	    	
    	    } else {    	    
	    	    UUID[] OBEX_OBJECT_PUSH= new UUID[] { new UUID(0x1105) }; 
	    	    // OBEX_OBJECT_PUSH = 0x1105; 
	    	    for (RemoteDevice btDevice: currentInquiryknownDevices){ 
	    	    	maxServiceSearchesSemaphore.acquire(); 
		    		// If not, wait for a service search to end. 
		    		int transId = discoveryAgent.searchServices(null /*attrIDs*/, OBEX_OBJECT_PUSH, btDevice, this); 
		    		remainingDevices.incrementAndGet(); 
		    		logger.info("transactionId2deviceName " + 
		    				transId + " -> " + btDevice.getBluetoothAddress()); 
		    		transactionId2deviceName.put(transId, btDevice); 
	    	    } 
	    	    if (0 == currentInquiryknownDevices.size()){ 
	    	    	logger.info("Inquiry returned 0 devices. releasing semaphore to avoid deadlock"); 
	    	    	scanningSemaphore.release(); 
	    	    	logger.debug("Inquiry returned 0 devices. releasing semaphore to avoid deadlock... done"); 
	    	    } 
    	    }
    	}catch (Exception e) { 
    	    logger.equals(e);    	    
    	} 
    }

    public void finishDeviceSend(String remoteAddress) {
    	logger.info(String.format("The file was sent to %s successfully", remoteAddress));    	
    }

    public String getDeviceId(){
        return this.deviceId;
    }
}

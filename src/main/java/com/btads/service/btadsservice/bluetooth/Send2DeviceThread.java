package com.btads.service.btadsservice.bluetooth;

import java.io.File; 
import java.io.FileInputStream; 
import java.io.IOException; 
import java.io.OutputStream; 
import java.util.Properties;
import java.util.concurrent.Semaphore;

						
import javax.microedition.io.Connector; 
import javax.obex.ClientSession; 
import javax.obex.HeaderSet; 
import javax.obex.Operation; 
import javax.obex.ResponseCodes;

import org.apache.log4j.Logger;

						
import com.btads.service.btadsservice.RestWrapper;
import com.btads.service.btadsservice.UpdateRestWrapper;
import com.btads.service.btadsservice.rest.BtAdsConnector;
import com.btads.service.btadsservice.rest.FileInfoCustom;
import com.intel.bluetooth.BlueCoveImpl;

						
// multifile send: 
//http://pyx4j.com/snapshot/pyx4me/pyx4me-maven-plugins/obex-maven- plugin/xref/com/pyx4me/maven/obex/ObexBluetoothClient.html

						
public class Send2DeviceThread extends Thread { 
	
	private static final int SEND_ERROR_FILE_NOT_EXISTS = -503;
	public static final int SEND_CONNECTION_ERROR = -2;
	public static final int SEND_CONNECTION_REFUSED = -111;
	public static final int SEND_OPERATION_PROGRESS = -115;
	public static final int SEND_SUCCESSFULLY = 0;
	public static final int SEND_ERROR_EXCEPTION = -975;
	public static final int SEND_UNKNOWN_ERROR = -1;

	private final static Logger logger = Logger.getLogger(Send2DeviceThread.class);
	
    private ClientSession clientSession; 
    private HeaderSet hsConnectReply; 
    private HeaderSet hsOperation; 
    private Operation putOperation; 
    private String deviceId; 
    private Semaphore sem; 
    private String url; 
    private Object stackId;    
    private Properties mainProperties;
    private UpdateRestWrapper updateRestWrapper;
						    
    public Send2DeviceThread(String url, String deviceId, Semaphore sem, Object stackId,
    		Properties mainProperties) { 
		this.deviceId= deviceId; 
		this.sem= sem; 
		this.url= url; 
		this.stackId=stackId; 
		this.mainProperties = mainProperties;
		this.updateRestWrapper = new UpdateRestWrapper(this.mainProperties);
    }

						    
    private void initBtStack(){ 
    	
    	this.updateRestWrapper.start();
    	
    	BlueCoveImpl.setThreadBluetoothStackID(stackId); 	 
    }
    

						    
    @Override 
    public void run() { 
    	sendSynchronous(); 
    	sem.release();
    }
    
    public int sendSynchronous(){
    	int res = SEND_UNKNOWN_ERROR;
    	String threadName = this.getName(); 
		logger.info("running Send2DeviceThread " + threadName + " on device " + deviceId); 
		try { 
		    initBtStack(); 
		    logger.debug("Sending from " + threadName + " to " + url); 
		    
		    FileInfoCustom fileToSend = RestWrapper.getInstance().getNextFile(url);
			
		    if (fileToSend != null){
		    	res= send(fileToSend.getFileName());
		    
		    	UpdateRestWrapper.addUpdateResult(url, fileToSend.getMessageId(), res);
		    }
		    
		    logger.info("Sending from " + threadName + " to " + url + " ... done: " + res); 
		} catch (Exception e) { 
		    logger.error(e); 
		}finally { 
		    try { 
		    	BlueCoveImpl.releaseThreadBluetoothStack(); 
		    } catch (Exception e) { 
		    	logger.error(e); 
		    } 		    
		}
		
		return res;
    }

						    
    public int send(String fileToSend) { 
		String mimeType="x-application/java"; 
		File localFileToSend= new File(fileToSend); 
		
		if (!localFileToSend.exists()){
			return SEND_ERROR_FILE_NOT_EXISTS;
		}
		
		int res; 
		if (0 != (res=openConnection(url, localFileToSend.getName(), mimeType))){ 
		    logger.error("Cant open connection " + res); 
		    return res; 
		} 
	
		try {
		    // Send file to the server 
		    OutputStream os = putOperation.openOutputStream(); 
		    sendFile(localFileToSend, os); 
		    os.close(); 
		} catch (IOException e) { 
		    logger.error(e); 
		    return SEND_ERROR_EXCEPTION; 
		} catch (Exception e) {
			logger.error(e);
			return SEND_UNKNOWN_ERROR;
		} finally { 		
		    closeConection(); 
		} 
		
		
		
		return SEND_SUCCESSFULLY; 
    }

							    
    private int openConnection(String deviceUrl, String fileToSend, String mimeType) { 
		try { 
		    clientSession = (ClientSession) Connector.open(deviceUrl); 
		    hsConnectReply = clientSession.connect(null); 
		    if (hsConnectReply.getResponseCode() != ResponseCodes.OBEX_HTTP_OK) { 
		    	logger.error("Failed to connect"); 
		    	return SEND_UNKNOWN_ERROR; 
		    } 
		    hsOperation = clientSession.createHeaderSet(); 
		    hsOperation.setHeader(HeaderSet.NAME, fileToSend); 
		    hsOperation.setHeader(HeaderSet.TYPE, mimeType); 
		    
		    putOperation = clientSession.put(hsOperation); 
		} catch (Exception e) { 
		    logger.error(e); 
		    closeConection(); 
		    
		    if (e.getMessage().contains(String.valueOf(SEND_OPERATION_PROGRESS))){ 
		    	return SEND_OPERATION_PROGRESS; 
		    } 
		    if (e.getMessage().contains(String.valueOf(SEND_CONNECTION_REFUSED))){ 
		    	return SEND_CONNECTION_REFUSED; 
		    } 
		    return SEND_CONNECTION_ERROR; 
		} 
		
		return SEND_SUCCESSFULLY; 
    }

								    
    private void closeConection(){ 
		try{ 
		    putOperation.close(); 
		} catch (Exception e) {
			logger.error(e);
		} 
		
		try{ 
		    clientSession.disconnect(null); 
		} catch (Exception e) {
			logger.error(e);
		} 
		
		try {
		    clientSession.close(); 
		} catch (Exception e) {
			logger.error(e);
		} 
	
		putOperation=null; 
		clientSession= null; 
		hsOperation= null; 
		putOperation= null; 
    }

								    
    private void sendFile(File fileToSend, OutputStream os ) throws IOException{ 
		int data; 
		FileInputStream fin= new FileInputStream(fileToSend); 
		while (SEND_UNKNOWN_ERROR != (data=fin.read())){ 
		    os.write(data); 
		} 
		fin.close(); 
    } 
} 
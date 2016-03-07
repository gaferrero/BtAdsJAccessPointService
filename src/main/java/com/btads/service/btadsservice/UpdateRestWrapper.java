package com.btads.service.btadsservice;

import com.btads.service.btadsservice.bluetooth.DeviceThread;
import com.btads.service.btadsservice.bluetooth.Send2DeviceThread;
import com.btads.service.btadsservice.rest.BtAdsConnector;
import com.btads.service.btadsservice.rest.FileInfoCustom;
import com.btads.service.btadsservice.rest.IBtAdsConnector;
import com.btads.service.btadsservice.rest.ResultSend;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

/**
 * Created by Gustavo on 01/11/2015.
 */
public class UpdateRestWrapper extends Thread {

	private final static Logger logger = Logger.getLogger(UpdateRestWrapper.class);	
	
    private IBtAdsConnector connector;
    private Properties mainProperties;
    private String securityTokenSession;
    
    private static BlockingQueue<ResultSend> queue= new LinkedBlockingQueue<ResultSend>();

    private UpdateRestWrapper(){
    	
    }
    
    public UpdateRestWrapper(Properties mainProperties){

        this.mainProperties = mainProperties;
    }

    private void initConnector(){
        if (connector == null){ 
            connector = (IBtAdsConnector) new BtAdsConnector(mainProperties.getProperty("app.folder"));
        }
    }
    

    private boolean verifyConnections(){

        initConnector();

        try {
            securityTokenSession = connector.getSecurityToken(mainProperties.getProperty("app.url"),
                    mainProperties.getProperty("app.authorizationKey"));
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    
    public static void addUpdateResult(String address, String messageId, int result){
    	try { 
    		logger.debug(String.format("Adding Result %s to the queue", address));
    		synchronized(queue){	    	    
	    	    queue.put(new ResultSend(address, messageId, result)); 	    	    
    		}
    	}catch (Exception e) { 
    	    logger.error(e);
    	} 
    }
    
    @Override
    public void run(){
    	verifyConnections();
    	
    	while (true){ 
		    try { 
 				
				ResultSend resultSend = null;
				synchronized(queue){
					resultSend= !queue.isEmpty() ? queue.take() : null;
				}
				
				if (resultSend != null){
					connector.updateResult(mainProperties.getProperty("app.url"), 
							securityTokenSession, 
							resultSend.getAddress(), 
							resultSend.getMessageId(), 
							resultSend.getResult());
				}
			
				Thread.sleep(500);
		    } catch (Exception e) { 
		    	logger.error(e); 
		    			    	
		    } 
		    		    
		}  
    }
    
        
}

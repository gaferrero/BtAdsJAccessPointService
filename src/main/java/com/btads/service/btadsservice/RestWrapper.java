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
public class RestWrapper {

	private final static Logger logger = Logger.getLogger(RestWrapper.class);
	private static RestWrapper instance;
	
    private IBtAdsConnector connector;
    private Properties mainProperties;
    private String securityTokenSession;
    
    private static BlockingQueue<ResultSend> queue= new LinkedBlockingQueue<ResultSend>();

    private RestWrapper(){
    	
    }
    
    private RestWrapper(Properties mainProperties){

        this.mainProperties = mainProperties;
    }

    private void initConnector(){
        if (connector == null){ 
            connector = (IBtAdsConnector) new BtAdsConnector(mainProperties.getProperty("app.folder"));
        }
    }
    
    public void setProperties(Properties mainProperties) throws IOException{
    	if (mainProperties == null){
    		throw new IOException("Main properties are not defined.");
    	}
    	
    	this.mainProperties = mainProperties;
    }

    public boolean verifyConnections(){

        initConnector();

        try {
            securityTokenSession = connector.getSecurityToken(mainProperties.getProperty("app.url"),
                    mainProperties.getProperty("app.authorizationKey"));
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    public FileInfoCustom getNextFile( String address){
    	
    	verifyConnections();
    	
        FileInfoCustom fileInfo = null;
        try {
            fileInfo = connector.getNextFile(mainProperties.getProperty("app.url"), securityTokenSession, address);
        } catch (IOException e) {
        	//TODO: Verify the error code in order to manage the connection.
            return null;
        }
        return fileInfo;
    }   
    
    
    public static RestWrapper getInstance(){
    	if (instance == null){
    		instance = new RestWrapper();
    	}
    	
    	return instance;
    }
        
}

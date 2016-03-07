package com.btads.service.btadsservice;

import com.btads.service.btadsservice.bluetooth.BtAdsBlueTooth;
import com.btads.service.btadsservice.rest.BtAdsConnector;
import com.btads.service.btadsservice.rest.FileInfoCustom;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * Created by Gustavo on 19/10/2015.
 */
public class BtAdsService {

	private final static Logger logger = Logger.getLogger(BtAdsService.class);
	
    private String securityToken;
    private BtAdsConnector connector;
    private Properties mainProperties;
    private BtAdsBlueTooth blueTooth;    
    

    public BtAdsService(){
    }

    private boolean readProperties() {
    	
    	logger.debug("Reading main properties");
    	boolean result = false;
    	
        try {
            mainProperties = new Properties();

            String path = "main.properties";
            
            File file = new File(path);
            if (!file.exists()){
            	logger.error("The main.propeties file does not exist.");            	
            } else {
	            FileInputStream fileInputStream = new FileInputStream(path);                       
	            
	            mainProperties.load(fileInputStream);
	            fileInputStream.close();
	            result = true;
            }            
        } catch (Exception ex){
            logger.equals(ex);
        }
        
        return result;
    }

    private boolean connectToServer(){

        try {
            securityToken = connector.getSecurityToken(mainProperties.getProperty("app.url"),
                    mainProperties.getProperty("app.authorizationKey"));

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }




    public void start(){
        try {
        	        	
            if (!readProperties())
                return;

            RestWrapper.getInstance().setProperties(mainProperties);
            if (!RestWrapper.getInstance().verifyConnections())
            {
            	//TODO: Execute method until the server is online.
            	System.exit(-2);
            }
           
            blueTooth = new BtAdsBlueTooth(mainProperties);
            blueTooth.start();
                                 

        } catch (Exception e) {
            logger.error(e);
        }
    }


    public void stop() {    	
    	blueTooth.kill();
    	blueTooth.interrupt();    	
    }
}

package com.btads.service.btadsservice;

import java.io.File;

import org.apache.log4j.Logger;

/**
 * Hello world!
 *
 */
public class App 
{	
	private final static Logger logger = Logger.getLogger(App.class);
    private final static int WRONG_OPERATING_SYSTEM = -120;
	
	public App(){
		
	}
	
	private void initialize(){

        logger.info("Bluetooth Service Linux - JAVA Version" );
        logger.info("(c) BtAds");


        if (System.getProperty("os.name").startsWith("Windows")){
            logger.error("Wrong Operating System");
            System.exit(WRONG_OPERATING_SYSTEM);
        }


        BtAdsService service = new BtAdsService();
        service.start();
	}
	
    public static void main( String[] args )
    {
        App app = new App();
        app.initialize();
    }
    
    public static void start(String[] args){
    	
    }
    
    public static void stop(String[] args){
    	
    }
}

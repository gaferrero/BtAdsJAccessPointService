package com.btads.service.btadsservice.rest;

import org.apache.cxf.jaxrs.client.WebClient;
import org.json.JSONObject;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * Created by Gustavo on 21/10/2015.
 */
public class BtAdsConnector implements IBtAdsConnector  {

    private String fileFolder;

    public BtAdsConnector(String temporalFolder){
        this.fileFolder = temporalFolder;
    }

    public String getSecurityToken(String url, String authorizationKey) throws IOException {
        WebClient client = WebClient.create(url);

        String authorizationHeader = "Basic " + authorizationKey;
        client.header("Authorization", authorizationHeader);

        String token = client.path("CredentialService.svc/Authenticate").accept("text/json").get(String.class);

        if (token.isEmpty())
            throw new IOException("Error autorizando el ingreso");

        JSONObject json = new JSONObject(token);
        return json.getString("SecurityToken");

    }

    public FileInfoCustom getNextFile(String url, String token, String address) throws IOException {

        WebClient client = WebClient.create(url);

        client.header("Authorization", token);

        Response response = client.path(
                String.format("Orchestrator.svc/Orchestrator/Next/Stream/%s", address)).get();

        String location = this.fileFolder + response.getHeaderString("filename");
        String messageId = response.getHeaderString("messageid");

        FileInfoCustom fileInfoCustom = new FileInfoCustom();
        fileInfoCustom.setFileName(location);
        fileInfoCustom.setMessageId(messageId);

        File f = new File(location);
        if(f.exists() && !f.isDirectory()) {
            return fileInfoCustom;
        }

        FileOutputStream out = new FileOutputStream(location);
        InputStream is = (InputStream)response.getEntity();
        int len = 0;
        byte[] buffer = new byte[4096];
        while((len = is.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
        out.flush();
        out.close();
        is.close();

        return fileInfoCustom;
    }

    public void updateResult(String url, String token, String address, String messageId, int result){

        WebClient client = WebClient.create(url);
        client.header("Authorization", token);

        String path = String.format("Orchestrator.svc/Orchestrator/UpdateResult");

        JSONObject json = new JSONObject(new ResultSend(messageId, address, result));
        String data = json.toString();

        client.accept("application/json").type("application/json").path(path).invoke("POST", data);
    }

}

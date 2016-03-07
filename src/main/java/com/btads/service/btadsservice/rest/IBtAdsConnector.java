package com.btads.service.btadsservice.rest;

import java.io.IOException;

/**
 * Created by Gustavo on 01/11/2015.
 */
public interface IBtAdsConnector {
    String getSecurityToken(String url, String authorizationKey) throws IOException;

    FileInfoCustom getNextFile(String url, String token, String address) throws IOException;

    void updateResult(String url, String token, String address, String messageId, int result);
}

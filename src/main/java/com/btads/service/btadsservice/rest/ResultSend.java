package com.btads.service.btadsservice.rest;

/**
 * Created by Gustavo on 24/10/2015.
 */
public class ResultSend {

    private String messageId;
    private String address;
    private int result;

    public ResultSend(String messageId, String address, int result){
        this.messageId = messageId;
        this.address = address;
        this.result = result;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }
}

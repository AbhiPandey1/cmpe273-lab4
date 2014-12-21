package edu.sjsu.cmpe.cache.client;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.*;
import java.lang.InterruptedException;
import java.io.*;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.http.options.Options;


public class CRDTClient implements CRDTCallbackInterface {

    private ConcurrentHashMap<String, CacheServiceInterface> servers;
    private ArrayList<String> successServers;
    private ConcurrentHashMap<String, ArrayList<String>> dictResults;

    private static CountDownLatch countDownLatch;

    public CRDTClient() {

        servers = new ConcurrentHashMap<String, CacheServiceInterface>(3);
        CacheServiceInterface cache0 = new DistributedCacheService("http://localhost:3000", this);
        CacheServiceInterface cache1 = new DistributedCacheService("http://localhost:3001", this);
        CacheServiceInterface cache2 = new DistributedCacheService("http://localhost:3002", this);
        servers.put("http://localhost:3000", cache0);
        servers.put("http://localhost:3001", cache1);
        servers.put("http://localhost:3002", cache2);
    }

    // Callbacks
    @Override
    public void putFailed(Exception e) {
        System.out.println("The request has encountered a faliure");
        countDownLatch.countDown();
    }

    @Override
    public void putCompleted(HttpResponse<JsonNode> response, String serverUrl) {
        int codeValue = response.getCode();
        System.out.println("Put response is completed ! code " + codeValue + " on server " + serverUrl);
        successServers.add(serverUrl);
        countDownLatch.countDown();
    }

    @Override
    public void getFailed(Exception e) {
        System.out.println("The request has encountered a faliure");
        countDownLatch.countDown();
    }

    @Override
    public void getCompleted(HttpResponse<JsonNode> response, String serverUrl) {

        String value = null;
        if (response != null && response.getCode() == 200) {
            value = response.getBody().getObject().getString("value");
                System.out.println("value from server " + serverUrl + "is " + value);
            ArrayList serversWithValue = dictResults.get(value);
            if (serversWithValue == null) {
                serversWithValue = new ArrayList(3);
            }
            serversWithValue.add(serverUrl);

            // Save Arraylist of servers into dictResults
            dictResults.put(value, serversWithValue);
        }

        countDownLatch.countDown();
    }



    public boolean put(long key, String value) throws InterruptedException {
        successServers = new ArrayList(servers.size());
        countDownLatch = new CountDownLatch(servers.size());

        for (CacheServiceInterface cache : servers.values()) {
            cache.put(key, value);
        }

        countDownLatch.await();

        boolean isSuccess = Math.round((float)successServers.size() / servers.size()) == 1;

        if (! isSuccess) {

            delete(key, value);
        }
        return isSuccess;
    }

    public void delete(long key, String value) {

        for (final String serverUrl : successServers) {
            CacheServiceInterface server = servers.get(serverUrl);
            server.delete(key);
        }
    }


  public String get(long key) throws InterruptedException {
        dictResults = new ConcurrentHashMap<String, ArrayList<String>>();
        countDownLatch = new CountDownLatch(servers.size());

        for (final CacheServiceInterface server : servers.values()) {
            server.get(key);
        }
        countDownLatch.await();

        String actualValue = dictResults.keys().nextElement();

        if (dictResults.keySet().size() > 1 || dictResults.get(actualValue).size() != servers.size()) {
            ArrayList<String> maxValues = maxKeyForTable(dictResults);
            if (maxValues.size() == 1) {
            	actualValue = maxValues.get(0);

                ArrayList<String> repairServers = new ArrayList(servers.keySet());
                repairServers.removeAll(dictResults.get(actualValue));

                for (String serverUrl : repairServers) {
                    System.out.println("repairing: " + serverUrl + " value: " + actualValue);
                    CacheServiceInterface server = servers.get(serverUrl);
                    server.put(key, actualValue);

                }

            } else {
            }
        }

        return actualValue;

    }


    public ArrayList<String> maxKeyForTable(ConcurrentHashMap<String, ArrayList<String>> table) {
        ArrayList<String> noOfMaxKeys= new ArrayList<String>();
        int maxValue = -1;
        for(Map.Entry<String, ArrayList<String>> entry : table.entrySet()) {
            if(entry.getValue().size() > maxValue) {
            	noOfMaxKeys.clear(); 
                noOfMaxKeys.add(entry.getKey());
                maxValue = entry.getValue().size();
            }
            else if(entry.getValue().size() == maxValue)
            {
            	noOfMaxKeys.add(entry.getKey());
            }
        }
        return noOfMaxKeys;
    }





}
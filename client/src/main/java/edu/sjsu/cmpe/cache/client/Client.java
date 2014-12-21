package edu.sjsu.cmpe.cache.client;

import com.mashape.unirest.http.Unirest;

import java.util.*;
import java.lang.*;
import java.io.*;

public class Client {

    public static void main(String[] args) throws Exception {
        System.out.println("Start the Cache Client System");
        CRDTClient newCrdtClient = new CRDTClient();

        
        boolean crdtResult = newCrdtClient.put(1, "a");
        System.out.println("The CRDT result generated is " + crdtResult);
        Thread.sleep(30*1000);
        System.out.println("Stage 1: putting(1 : a); a sleep process of 30s initiated ");


        newCrdtClient.put(1, "b");
        Thread.sleep(30*1000);
        System.out.println("Stage 2: putting(1 : b); a sleep process of 30s initiated ");


        String output = newCrdtClient.get(1);
        System.out.println("Stage 3: getting(1) => " + output);

        System.out.println("Done Now Exiting Client");
        Unirest.shutdown();
    }

}

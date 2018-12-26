package com.netzwerk.savechat.client;

import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.nerdyhomeproductions.commandlinechat.MainActivity;

public class Client extends Thread {
    private String hostname;
    private int port;
    PublicKey pubkey, ptrkey;
    PrivateKey prvkey;
    private MainActivity commandLine;
    BlockingQueue<String> reader = new ArrayBlockingQueue<>(10);

    public Client(MainActivity commandLine) {
        this.commandLine = commandLine;
    }

    public void add(String s) {
        try {
            reader.put(s);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    public void println(String s) {
        commandLine.add(s);
    }

    public void run() {
        // create working dir
        if (!new File(Environment.getExternalStorageDirectory().toString() + "/cmdchat").mkdirs()) {
            // println("Could not create directory");
        }
        // init connection
        hostname = "37.120.187.17";
        port = 22713;
        try {
            String line;
            println("Enter IP-address/hostname:");
            line = reader.take();
            if (line.trim().length() > 0) hostname = line.trim();
            println("Enter port:");
            line = reader.take();
            if (line.trim().length() > 0) port = Integer.parseInt(line);
        } catch (NumberFormatException | InterruptedException ex) {
            ex.printStackTrace();
        }

        try {
            println("Connecting to: " + hostname + ":" + port);

            Socket socket = new Socket(hostname, port);

            println("Establishing encryption...");
            WriteThread writeThread = new WriteThread(socket, this);
            ReadThread readThread = new ReadThread(socket, this, writeThread);

            readThread.start();
            writeThread.start();

        } catch (UnknownHostException ex) {
            println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            println("I/O Error: " + ex.getMessage());
        }

    }

    public void setCommandLine(MainActivity commandLine) {
        this.commandLine = commandLine;
    }
}
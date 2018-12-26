package com.netzwerk.savechat.client;

import com.netzwerk.savechat.Crypt;

import java.io.*;
import java.net.*;

public class ReadThread extends Thread {
    private BufferedReader reader;
    private Client client;
    private WriteThread writeThread;

    ReadThread(Socket socket, Client client, WriteThread writeThread) {
        this.client = client;
        this.writeThread = writeThread;
        try {
            InputStream input = socket.getInputStream();
            this.reader = new BufferedReader(new InputStreamReader(input));
        } catch (IOException ex) {
            client.println("Error getting input stream: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public void run() {
        while (true) {
            try {
                String response = reader.readLine();
                String message = Crypt.decrypt(response, client.prvkey, client);
                switch (message.charAt(0)) {
                    case 'm':
                        client.println(message.substring(1));
                        break;
                    case 'k':
                        client.ptrkey = Crypt.publicKeyFromBytes(Crypt.decode(message.substring(1)), client);
                        writeThread.newSecrets();
                        byte[] pubbytes = client.ptrkey.getEncoded();
                        byte[] pubbytes2 = client.pubkey.getEncoded();
                        byte[] hashbytes = new byte[pubbytes.length];
                        for (int i = 0; i < pubbytes.length; i++) hashbytes[i] = (byte) (pubbytes[i] ^ pubbytes2[i]);
                        client.println("Common fingerprint: " + Crypt.hash(hashbytes, client) + "\n\n");
                        break;
                    case 'e':
                        client.println(Crypt.decrypt(message.substring(1), client.prvkey, client));
                        break;
                }
            } catch (IOException ex) {
                client.println("Error reading from server: " + ex.getMessage());
                ex.printStackTrace();
                break;
            }
        }
    }
}

package com.netzwerk.savechat.client;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;

import com.netzwerk.savechat.Crypt;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;

public class WriteThread extends Thread {

    private PrintWriter writer;
    private Client client;
    private PublicKey svrkey;
    private SecretKey secretKey;
    private SecretKey secretServerKey;
    private Socket socket;

    WriteThread(Socket socket, Client client) {
        this.client = client;
        this.socket = socket;

        try {
            OutputStream output = socket.getOutputStream();
            writer = new PrintWriter(output, true);
        } catch (IOException ex) {
            client.println("Error getting output stream: " + ex.getMessage());
            ex.printStackTrace();
        }

        try {
            svrkey = Crypt.publicKeyFromBytes(readFile(new File(Environment.getExternalStorageDirectory().toString() + "/cmdchat/" + "svrkey")), client);
        } catch (IOException ex) {
            ex.printStackTrace();
            client.println("Error reading the server's public key.");
        }

        newSecrets();
    }

    private byte[] readFile(File file) throws IOException {
        int size = (int) file.length();
        byte[] bytes = new byte[size];
        FileInputStream inputStream = new FileInputStream(file);
        int readNow, readTotal = 0;
        while ((readNow = inputStream.read(bytes, readTotal, size - readTotal)) > 0) {
            readTotal += readNow;
        }
        inputStream.close();
        return bytes;
    }

    public void run() {
        // send key
        loadKeypair();

        // send pass
        loadUserIdentifier();

        // send console
        String text = "";
        do {
            try {
                text = client.reader.take();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            if (text.equals("!exit")) {
                try {
                    socket.close();
                    System.exit(0);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } else if (text.equals("!change")) {
                writer.println(Crypt.encrypt("p", svrkey, secretServerKey, client));
                client.ptrkey = null;
            } else if (text.length() > 8 && text.substring(0, 7).equals("!change")) {
                writer.println(Crypt.encrypt("p" + text.substring(8), svrkey, secretServerKey, client));
                client.ptrkey = null;
            } else if (client.ptrkey == null) {
                writer.println(Crypt.encrypt(text, svrkey, secretServerKey, client));
            } else {
                writer.println(Crypt.encrypt("e" + Crypt.encrypt(text, client.ptrkey, secretKey, client), svrkey, secretServerKey, client));
            }


        } while (true);
    }

    private void loadUserIdentifier() {
        String pass = "";
        File passFile = new File(Environment.getExternalStorageDirectory().toString() + "/cmdchat/" + "pass");
        if (passFile.exists()) {
            try {
                pass = new String(readFile(passFile));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
            SecureRandom random = new SecureRandom();
            char[] randomdata = new char[128];
            for (int i = 0; i < randomdata.length; i++) {
                randomdata[i] = (char) (32 + random.nextInt(90));
            }
            pass = new String(randomdata);
            try {
                writeFile(passFile, pass.getBytes());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        writer.println(Crypt.encrypt(pass, svrkey, secretServerKey, client));
    }


    private void loadKeypair() {
        File pubFile = new File(Environment.getExternalStorageDirectory().toString() + "/cmdchat/" + "pubkey");
        File prvFile = new File(Environment.getExternalStorageDirectory().toString() + "/cmdchat/" + "prvkey");
        if (pubFile.exists() && prvFile.exists()) {
            try {
                client.pubkey = Crypt.publicKeyFromBytes(readFile(pubFile), client);
                client.prvkey = Crypt.privateKeyFromBytes(readFile(prvFile), client);
            } catch (IOException ex) {
                client.println("Error in keypair.");
                System.exit(-1);
                ex.printStackTrace();
            }
        } else {
            try {
                KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");
                keygen.initialize(4096);
                KeyPair rsaKeys = keygen.genKeyPair();
                client.pubkey = rsaKeys.getPublic();
                client.prvkey = rsaKeys.getPrivate();

                writeFile(pubFile, client.pubkey.getEncoded());
                writeFile(prvFile, client.prvkey.getEncoded());
            } catch (NoSuchAlgorithmException ex) {
                client.println("This should never happen.");
                System.exit(-1);
            } catch (IOException ex) {
                client.println("Could not store keypair.");
                ex.printStackTrace();
            }
        }

        String encodedPubkey = Crypt.encode(client.pubkey.getEncoded());
        writer.println(Crypt.encrypt(encodedPubkey, svrkey, secretServerKey, client));
    }

    private void writeFile(File file, byte[] data) throws IOException {
        if (file.exists() || file.createNewFile()) {
            FileOutputStream outputStream = new FileOutputStream(file);
            outputStream.write(data);
            outputStream.close();
        }
    }

    void newSecrets() {
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(256);
            secretServerKey = generator.generateKey();
            secretKey = generator.generateKey();
        } catch (NoSuchAlgorithmException ex) {
            client.println("WTF how did this happen??! " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
package com.nerdyhomeproductions.commandlinechat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.netzwerk.savechat.client.Client;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    Client c;
    private static final int PERMISSION_STORAGE = 3218;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED ||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_STORAGE);
        else openClient();


        // c.add("37.120.187.17");
        // c.add("22713");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openClient();
                }
                break;
        }
    }

    private void openClient() {
        if (UglyInstanceKeeper.client == null) {
            c = new Client(this);
            c.start();
            UglyInstanceKeeper.client = c;
        } else {
            c = UglyInstanceKeeper.client;
            c.setCommandLine(this);
        }
        if (UglyInstanceKeeper.log == null) {
            UglyInstanceKeeper.log = new ArrayList<>();
        } else {
            for (String s : UglyInstanceKeeper.log) {
                TextView output = findViewById(R.id.output);
                output.append(s + "\n");
            }
        }
    }

    public void buttonSendClick(View v) {
        // get input and output
        TextView output = findViewById(R.id.output);
        EditText input = findViewById(R.id.input);
        // add input and newline to output
        output.append(input.getText() + "\n");
        output.scrollTo(0, output.getBottom());
        UglyInstanceKeeper.log.add(input.getText().toString());
        // forward input
        c.add(input.getText().toString());
        // clear input
        input.setText("");
    }

    public void add(String s) {
        final String text = s;
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView output = findViewById(R.id.output);
                output.append(text + "\n");
                output.scrollTo(0, output.getBottom());
            }
        });
        UglyInstanceKeeper.log.add(s);
    }

}

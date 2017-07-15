package com.example.victor.speedbluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;


public class SpeedActivity extends AppCompatActivity {

    BluetoothSocket mmSocket = null;
    BluetoothDevice mmDevice = null;
    Thread thread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speed);
        final TextView speedText = (TextView) findViewById(R.id.info);
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        final Handler handler = new Handler();

        /* This code together with the one in onDestroy()
         * will make the screen be always on until this Activity gets destroyed. */
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        speedText.setText("Connecting...");
        boolean connected = false;

        while(!connected) {
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    if (device.getName().equals("rpi3")) //Note, you will need to change this to match the name of your device
                    {
                        Log.e("SpeedBluetooth", device.getName());
                        mmDevice = device;
                        speedText.setText("Found rpi device!");
                        connected = true;
                        break;
                    }
                }
            }
            if (!connected)
            {
                speedText.setText("Can't find rpi device! Trying again");
            }
        }

        final class workerThread implements Runnable {
            public void run()
            {
                UUID uuid = UUID.fromString("94f39d09-7d6d-437d-973b-fba39e49d2ab"); //Do not change!
                try {
                    mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
                    if (!mmSocket.isConnected()) {
                        mmSocket.connect();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
                boolean workDone = false;
                while(!Thread.currentThread().isInterrupted())
                {
                    int bytesAvailable;
                    try {
                        if (workDone == true){
                            mmSocket.close();
                            handler.post(new Runnable()
                            {
                                public void run()
                                {
                                    speedText.setText("Finished!");
                                }
                            });
                            break;
                        }
                        final InputStream mmInputStream;
                        mmInputStream = mmSocket.getInputStream();
                        bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {

                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);

                            final String data = new String(packetBytes, "US-ASCII");
                            Log.w("readBluetooth", data);
                            //The variable data now contains our full command
                            handler.post(new Runnable()
                            {
                                public void run()
                                {
                                    speedText.setText(data);
                                }
                            });

                        }
                    } catch (IOException e) {
                        workDone = true;
                        e.printStackTrace();
                    }

                }
            }
        };

        if (thread==null || !thread.isAlive()) {
            thread = new Thread(new workerThread());
            thread.start();
        }
    }
}

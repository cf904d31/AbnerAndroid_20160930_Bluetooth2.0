package iii.org.tw.bluetooth20;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBluetoothAdapter ;
    private boolean isSupportBT = true;
    private boolean isBTInitEnabled = false , isBTEnabled = false;

    private ListView listDevices;
    private SimpleAdapter adapter;
    private String[] from = {"name","addr","type"};
    private int[] to = {R.id.item_name,R.id.item_addr,R.id.item_type};
    private LinkedList<HashMap<String,String>> data;
    private MyBTReceiver receiver;

    private UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    //-----此UUID是安卓手機通用的
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        1);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

        startBT();
        listDevices = (ListView)findViewById(R.id.listDevices);
        initListView();
        receiver = new MyBTReceiver();

    }



    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter); // Don't forget to unregister during onDestroy
    }

    @Override
    protected void onPause() {
        if (isSupportBT && mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }

        unregisterReceiver(receiver);
        super.onPause();
    }



    private void initListView() {
        data = new LinkedList<>();
        adapter = new SimpleAdapter(this,data,R.layout.layout_itemdevices,from,to);
        listDevices.setAdapter(adapter);
    }

    public void scanPaired(View v) {
        data.clear();

        //------Querying paired devices
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            HashMap<String,String> item = new HashMap<>();
            item.put(from[0],device.getName());
            item.put(from[1],device.getAddress());
            item.put(from[2],"已配對");
            data.add(item);
        }
        adapter.notifyDataSetChanged();


        //------Discovering devices
        mBluetoothAdapter.startDiscovery();
    }

    public void enableDiscoverability(View v) {
        Intent discoverableIntent = new
        Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);
    }

    private class MyBTReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (!isBTNameExists(device.getName())) {
                HashMap<String, String> item = new HashMap<>();
                item.put(from[0], device.getName());
                item.put(from[1], device.getAddress());
                item.put(from[2], "scan");
                data.add(item);
                adapter.notifyDataSetChanged();
            }
        }
    }


    private void startBT() {
        mBluetoothAdapter  = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter  == null) {
            // Device does not support Bluetooth
            isSupportBT = false;
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                isBTInitEnabled = true;
                isBTEnabled = true;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                isBTEnabled = true;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void finish() {
        if (isSupportBT && !isBTInitEnabled) {
            mBluetoothAdapter.disable();
        }
        super.finish();
    }

    private boolean isBTNameExists (String name) {
        boolean isExists = false;
        for (HashMap<String,String> devices : data) {
            if (devices.get(from[1]).equals(name)) {
                isExists = true;
                break;
            }
        }
        return isExists;
    }


    public void asServer(View v) {
        AcceptThread serverThread = new AcceptThread();
        serverThread.start();
    }


    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("Abner", MY_UUID);
            } catch (IOException e) { }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }
                // If a connection was accepted
//                if (socket != null) {
//                    // Do work to manage the connection (in a separate thread)
//                    manageConnectedSocket(socket);
//                    mmServerSocket.close();
//                    break;
//                }
            }
        }

        /** Will cancel the listening socket, and cause the thread to finish */
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) { }
        }
    }
}

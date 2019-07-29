

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.lang.*;

public class MainActivity extends AppCompatActivity {

    //Declaring the constants
    private static final int REQUEST_ENABLE_BT = 1001;
    private static final String TAG = "ConnectThread";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    //Variables required for Bluetooth Communication
    private BluetoothAdapter mBluetoothAdapter;
    public static BluetoothSocket mBluetoothSocket = null;
    InputStream mmInStream = null;
    private byte[] mmBuffer;// mmBuffer store for the stream
    //Boolean variable
    boolean stopThread;


    //Declaring arraylist and adapter
    private ArrayList<String> mArrayListMessages;
    private ArrayAdapter<String> mArrayAdapter;




    //UI Elements
    private ListView lv_paired_devices;
    private ProgressBar progressBar;
    private  TextView Sensortext1;
    private  TextView Sensortext2;
    private  TextView Sensortext3;
    private  TextView Sensortext4;
    private LinearLayout LL1;
    private ListView lv_chat;


    //Variables for storing and displaying paired devices
    private ArrayList<String> list_paired_devices;
    private ArrayAdapter<String> stringArrayAdapter;

    //Boolean variable to store connection status
    private boolean isBluetoothConnected = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mArrayListMessages = new ArrayList<>();
        mArrayAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, mArrayListMessages);

        Sensortext1=findViewById(R.id.text1);
        Sensortext2=findViewById(R.id.text2);
        Sensortext3=findViewById(R.id.text3);
        Sensortext4=findViewById(R.id.text4);
        LL1= findViewById(R.id.ll1);
        RelativeLayout rlayout = findViewById(R.id.mainlayout);
        Sensortext1.setVisibility(View.INVISIBLE); // Set visibility of textviews
        Sensortext2.setVisibility(View.INVISIBLE);
        Sensortext3.setVisibility(View.INVISIBLE);
        Sensortext4.setVisibility(View.INVISIBLE);
        rlayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LL1.setVisibility(View.VISIBLE);
                Sensortext1.setVisibility(View.INVISIBLE);// Set visibility of textviews
                Sensortext2.setVisibility(View.INVISIBLE);
                Sensortext3.setVisibility(View.INVISIBLE);
                Sensortext4.setVisibility(View.INVISIBLE);

            }
        });

        //Initialize all UI elements
        lv_paired_devices = findViewById(R.id.lv_paired_devices);
        //lv_chat = findViewById(R.id.lv_chat);
        progressBar = findViewById(R.id.progress_bluetooth);
        //Initialize ArrayList and ArrayAdapter
        list_paired_devices = new ArrayList<>();
        stringArrayAdapter = new ArrayAdapter<>(getApplicationContext()
                , android.R.layout.simple_list_item_1
                , list_paired_devices);


        //Initialize Bluetooth Adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //Enable Bluetooth if not enabled
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        //Get the MAC Address of clicked device from the listview
        lv_paired_devices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Get MAC Address and Name of clicked device
                String data = ((TextView) view).getText().toString().trim();
                String macAddress = data.substring(data.length() - 17);

                //Initiate the connection
                new ConnectBluetooth().execute(macAddress);
            }
        });
    }

    public void onClickListPairedDevices(View view) {
        //Clear the list of paired devices
        list_paired_devices.clear();
        lv_paired_devices.setVisibility(View.VISIBLE);

        //Get paired devices
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                //Get name and mac address of each device
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                String stringList = "Name: " + deviceName + "\n" + "MAC Address: " + deviceHardwareAddress;
                list_paired_devices.add(stringList);
            }

            //Set the list view with paired devices
            lv_paired_devices.setAdapter(stringArrayAdapter);
        }
    }

    public void onClickEnableBluetooth(View view) {
        //Enable Bluetooth if not enabled
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else
            Toast.makeText(this, "Bluetooth is already enabled.", Toast.LENGTH_SHORT).show();

    }
    public void onClickUpdateMessages(View view) {
        beginListenForData();//calling the function
        LL1.setVisibility(View.INVISIBLE);
        Sensortext1.setVisibility(View.VISIBLE);
        Sensortext2.setVisibility(View.VISIBLE);
        Sensortext3.setVisibility(View.VISIBLE);
        Sensortext4.setVisibility(View.VISIBLE);
        lv_paired_devices.setVisibility(View.INVISIBLE);



    }

    public class ConnectBluetooth extends AsyncTask<String, Void, Void> {
            //Background activity for bluetooth connection so that it doesn't interrupt UI thread
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(String... strings) {
            //Connect to another Bluetooth device
            try {
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(strings[0]);
                mBluetoothSocket = device.createRfcommSocketToServiceRecord(UUID.fromString(String.valueOf(MY_UUID)));
                mBluetoothAdapter.cancelDiscovery();
                mBluetoothSocket.connect();
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, e.getMessage());
                isBluetoothConnected = false;
            }
            return null;
        }


        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            //Toast a message to user
            if (isBluetoothConnected) {
                Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();
                list_paired_devices.clear();
                lv_paired_devices.setAdapter(stringArrayAdapter);
            } else
                Toast.makeText(MainActivity.this, "Connection Failed", Toast.LENGTH_SHORT).show();

            progressBar.setVisibility(View.INVISIBLE);
        }
    }
    void beginListenForData()
    {
        final Handler handler = new Handler();//declaration of handler to pass the data from this thread to UI thread
        stopThread = false;
        mmBuffer = new byte[1024];

        Thread thread  = new Thread(new Runnable() //new thread
        {
            public void run()
            {
                while (!Thread.currentThread().isInterrupted() && !stopThread) {

                    try {
                        mmInStream = mBluetoothSocket.getInputStream();// Instream to read the input data
                        mmBuffer = new byte[65536];//Initialization of mmBuffer
                        int numBytes;
                        if (mmInStream != null) {
                            final StringBuilder message = new StringBuilder(); // Declaring string builder to create a mutable string
                            numBytes = mmInStream.read(mmBuffer);

                            for (int i = 0; i < numBytes; i++) {
                                message.append(((char) Integer.parseInt(String.valueOf(mmBuffer[i]))));

                            }
                            handler.post(new Runnable() {
                                public void run()
                                {
                                    String a = message.toString();//Typecasting of stringbuilder to string

                                    String[] separated = a.split(":");//splitting of string
                                    if(separated.length==5) {
                                        String sensor1 = separated[0];
                                        Sensortext1.setText(" Sensor 1 = " + "  " + sensor1);
                                        String sensor2 = separated[1];
                                        Sensortext2.setText(" Sensor 2 = " + "  " + sensor2);
                                        String sensor3 = separated[2];
                                        Sensortext3.setText(" Sensor 3 = " + "  " + sensor3);
                                        String sensor4 = separated[3];
                                        Sensortext4.setText(" Sensor 1 = " + "  " + sensor4);
                                    }

                                }
                            });


                        } else {
                            Toast.makeText(MainActivity.this, "Input stream null", Toast.LENGTH_SHORT).show();//toast
                        }


                    } catch (IOException e) {
                        Log.d(TAG, "Input stream was disconnected", e);//log
                        stopThread = true;
                        break;
                    }
                }

            }
        });

        thread.start();
    }
}
trftask3main.txt
Displaying trftask3xml.txt.
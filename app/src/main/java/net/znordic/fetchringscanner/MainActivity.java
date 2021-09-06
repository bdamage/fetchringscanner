package net.znordic.fetchringscanner;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.ProfileManager;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements  ScannerMgr.DatawedgeListener, EMDKManager.EMDKListener {
    private static final String TAG = "EMDKtest";

    // Declare a variable to store ProfileManager object
    private ProfileManager profileManager = null;
    ScannerMgr scanner = null;

    // Declare a variable to store EMDKManager object
    private EMDKManager emdkManager = null;
    Map<String, String> outputData =  new HashMap<String, String>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //The EMDKManager object will be created and returned in the callback.
        EMDKResults results = EMDKManager.getEMDKManager(getApplicationContext(), this);

        if(scanner==null)
            scanner = new ScannerMgr(this);

        scanner.registerReceiver();
        scanner.createScannerProfileClean();

        //Check the return status of getEMDKManager
        if (results.statusCode == EMDKResults.STATUS_CODE.SUCCESS) {
            // EMDKManager object creation success


        } else {
            // EMDKManager object creation failed

        }

        Button btn = (Button) findViewById(R.id.btnQueryRS);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyProfile("TestRingScanner");
                scanner.enumerateScanners();
            }
        });



    }

    @Override
    public void onDatawedgeEvent(Intent intent) {

        if((scanner.scannerList != null) && (scanner.scannerList.size() > 0)) {
            for (Bundle bunb : scanner.scannerList){
                if(bunb.getBoolean("SCANNER_CONNECTION_STATE")==true) {
                    Log.d(TAG, "Identifier:" + bunb.getString("SCANNER_IDENTIFIER"));
                    TextView tv = (TextView) findViewById(R.id.textViewModel);
                    tv.setText( bunb.getString("SCANNER_NAME"));

                    outputData.put("Model",bunb.getString("SCANNER_NAME"));

                }
            }
            writeData();
        }
    }

    protected void writeData(){
        Log.d(TAG,"*****************************************");

        for(Map.Entry<String, String> entry : outputData.entrySet() ) {
            Log.d(TAG, "" + entry.getKey() +" : "+ entry.getValue());
        }
        Log.d(TAG,"*****************************************");
        StoreData storeData = new StoreData(this);
        storeData.write(StoreData.XML, outputData);
        storeData.write(StoreData.JSON, outputData);
        storeData.write(StoreData.INI, outputData);
    }



    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        //Clean up the objects created by EMDK manager
        emdkManager.release();

        scanner.unregisterReceiver();

    }
    @Override
    public void onClosed() {
        // TODO Auto-generated method stub
    }


    @Override
    public void onOpened(EMDKManager emdkManager) {
        // TODO Auto-generated method stub

        // This callback will be issued when the EMDK is ready to use.
        this.emdkManager = emdkManager;

// Get the ProfileManager object to process the profiles
        profileManager = (ProfileManager) emdkManager
                .getInstance(EMDKManager.FEATURE_TYPE.PROFILE);


        applyProfile("TestRingScanner");
        scanner.enumerateScanners();

    }

    public void applyProfile(String profileName) {

        if (profileManager != null) {
            String[] modifyData = new String[1];

// Call processPrfoile with profile name and SET flag to create the profile. The modifyData can be null.
            EMDKResults results = profileManager.processProfile(profileName,
                    ProfileManager.PROFILE_FLAG.SET, modifyData);

            if (results.statusCode == EMDKResults.STATUS_CODE.CHECK_XML) {
                // Get XML response as a String
                String statusXMLResponse = results.getStatusString();
                Log.d(TAG, statusXMLResponse);

                try {
                    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                    XmlPullParser parser = factory.newPullParser();
                    parser.setInput(new StringReader(statusXMLResponse));
                    // Call method to parse the response
                    //  parseXML(parser);

                    try {
                        int eventType = parser.getEventType();
                        while (eventType != XmlPullParser.END_DOCUMENT) {
                            //System.out.println(" tag "+parser.getName());
                            //check for "No connection" status
                            if(eventType == XmlPullParser.START_TAG && (parser.getName().compareTo("characteristic-query-error")==0)) {
                                int n = parser.getAttributeCount();
                                if(n>1) {
                                    Log.d(TAG, "Connection Status:" + parser.getAttributeValue(1));
                                    TextView tv = (TextView) findViewById(R.id.textViewSerial);
                                    tv.setText(parser.getAttributeValue(1));
                                }
                            }

                            //detect ring scanner serial and firmware
                            if(eventType == XmlPullParser.START_TAG && (parser.getName().compareTo("parm")==0)) {
                                //System.out.println("Start tag "+parser.getName());
                                //  System.out.println("attr count: "+parser.getAttributeCount() + "");

                                int n = parser.getAttributeCount();
                                if(n>0) {
                                    String attributeName = parser.getAttributeValue(0);
                                    //     System.out.println("att0: " +attributeName );

                                    if (attributeName.compareTo( "SerialNumber")==0) {
                                        Log.d(TAG, "RS Serial:" + parser.getAttributeValue(1));
                                        TextView tv = (TextView) findViewById(R.id.textViewSerial);
                                        tv.setText(parser.getAttributeValue(1));
                                        outputData.put("Serial",parser.getAttributeValue(1));

                                    }
                                    if (attributeName.compareTo( "FirmwareVersion")==0) {
                                        Log.d(TAG, "RS FirmwareVersion:" + parser.getAttributeValue(1));
                                        TextView tv = (TextView) findViewById(R.id.textViewFirmware);
                                        tv.setText(parser.getAttributeValue(1));
                                        outputData.put("Firmware",parser.getAttributeValue(1));
                                    }
                                }
                            }
                            eventType = parser.next();
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
//                    parser.getAttributeValue("","SerialNumber");
                } catch (XmlPullParserException e) {
                    e.printStackTrace();
                }

                // displayResults();
            } else {
                // Show dialog of Failure
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Failure");
                builder.setMessage("Failed to apply profile...")
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {

                                    }
                                });
                AlertDialog alert = builder.create();
                alert.show();
            }

        }

    }


}


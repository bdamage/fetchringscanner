package net.znordic.fetchringscanner;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.Set;

public class ScannerMgr extends BroadcastReceiver {

    public static final String TAG = "ScannerMgr";
    public String APP_PACKAGE_NAME = "com.zebra.datawedgetest";
    public static final String PROFILENAME = "FetchBTScanners";

    private static final long BEAM_TIMEOUT = 2000;
    protected Boolean barcodeScanned = false;
    private boolean barcodeScannedStarted = false;
    private long scanTime;
    ArrayList<Bundle> scannerList;

    public static final String enumeratedList = "com.symbol.datawedge.api.ACTION_ENUMERATEDSCANNERLIST";

    public static final String KEY_ENUMERATEDSCANNERLIST = "DWAPI_KEY_ENUMERATEDSCANNERLIST";
    public static final String NOTIFICATION = "com.symbol.datawedge.api.NOTIFICATION";
    public static final String NOTIFICATION_ACTION = "com.symbol.datawedge.api.NOTIFICATION_ACTION";
    public static final String NOTIFICATION_TYPE_SCANNER_STATUS = "SCANNER_STATUS";
    public static final String NOTIFICATION_TYPE_PROFILE_SWITCH = "PROFILE_SWITCH";
    public static final String NOTIFICATION_TYPE_CONFIGURATION_UPDATE = "CONFIGURATION_UPDATE";

    public static final String DATA_STRING_TAG = "com.symbol.datawedge.data_string";
    public static final String LABEL_TYPE = "com.symbol.datawedge.label_type";
    public static final String SOURCE_TAG = "com.symbol.datawedge.source";
    public static final String DECODE_DATA_TAG = "com.symbol.datawedge.decode_data";

    // http://techdocs.zebra.com/datawedge/6-5/guide/api/registerfornotification/

    DatawedgeListener mDatawedgeEvent;
    Context mContext;


    public ScannerMgr(Activity activity)  {
        mContext = activity.getApplicationContext();
        try {
            mDatawedgeEvent = (DatawedgeListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(this.toString()
                    + " must implement DatawedgeListener");
        }

        APP_PACKAGE_NAME = mContext.getPackageName();
        registerReceiver();
    }

    public void registerReceiver(BroadcastReceiver broadcastReceiver){
        IntentFilter filter = new IntentFilter();
        filter.addAction(enumeratedList);
        filter.addAction(APP_PACKAGE_NAME);
        filter.addAction(NOTIFICATION_ACTION); // SCANNER_STATUS
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        mContext.registerReceiver(broadcastReceiver, filter);
    }

    public void unregisterReceiver(BroadcastReceiver broadcastReceiver) {
        mContext.unregisterReceiver(broadcastReceiver);
    }


    public void unregisterReceiver() {
        Log.d(TAG, "UnregisterReceiver");
        // Register for notifications - SCANNER_STATUS
        Bundle b = new Bundle();
        b.putString("com.symbol.datawedge.api.APPLICATION_NAME", APP_PACKAGE_NAME);
        b.putString("com.symbol.datawedge.api.NOTIFICATION_TYPE", "SCANNER_STATUS");
        Intent i = new Intent();
        i.setAction("com.symbol.datawedge.api.ACTION");
        i.putExtra("com.symbol.datawedge.api.UNREGISTER_REGISTER_FOR_NOTIFICATION", b);//(1)
        mContext.sendBroadcast(i);

        mContext.unregisterReceiver(this);
    }

    public void registerReceiver() {
        Log.d(TAG, "RegisterReceiver");
        IntentFilter filter = new IntentFilter();
        filter.addAction(enumeratedList);
        filter.addAction(APP_PACKAGE_NAME);
        filter.addAction(NOTIFICATION_ACTION); // SCANNER_STATUS
        filter.addAction("com.symbol.datawedge.api.RESULT_ACTION");
        filter.addCategory(Intent.CATEGORY_DEFAULT);


        mContext.registerReceiver(this, filter);

    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        Log.d(TAG, "Action: " + action);
        if(action.equals("com.symbol.datawedge.api.RESULT_ACTION")) {


            if(intent.hasExtra("com.symbol.datawedge.api.RESULT_ENUMERATE_SCANNERS")) {
               scannerList = (ArrayList<Bundle>) intent.getSerializableExtra("com.symbol.datawedge.api.RESULT_ENUMERATE_SCANNERS");
                if((scannerList != null) && (scannerList.size() > 0)) {
                    for (Bundle bunb : scannerList){
                        String[] entry = new String[4];
                        entry[0] = bunb.getString("SCANNER_NAME");
                        entry[1] = bunb.getBoolean("SCANNER_CONNECTION_STATE")+"";
                        entry[2] = bunb.getInt("SCANNER_INDEX")+"";
                        entry[3] = bunb.getString("SCANNER_IDENTIFIER");

                        Log.d(TAG, "Scanner:" + entry[0]  + " Connection:" + entry[1] + " Index:" + entry[2] + " ID:" + entry[3]);

                    }
                     mDatawedgeEvent.onDatawedgeEvent(intent);
                }

            }


            if (intent.hasExtra("com.symbol.datawedge.api.RESULT_GET_VERSION_INFO")) {


                String text = null;
                String SimulScanVersion = "Not supported";
                String[] ScannerFirmware = {""};
                Bundle res = intent.getBundleExtra("com.symbol.datawedge.api.RESULT_GET_VERSION_INFO");
                String DWVersion = res.getString("DATAWEDGE");
                String BarcodeVersion = res.getString("BARCODE_SCANNING");
                String DecoderVersion = res.getString("DECODER_LIBRARY");

                Set<String> bundleKeySet = res.keySet(); // string key set
                for(String key : bundleKeySet){ // traverse and print pairs
                    Log.i(TAG,key +"  : " + res.get(key));
                }


                if (res.containsKey("SCANNER_FIRMWARE")) {
                    ScannerFirmware = res.getStringArray("SCANNER_FIRMWARE");
                }

                if (res.containsKey("SIMULSCAN")) {
                    SimulScanVersion = res.getString("SIMULSCAN");
                }

                text = "DataWedge:" + DWVersion + "\nDecoderLib:" + DecoderVersion + "\nFirmware:";

                if (ScannerFirmware != null) {
                    for (String s : ScannerFirmware) {
                        text += "\n" + s;
                    }
                }
                text += "\nBarcodescan:" + BarcodeVersion + "\nSimulscan:" + SimulScanVersion;
                Log.d(TAG, text);
            }
        }
      //  Toast.makeText(context, text, Toast.LENGTH_LONG).show();



        if (action.equals(APP_PACKAGE_NAME)) {
          //  mDatawedgeEvent.onDatawedgeEvent(intent);
        } else if (action.equals(ScannerMgr.NOTIFICATION_ACTION)) {
            if (intent.hasExtra(NOTIFICATION)) {
                Bundle b = intent.getBundleExtra(NOTIFICATION);
                String NOTIFICATION_TYPE = b.getString("NOTIFICATION_TYPE");
                if (NOTIFICATION_TYPE != null) {
                    switch (NOTIFICATION_TYPE) {
                        case ScannerMgr.NOTIFICATION_TYPE_SCANNER_STATUS:

                            Log.d(TAG, "SCANNER_STATUS: status: " + b.getString("STATUS") + ", profileName: " + b.getString("PROFILE_NAME"));
                            String scanner_status = b.getString("STATUS");
                            if (scanner_status.equalsIgnoreCase("WAITING")) {
                                // check if barcode scan was started and timed out
                                if (!barcodeScanned && barcodeScannedStarted && (System.currentTimeMillis() - scanTime >= BEAM_TIMEOUT)) {
                                    //Toast.makeText(getApplicationContext(), "SCAN TIMEOUT", Toast.LENGTH_SHORT).show();

                                }


                            }
                            if (scanner_status.equalsIgnoreCase("SCANNING")) {
                                barcodeScanned = false;
                                barcodeScannedStarted = true;
                                scanTime = System.currentTimeMillis();
                            }
                            break;

                        case ScannerMgr.NOTIFICATION_TYPE_PROFILE_SWITCH:
                            Log.d(TAG, "PROFILE_SWITCH: profileName: " + b.getString("PROFILE_NAME") + ", profileEnabled: " + b.getBoolean("PROFILE_ENABLED"));
                            break;

                        case ScannerMgr.NOTIFICATION_TYPE_CONFIGURATION_UPDATE:
                            break;
                    }
                }
            }
        }


    }


    public void enumerateScanners(){
        Intent i = new Intent();
        i.setAction("com.symbol.datawedge.api.ACTION");
        i.putExtra("com.symbol.datawedge.api.ENUMERATE_SCANNERS", "");
        mContext.sendBroadcast(i);
    }


    public void getVersions(){
        Intent i = new Intent();
        i.setAction("com.symbol.datawedge.api.ACTION");
        i.putExtra("com.symbol.datawedge.api.GET_VERSION_INFO", "");
        mContext.sendBroadcast(i);
    }

    public void getConfig(){
        Bundle bMain = new Bundle();
        bMain.putString("PROFILE_NAME", PROFILENAME);
        Bundle bConfig = new Bundle();
        ArrayList<Bundle> pluginName = new ArrayList<>();

        Bundle pluginInternal = new Bundle();
        pluginInternal.putString("PLUGIN_NAME", "BARCODE");//can put a list "ADF,BDF"
        pluginInternal.putString("OUTPUT_PLUGIN_NAME","BARCODE");
        pluginName.add(pluginInternal);
        bConfig.putParcelableArrayList("PROCESS_PLUGIN_NAME", pluginName);
        bMain.putBundle("PLUGIN_CONFIG", bConfig);

        Intent i = new Intent();
        i.setAction("com.symbol.datawedge.api.ACTION");
        i.putExtra("com.symbol.datawedge.api.GET_CONFIG", bMain);
        mContext.sendBroadcast(i);
    }


    public void createScannerProfileClean() {

        Bundle bMain = new Bundle();
        bMain.putString("PROFILE_NAME", PROFILENAME);
        bMain.putString("PROFILE_ENABLED", "true");
        bMain.putString("CONFIG_MODE", "CREATE_IF_NOT_EXIST");
        Bundle bConfig = new Bundle();
        bConfig.putString("PLUGIN_NAME", "INTENT");
        // bConfig.putString("RESET_CONFIG","true");
        Bundle bParams = new Bundle();
        bParams.putString("intent_output_enabled", "true");
        bParams.putString("intent_action", APP_PACKAGE_NAME);
        bParams.putString("intent_category", "android.intent.category.DEFAULT");
        bParams.putString("intent_delivery", "2");
        bConfig.putBundle("PARAM_LIST", bParams);
        bMain.putBundle("PLUGIN_CONFIG", bConfig);

        Intent i = new Intent();
        i.setAction("com.symbol.datawedge.api.ACTION");
        i.putExtra("com.symbol.datawedge.api.SET_CONFIG", bMain);
        mContext.sendBroadcast(i);


        bConfig.putBundle("PARAM_LIST", bParams);
        bMain.putBundle("PLUGIN_CONFIG", bConfig);
        i.setAction("com.symbol.datawedge.api.ACTION");
        i.putExtra("com.symbol.datawedge.api.SET_CONFIG", bMain);
        mContext.sendBroadcast(i);

        // Disable keystroke
        bMain.putString("PROFILE_NAME", PROFILENAME);
        bMain.putString("CONFIG_MODE", "UPDATE");
        bConfig.putString("PLUGIN_NAME", "KEYSTROKE");
        bParams.putString("keystroke_output_enabled","false");
        bConfig.putBundle("PARAM_LIST", bParams);


        Bundle bundleApp = new Bundle();
        bundleApp.putString("PACKAGE_NAME",APP_PACKAGE_NAME);
        bundleApp.putStringArray("ACTIVITY_LIST", new String[]{"*"});

// NEXT APP_LIST BUNDLE(S) INTO THE MAIN BUNDLE
        bMain.putParcelableArray("APP_LIST", new Bundle[]{
                bundleApp
        });

        //PUT bConfig into bMain
        bMain.putBundle("PLUGIN_CONFIG", bConfig);
        i.setAction("com.symbol.datawedge.api.ACTION");
        i.putExtra("com.symbol.datawedge.api.SET_CONFIG", bMain);
        mContext.sendBroadcast(i);

        // Register for notifications - SCANNER_STATUS
        Bundle b = new Bundle();
        b.putString("com.symbol.datawedge.api.APPLICATION_NAME", APP_PACKAGE_NAME);
        b.putString("com.symbol.datawedge.api.NOTIFICATION_TYPE", "SCANNER_STATUS");
        i = new Intent();
        i.setAction("com.symbol.datawedge.api.ACTION");
        i.putExtra("com.symbol.datawedge.api.REGISTER_FOR_NOTIFICATION", b);//(1)
        mContext.sendBroadcast(i);
    }


    void softTrigger(){

        Intent i = new Intent();
        i.setAction("com.symbol.datawedge.api.ACTION");
        i.putExtra("com.symbol.datawedge.api.SOFT_SCAN_TRIGGER", "START_SCANNING");

        // send the intent to DataWedge
        mContext.sendBroadcast(i);
    }

    void enableDatawedge(boolean active)
    {

        Intent i = new Intent();
        i.setAction("com.symbol.datawedge.api.ACTION");
        i.putExtra("com.symbol.datawedge.api.ENABLE_DATAWEDGE", active);

    }

    // Container Activity must implement this interface
    public interface DatawedgeListener {
        public void onDatawedgeEvent(Intent intent);
    }


}

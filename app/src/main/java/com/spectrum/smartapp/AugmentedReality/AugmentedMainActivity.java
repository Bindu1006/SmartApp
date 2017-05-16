package com.spectrum.smartapp.AugmentedReality;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.spectrum.smartapp.LightsActivity;
import com.spectrum.smartapp.LoginActivity;
import com.spectrum.smartapp.MainActivity;
import com.spectrum.smartapp.R;
import com.spectrum.smartapp.SmartDevice.AlarmReceiver;
import com.spectrum.smartapp.Utils.DatabaseSqlHelper;
import com.wikitude.architect.ArchitectStartupConfiguration;
import com.wikitude.architect.ArchitectView;
import com.wikitude.tools.device.features.MissingDeviceFeatures;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AugmentedMainActivity extends AppCompatActivity  {
    private Map<Integer, List<SampleMeta>> samples;

    private Set<String> irSamples;
    private Set<String> geoSamples;
    private Set<String> instantSamples;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        irSamples = getListFrom("samples/samples_ir.lst");
        geoSamples = getListFrom("samples/samples_geo.lst");
        instantSamples = getListFrom("samples/samples_instant.lst");

            // ensure to clean cache when it is no longer required
        ArchitectView.deleteRootCacheDirectory(this);

            // extract names of samples from res/arrays
        final String[] values = this.getListLabels();

        // use default list-ArrayAdapter */
//        this.setListAdapter( new ArrayAdapter<String>( this, android.R.layout.simple_list_item_1, android.R.id.text1, values ) );

    }

    @Override
    protected void onStart() {
        super.onStart();

        android.support.v7.app.AlertDialog alertDialog = new android.support.v7.app.AlertDialog.Builder(AugmentedMainActivity.this).create();

        DatabaseSqlHelper databaseHelper = new DatabaseSqlHelper(AugmentedMainActivity.this);
        if (databaseHelper.getDeviceCount() == 0) {
            alertDialog.setTitle("Error");
            alertDialog.setMessage("You do not have any device added!!! Please add devices to enjoy Augmented Reality.");
            alertDialog.setButton(android.support.v7.app.AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            Log.d("SHRUTI", "Going back to Main activity ");
                            Intent intent = new Intent(AugmentedMainActivity.this, MainActivity.class);
                            startActivity(intent);
                        }
                    });
            alertDialog.show();
        } else {

            final Intent intent = new Intent( this, MainSamplesListActivity.class );
            final List<SampleMeta> activitiesToLaunch = getActivitiesToLaunch();

            String[] activityUrls = new String[activitiesToLaunch.size()];
            String[] activityTitles = new String[activitiesToLaunch.size()];
            String[] activityClasses = new String[activitiesToLaunch.size()];
            boolean[] activitiesIr = new boolean[activitiesToLaunch.size()];
            boolean[] activitiesGeo = new boolean[activitiesToLaunch.size()];
            boolean[] activitiesInstant = new boolean[activitiesToLaunch.size()];
            final String activityTitle = activitiesToLaunch.get(0).categoryName.replace("$", " ");

//         check if AR.VideoDrawables are supported on the current device. if not -> show hint-Toast message
            if (activitiesToLaunch.get(0).categoryName.contains("Video") && ! AugmentedMainActivity.isVideoDrawablesSupported()) {
                Toast.makeText(this, R.string.videosrawables_fallback, Toast.LENGTH_LONG).show();
            }

            final SampleMeta meta = activitiesToLaunch.get(0);
            activityTitles[0] = (meta.sampleName.replace("$", " "));
            activityUrls[0] = meta.path;
            activitiesIr[0] = meta.hasIr;
            activitiesGeo[0] = meta.hasGeo;
            activitiesInstant[0] = meta.hasInstant;
            activityClasses[0] = ("com.spectrum.smartapp.AugmentedReality.AutoHdSampleCamActivity");


            intent.putExtra(MainSamplesListActivity.EXTRAS_KEY_ACTIVITIES_ARCHITECT_WORLD_URLS_ARRAY, activityUrls);
            intent.putExtra(MainSamplesListActivity.EXTRAS_KEY_ACTIVITIES_CLASSNAMES_ARRAY, activityClasses);
            intent.putExtra(MainSamplesListActivity.EXTRAS_KEY_ACTIVITIES_TILES_ARRAY, activityTitles);
            intent.putExtra(MainSamplesListActivity.EXTRAS_KEY_ACTIVITIES_IR_ARRAY, activitiesIr);
            intent.putExtra(MainSamplesListActivity.EXTRAS_KEY_ACTIVITIES_GEO_ARRAY, activitiesGeo);
            intent.putExtra(MainSamplesListActivity.EXTRAS_KEY_ACTIVITIES_INSTANT_ARRAY, activitiesInstant);
            intent.putExtra(MainSamplesListActivity.EXTRAS_KEY_ACTIVITY_TITLE_STRING, activityTitle);

			/* launch activity */
            this.startActivity( intent );

        }




    }

    private Set<String> getListFrom(String fname) {
        HashSet<String> data = new HashSet<String>();
        try {
            BufferedReader burr = new BufferedReader(new InputStreamReader(getAssets().open(fname)));
            String line;
            while ((line = burr.readLine()) != null) {
                data.add(line);
            }
            burr.close();
        } catch (FileNotFoundException e) {
            Log.w("Wikitude SDK Samples", "Can't read list from file " + fname);
        } catch (IOException e) {
            Log.w("Wikitude SDK Samples", "Can't read list from file " + fname);
        }
        return data;
    }

//    @Override
//    protected void onListItemClick(ListView l, View v, int position, long id ) {
//        super.onListItemClick( l, v, position, id );
//
//        final Intent intent = new Intent( this, MainSamplesListActivity.class );
//
//        final List<SampleMeta> activitiesToLaunch = getActivitiesToLaunch();
//        for (SampleMeta s : activitiesToLaunch) {
//            Log.d("SHRUTI", s.toString());
//        }
//        final String activityTitle = activitiesToLaunch.get(0).categoryName.replace("$", " ");
//        String[] activityTitles = new String[activitiesToLaunch.size()];
//        String[] activityUrls = new String[activitiesToLaunch.size()];
//        String[] activityClasses = new String[activitiesToLaunch.size()];
//
//        boolean[] activitiesIr = new boolean[activitiesToLaunch.size()];
//        boolean[] activitiesGeo = new boolean[activitiesToLaunch.size()];
//        boolean[] activitiesInstant = new boolean[activitiesToLaunch.size()];
//
//        // check if AR.VideoDrawables are supported on the current device. if not -> show hint-Toast message
//        if (activitiesToLaunch.get(0).categoryName.contains("Video") && ! AugmentedMainActivity.isVideoDrawablesSupported()) {
//            Toast.makeText(this, R.string.videosrawables_fallback, Toast.LENGTH_LONG).show();
//        }
//
//        // find out which Activity to launch when sample row was pressed, some handle AR.platform.sendJSONObject, others inject poi data from native via javascript
//        for (int i= 0; i< activitiesToLaunch.size(); i++) {
//            final SampleMeta meta = activitiesToLaunch.get(i);
//
//            activityTitles[i] = (meta.sampleName.replace("$", " "));
//            activityUrls[i] = meta.path;
//            activitiesIr[i] = meta.hasIr;
//            activitiesGeo[i] = meta.hasGeo;
//            activitiesInstant[i] = meta.hasInstant;
//
//            if (meta.categoryId.equals("02") && meta.sampleId==3) {
//                activityClasses[i] = ("com.wikitude.samples.SampleCamActivity");
//            } else if (meta.categoryId.equals("03")) {
//                activityClasses[i] = ("com.wikitude.samples.SampleCamActivity");
//            } else if (meta.categoryId.equals("04")){
//                activityClasses[i] = ("com.wikitude.samples.SampleCamActivity");
//            } else if (meta.categoryId.equals("07") && meta.sampleId==1) {
//                activityClasses[i] = ("com.wikitude.samples.SampleCamContentFromNativeActivity");
//            } else if (meta.categoryId.equals("10") && meta.sampleId==1) {
//                activityClasses[i] = ("com.wikitude.samples.SampleFrontCamActivity");
//            } else if (meta.categoryId.equals("10") && meta.sampleId==3) {
//                activityClasses[i] = ("com.spectrum.smartapp.AugmentedReality.SampleCam2Activity");
//            } else if (meta.categoryId.equals("11") && meta.sampleId==1) {
//                activityClasses[i] = ("com.wikitude.samples.SamplePluginActivity");
//            } else if (meta.categoryId.equals("11") && meta.sampleId==2) {
//                activityClasses[i] = ("com.wikitude.samples.FaceDetectionPluginActivity");
//            } else if (meta.categoryId.equals("11") && meta.sampleId==3) {
//                activityClasses[i] = ("com.wikitude.samples.SimpleInputPluginActivity");
//            } else if (meta.categoryId.equals("11") && meta.sampleId==4) {
//                activityClasses[i] = ("com.wikitude.samples.CustomCameraActivity");
//            } else if (meta.categoryId.equals("11") && meta.sampleId==5) {
//                activityClasses[i] = ("com.wikitude.samples.MarkerTrackingPluginActivity");
//            } else {
//                activityClasses[i] = ("com.spectrum.smartapp.AugmentedReality.AutoHdSampleCamActivity");
//            }
//        }
//
//        intent.putExtra(MainSamplesListActivity.EXTRAS_KEY_ACTIVITIES_ARCHITECT_WORLD_URLS_ARRAY, activityUrls);
//        intent.putExtra(MainSamplesListActivity.EXTRAS_KEY_ACTIVITIES_CLASSNAMES_ARRAY, activityClasses);
//        intent.putExtra(MainSamplesListActivity.EXTRAS_KEY_ACTIVITIES_TILES_ARRAY, activityTitles);
//        intent.putExtra(MainSamplesListActivity.EXTRAS_KEY_ACTIVITIES_IR_ARRAY, activitiesIr);
//        intent.putExtra(MainSamplesListActivity.EXTRAS_KEY_ACTIVITIES_GEO_ARRAY, activitiesGeo);
//        intent.putExtra(MainSamplesListActivity.EXTRAS_KEY_ACTIVITIES_INSTANT_ARRAY, activitiesInstant);
//        intent.putExtra(MainSamplesListActivity.EXTRAS_KEY_ACTIVITY_TITLE_STRING, activityTitle);
//
//			/* launch activity */
//        this.startActivity( intent );
//
//    }

    protected final String[] getListLabels() {
        boolean includeIR = (ArchitectView.getSupportedFeaturesForDevice(getApplicationContext()) & ArchitectStartupConfiguration.Features.ImageTracking) != 0;
        boolean includeGeo = (ArchitectView.getSupportedFeaturesForDevice(getApplicationContext()) & ArchitectStartupConfiguration.Features.Geo) != 0;
        boolean includeInstant = (ArchitectView.getSupportedFeaturesForDevice(getApplicationContext()) & ArchitectStartupConfiguration.Features.InstantTracking) != 0;

        MissingDeviceFeatures missingDeviceFeatures = ArchitectView.isDeviceSupported(this,
                ArchitectStartupConfiguration.Features.ImageTracking | ArchitectStartupConfiguration.Features.Geo | ArchitectStartupConfiguration.Features.InstantTracking);

        if (missingDeviceFeatures.areFeaturesMissing()) {
            Toast toast =  Toast.makeText(this, missingDeviceFeatures.getMissingFeatureMessage() +
                    "Because of this some samples may not be visible.", Toast.LENGTH_LONG);
            toast.show();
        }

        samples = getActivitiesToLaunch(includeIR, includeGeo, includeInstant);
        final String[] labels = new String[samples.keySet().size()];
        for (int i = 0; i<labels.length; i++) {
            labels[i] = samples.get(i).get(0).categoryName.replace("$", " ");
        }
        return labels;
    }

    protected int getContentViewId() {
        return R.layout.list_startscreen;
    }

    public void buttonClicked(final View view)
    {
        try {
            this.startActivity( new Intent( this, Class.forName( "com.wikitude.samples.utils.urllauncher.ARchitectUrlLauncherActivity" ) ) );
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private List<SampleMeta> getActivitiesToLaunch(){
        return samples.get(0);
    }

    private Map<Integer, List<SampleMeta>> getActivitiesToLaunch(boolean includeIR, boolean includeGeo, boolean includeInstant){
        final Map<Integer, List<SampleMeta>> pos2activites = new HashMap<Integer, List<SampleMeta>>();

        String[] assetsIWant;

        try {
            assetsIWant = getAssets().list("samples");
            int pos = -1;
            String lastCategoryId = "";
            for(final String asset: assetsIWant) {
                if (!asset.substring(asset.length() - 4).contains(".")) {
                    try {
                        // don't include sample if it requires IR functionality on
                        // devices which don't support it.
                        boolean needIr = irSamples.contains(asset);
                        boolean needGeo = geoSamples.contains(asset);
                        boolean needInstant = instantSamples.contains(asset);
                        if ((includeIR || !needIr) && (includeGeo || !needGeo) && (includeInstant || !needInstant)) {
                            SampleMeta sampleMeta = new SampleMeta(asset, needIr, needGeo, needInstant);
                            if (!sampleMeta.categoryId.equals(lastCategoryId)) {
                                pos++;
                                pos2activites.put(pos, new ArrayList<SampleMeta>());
                            }
                            pos2activites.get(pos).add(sampleMeta);
                            lastCategoryId = sampleMeta.categoryId;
                        }
                    } catch (IllegalArgumentException e) {
                        // Log.e("Ignored Asset to load", asset + " invalid: "+ e.getMessage());
                    }
                }
            }

            return pos2activites;


        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static class SampleMeta {


        final String path, categoryName, sampleName, categoryId;
        final int sampleId;
        final boolean hasGeo, hasIr, hasInstant;

        public SampleMeta(String path, boolean hasIr, boolean hasGeo, boolean hasInstant) {
            super();
            this.path = path;
            this.hasGeo = hasGeo;
            this.hasIr = hasIr;
            this.hasInstant = hasInstant;
            if (path.indexOf("_")<0) {
                throw new IllegalArgumentException("all files in asset folder must be folders and define category and subcategory as predefined (with underscore)");
            }
//            this.categoryId = path.substring(0, path.indexOf("_"));
//            path = path.substring(path.indexOf("_")+1);
//            this.categoryName = path.substring(0, path.indexOf("_"));
//            path = path.substring(path.indexOf("_")+1);
//            this.sampleId = Integer.parseInt(path.substring(0, path.indexOf("_")));
//            path = path.substring(path.indexOf("_")+1);
//            this.sampleName = path;

            this.categoryId = "01";
            this.categoryName = "Image$Recognition";
            this.sampleId = 1;
            this.sampleName = "01_Image$Recognition_1_Image$On$Target";
        }

        @Override
        public String toString() {
            return "categoryId:" + this.categoryId + ", categoryName:" + this.categoryName + ", sampleId:" + this.sampleId +", sampleName: " + this.sampleName + ", path: " + this.path;
        }
    }

    /**
     * helper to check if video-drawables are supported by this device. recommended to check before launching ARchitect Worlds with videodrawables
     * @return true if AR.VideoDrawables are supported, false if fallback rendering would apply (= show video fullscreen)
     */
    public static final boolean isVideoDrawablesSupported() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Lollipop: assume it's ok
            // because creating a new GL context only to check this extension is overkill
            return true;
        } else {
            String extensions = GLES20.glGetString( GLES20.GL_EXTENSIONS );
            return extensions != null && extensions.contains( "GL_OES_EGL_image_external" );
        }
    }

    @Override
    public void onBackPressed() {
        Log.d("hi","hellos");
        Intent intent = new Intent(getBaseContext(), LightsActivity.class);
        startActivity(intent);
    }

}


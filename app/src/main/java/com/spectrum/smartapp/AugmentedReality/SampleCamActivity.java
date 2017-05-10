package com.spectrum.smartapp.AugmentedReality;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.SensorManager;
import android.location.LocationListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotKeystoreHelper;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttLastWillAndTestament;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.AttachPrincipalPolicyRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateResult;
import com.spectrum.smartapp.R;
import com.spectrum.smartapp.SmartDevice.AlarmReceiver;
import com.spectrum.smartapp.Utils.AmazonClientManager;
import com.spectrum.smartapp.Utils.DatabaseSqlHelper;
import com.wikitude.architect.ArchitectJavaScriptInterfaceListener;
import com.wikitude.architect.ArchitectView;
import com.wikitude.architect.ArchitectView.CaptureScreenCallback;
import com.wikitude.architect.ArchitectView.SensorAccuracyChangeListener;
import com.wikitude.common.camera.CameraSettings;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.util.UUID;

public class SampleCamActivity extends AbstractArchitectCamActivity {

	private static final String TAG = "SampleCamActivity";
	/**
	 * last time the calibration toast was shown, this avoids too many toast shown when compass needs calibration
	 */
	private long lastCalibrationToastShownTimeMillis = System.currentTimeMillis();

    protected Bitmap screenCapture = null;

    private static final int WIKITUDE_PERMISSIONS_REQUEST_EXTERNAL_STORAGE = 3;

	//AWS keys
	private static final String CUSTOMER_SPECIFIC_ENDPOINT = "a1hazupsm4spyo.iot.us-west-2.amazonaws.com";
	private static final String COGNITO_POOL_ID = "us-west-2:2b9118fd-a78f-435f-bb53-39ad064584e1";
	private static final String AWS_IOT_POLICY_NAME = "thing_policy";
	private static final Regions MY_REGION = Regions.US_WEST_2;
	private static final String KEYSTORE_NAME = "iot_keystore";
	private static final String KEYSTORE_PASSWORD = "password";
	private static final String CERTIFICATE_ID = "default";
	AWSIotMqttManager mqttManager;
	String clientId;
	AWSIotClient mIotAndroidClient;
	String keystorePath;
	String keystoreName;
	String keystorePassword;
	String certificateId;
	KeyStore clientKeyStore = null;
	public static AmazonClientManager clientManager = null;
	CognitoCachingCredentialsProvider credentialsProvider;

	String ipString;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		InitializeAmazonPubSubTask task = new InitializeAmazonPubSubTask();
		task.execute();
	}

	@Override
	public String getARchitectWorldPath() {
		return getIntent().getExtras().getString(
				MainSamplesListActivity.EXTRAS_KEY_ACTIVITY_ARCHITECT_WORLD_URL);
	}

	@Override
	public String getActivityTitle() {
		return (getIntent().getExtras() != null && getIntent().getExtras().get(
				MainSamplesListActivity.EXTRAS_KEY_ACTIVITY_TITLE_STRING) != null) ? getIntent()
				.getExtras().getString(MainSamplesListActivity.EXTRAS_KEY_ACTIVITY_TITLE_STRING)
				: "Test-World";
	}

	@Override
	public int getContentViewId() {
		return R.layout.augmented_cam;
	}

	@Override
	public int getArchitectViewId() {
		return R.id.architectView;
	}
	
	@Override
	public String getWikitudeSDKLicenseKey() {
		return WikitudeSDKConstants.WIKITUDE_SDK_KEY;
	}
	
	@Override
	public SensorAccuracyChangeListener getSensorAccuracyListener() {
		return new SensorAccuracyChangeListener() {
			@Override
			public void onCompassAccuracyChanged( int accuracy ) {
				/* UNRELIABLE = 0, LOW = 1, MEDIUM = 2, HIGH = 3 */
				if ( accuracy < SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM && SampleCamActivity.this != null && !SampleCamActivity.this.isFinishing() && System.currentTimeMillis() - SampleCamActivity.this.lastCalibrationToastShownTimeMillis > 5 * 1000) {
					Toast.makeText( SampleCamActivity.this, R.string.compass_accuracy_low, Toast.LENGTH_LONG ).show();
					SampleCamActivity.this.lastCalibrationToastShownTimeMillis = System.currentTimeMillis();
				}
			}
		};
	}

	@Override
	public ArchitectJavaScriptInterfaceListener getArchitectJavaScriptInterfaceListener() {
		return new ArchitectJavaScriptInterfaceListener() {
			@Override
			public void onJSONObjectReceived(JSONObject jsonObject) {
				try {
					if (jsonObject.getString("action") != null && jsonObject.getString("ip") != null) {

						String action = jsonObject.getString("action");
						ipString = jsonObject.getString("ip");
						ConnectAmazonPubSubTask connectTask = new ConnectAmazonPubSubTask();
						connectTask.execute();



					}
//					switch (jsonObject.getString("action")) {
//						case "present_poi_details":
//							final Intent poiDetailIntent = new Intent(SampleCamActivity.this, SamplePoiDetailActivity.class);
//							poiDetailIntent.putExtra(SamplePoiDetailActivity.EXTRAS_KEY_POI_ID, jsonObject.getString("id"));
//							poiDetailIntent.putExtra(SamplePoiDetailActivity.EXTRAS_KEY_POI_TITILE, jsonObject.getString("title"));
//							poiDetailIntent.putExtra(SamplePoiDetailActivity.EXTRAS_KEY_POI_DESCR, jsonObject.getString("description"));
//							SampleCamActivity.this.startActivity(poiDetailIntent);
//							break;
//
//						case "capture_screen":
//							SampleCamActivity.this.architectView.captureScreen(ArchitectView.CaptureScreenCallback.CAPTURE_MODE_CAM_AND_WEBVIEW, new CaptureScreenCallback() {
//								@Override
//								public void onScreenCaptured(final Bitmap screenCapture) {
//									if ( ContextCompat.checkSelfPermission(SampleCamActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) {
//										SampleCamActivity.this.screenCapture = screenCapture;
//										ActivityCompat.requestPermissions(SampleCamActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WIKITUDE_PERMISSIONS_REQUEST_EXTERNAL_STORAGE);
//									} else {
//										SampleCamActivity.this.saveScreenCaptureToExternalStorage(screenCapture);
//									}
//								}
//							});
//							break;
//						default:
//							Log.e(TAG, "onJSONObjectReceived: ");
//					}
				} catch (JSONException e) {
					Log.e(TAG, "onJSONObjectReceived: ", e);
				}
			}
		};
	}

	@Override
	public ArchitectView.ArchitectWorldLoadedListener getWorldLoadedListener() {
		return new ArchitectView.ArchitectWorldLoadedListener() {
			@Override
			public void worldWasLoaded(String url) {
				Log.i(TAG, "worldWasLoaded: url: " + url);
			}

			@Override
			public void worldLoadFailed(int errorCode, String description, String failingUrl) {
				Log.e(TAG, "worldLoadFailed: url: " + failingUrl + " " + description);
			}
		};
	}

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case WIKITUDE_PERMISSIONS_REQUEST_EXTERNAL_STORAGE: {
                if ( grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED ) {
                    this.saveScreenCaptureToExternalStorage(SampleCamActivity.this.screenCapture);
                } else {
                    Toast.makeText(this, "Please allow access to external storage, otherwise the screen capture can not be saved.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

	@Override
	public ILocationProvider getLocationProvider(final LocationListener locationListener) {
		return new LocationProvider(this, locationListener);
	}
	
	@Override
	public float getInitialCullingDistanceMeters() {
		// you need to adjust this in case your POIs are more than 50km away from user here while loading or in JS code (compare 'AR.context.scene.cullingDistance')
		return ArchitectViewHolderInterface.CULLING_DISTANCE_DEFAULT_METERS;
	}

	@Override
	protected boolean hasGeo() {
		return getIntent().getExtras().getBoolean(
				MainSamplesListActivity.EXTRAS_KEY_ACTIVITY_GEO);
	}

	@Override
	protected boolean hasIR() {
		return getIntent().getExtras().getBoolean(
				MainSamplesListActivity.EXTRAS_KEY_ACTIVITY_IR);
	}

	@Override
	protected boolean hasInstant() {
		return getIntent().getExtras().getBoolean(
				MainSamplesListActivity.EXTRAS_KEY_ACTIVITY_INSTANT);
	}

	@Override
	protected CameraSettings.CameraPosition getCameraPosition() {
		return CameraSettings.CameraPosition.DEFAULT;
	}

    protected void saveScreenCaptureToExternalStorage(Bitmap screenCapture) {
        if ( screenCapture != null ) {
            // store screenCapture into external cache directory
            final File screenCaptureFile = new File(Environment.getExternalStorageDirectory().toString(), "screenCapture_" + System.currentTimeMillis() + ".jpg");

            // 1. Save bitmap to file & compress to jpeg. You may use PNG too
            try {

                final FileOutputStream out = new FileOutputStream(screenCaptureFile);
                screenCapture.compress(Bitmap.CompressFormat.JPEG, 90, out);
                out.flush();
                out.close();

                // 2. create send intent
                final Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("image/jpg");
                share.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(screenCaptureFile));

                // 3. launch intent-chooser
                final String chooserTitle = "Share Snaphot";
                SampleCamActivity.this.startActivity(Intent.createChooser(share, chooserTitle));

            } catch (final Exception e) {
                // should not occur when all permissions are set
                SampleCamActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        // show toast message in case something went wrong
                        Toast.makeText(SampleCamActivity.this, "Unexpected error, " + e, Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
    }

	public class InitializeAmazonPubSubTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {

			Log.e(TAG, "Initialize Pub Sub AWS");
			clientId = UUID.randomUUID().toString();

			credentialsProvider = new CognitoCachingCredentialsProvider(
					SampleCamActivity.this, // context
					COGNITO_POOL_ID, // Identity Pool ID
					MY_REGION // Region
			);
			Region region = Region.getRegion(MY_REGION);

			mqttManager = new AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_ENDPOINT);

			mqttManager.setKeepAlive(10);

			AWSIotMqttLastWillAndTestament lwt = new AWSIotMqttLastWillAndTestament("my/lwt/topic",
					"Android client lost connection", AWSIotMqttQos.QOS0);
			mqttManager.setMqttLastWillAndTestament(lwt);

			mIotAndroidClient = new AWSIotClient(credentialsProvider);
			mIotAndroidClient.setRegion(region);
			mIotAndroidClient.setEndpoint("iot.us-west-2.amazonaws.com");

			keystorePath = SampleCamActivity.this.getFilesDir().getPath();
			keystoreName = KEYSTORE_NAME;
			keystorePassword = KEYSTORE_PASSWORD;
			certificateId = CERTIFICATE_ID;

			try {
				if (AWSIotKeystoreHelper.isKeystorePresent(keystorePath, keystoreName)) {
					if (AWSIotKeystoreHelper.keystoreContainsAlias(certificateId, keystorePath,
							keystoreName, keystorePassword)) {
						Log.i(TAG, "Certificate " + certificateId
								+ " found in keystore - using for MQTT.");
						// load keystore from file into memory to pass on connection
						clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
								keystorePath, keystoreName, keystorePassword);
					} else {
						Log.i(TAG, "Key/cert " + certificateId + " not found in keystore.");
					}
				} else {
					Log.i(TAG, "Keystore " + keystorePath + "/" + keystoreName + " not found.");
				}
			} catch (Exception e) {
				Log.e(TAG, "An error occurred retrieving cert/key from keystore.", e);
			}

			if (clientKeyStore == null) {
				Log.i(TAG, "Cert/key was not found in keystore - creating new key and certificate.");

				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							CreateKeysAndCertificateRequest createKeysAndCertificateRequest =
									new CreateKeysAndCertificateRequest();
							createKeysAndCertificateRequest.setSetAsActive(true);
							final CreateKeysAndCertificateResult createKeysAndCertificateResult;
							createKeysAndCertificateResult =
									mIotAndroidClient.createKeysAndCertificate(createKeysAndCertificateRequest);
							Log.i(TAG,
									"Cert ID: " +
											createKeysAndCertificateResult.getCertificateId() +
											" created.");

							AWSIotKeystoreHelper.saveCertificateAndPrivateKey(certificateId,
									createKeysAndCertificateResult.getCertificatePem(),
									createKeysAndCertificateResult.getKeyPair().getPrivateKey(),
									keystorePath, keystoreName, keystorePassword);

							clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
									keystorePath, keystoreName, keystorePassword);

							AttachPrincipalPolicyRequest policyAttachRequest =
									new AttachPrincipalPolicyRequest();
							policyAttachRequest.setPolicyName(AWS_IOT_POLICY_NAME);
							policyAttachRequest.setPrincipal(createKeysAndCertificateResult
									.getCertificateArn());
							mIotAndroidClient.attachPrincipalPolicy(policyAttachRequest);

							new Thread(new Runnable() {
								@Override
								public void run() {
									Log.e(TAG, "set enabled is true");
								}
							});
						} catch (Exception e) {
							Log.e(TAG,
									"Exception occurred when generating new private key and certificate.",
									e);
							e.printStackTrace();
						}
					}
				}).start();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			Log.d("SHRUTI","AWS initialize done");

			Toast.makeText(SampleCamActivity.this, "Done syncing", Toast.LENGTH_LONG).show();


		}

	}

	public class ConnectAmazonPubSubTask extends AsyncTask<Void, Void, Void> {

		private boolean connected = false;

		@Override
		protected Void doInBackground(Void... params) {

			//        Connect to Amazon IOS
			try {
				mqttManager.connect(clientKeyStore, new AWSIotMqttClientStatusCallback() {
					@Override
					public void onStatusChanged(final AWSIotMqttClientStatus status,
												final Throwable throwable) {
						Log.d(TAG, "Status = " + String.valueOf(status));

						if (status == AWSIotMqttClientStatus.Connected) {
							Log.e(TAG, "Connected");
							connected = true;
							final String topic = "$aws/things/RaspberryPi/shadow/update/test";
							Log.d("SHRUTI","AWS CONNECT done");
							DatabaseSqlHelper databaseHelper = new DatabaseSqlHelper(SampleCamActivity.this);
							String switchStatus = databaseHelper.getDeviceStatus(ipString);

							try {
								JSONObject msg = new JSONObject();
								msg.put("ip_address", ipString);

								if (switchStatus.equalsIgnoreCase("OFF")) {
									msg.put("cmd", "ON");
								} else {
									msg.put("cmd", "OFF");
								}

								mqttManager.publishString(msg.toString(), topic, AWSIotMqttQos.QOS0);
							} catch (Exception e) {
								Log.e(TAG, "Publish error.", e);
							}
						}

					}
				});
			} catch (final Exception e) {
				Log.e(TAG, "Connection error.", e);
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);

		}

	}
}

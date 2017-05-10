package com.spectrum.smartapp.AugmentedReality;

import android.location.LocationListener;

import com.wikitude.architect.ArchitectJavaScriptInterfaceListener;
import com.wikitude.architect.ArchitectView;
import com.wikitude.architect.ArchitectView.SensorAccuracyChangeListener;

public interface ArchitectViewHolderInterface {
	
	/**
	 * 50km = architectView's default cullingDistance, return this value in "getInitialCullingDistanceMeters()" to not change cullingDistance.
	 */
	public static final int CULLING_DISTANCE_DEFAULT_METERS = 50 * 1000;
	
	/**
	 * path to the architect-file (AR-Experience HTML) to launch
	 * @return
	 */
	public String getARchitectWorldPath();
	
	/**
	 * JS interface listener fired once e.g. 'AR.platform.sendJSONObject({foo:"bar", bar:123})' is called in JS
	 */
	ArchitectJavaScriptInterfaceListener getArchitectJavaScriptInterfaceListener();
	
	/**
	 * @return layout id of your layout.xml that holds an ARchitect View, e.g. R.layout.camview
	 */
	public int getContentViewId();
	
	/**
	 * @return Wikitude SDK license key, checkout www.wikitude.com for details
	 */
	public String getWikitudeSDKLicenseKey();
	
	/**
	 * @return layout-id of architectView, e.g. R.id.architectView
	 */
	public int getArchitectViewId();

	/**
	 * 
	 * @return Implementation of a Location
	 */
	public ILocationProvider getLocationProvider(final LocationListener locationListener);
	
	/**
	 * @return Implementation of Sensor-Accuracy-Listener. That way you can e.g. show prompt to calibrate compass
	 */
	public SensorAccuracyChangeListener getSensorAccuracyListener();
	
	/**
	 * sets maximum distance to render places. In case your places are more than 50km away from the user you must adjust this value (compare 'AR.context.scene.cullingDistance').
	 * Return ArchitectViewHolder.CULLING_DISTANCE_DEFAULT_METERS to not change default behavior (50km range) or any positive float to set cullingDistance on architectView start.
	 * @return
	 */
	public float getInitialCullingDistanceMeters();
	
	/**
	 * Interface for a location-provider implementation
	 * feel free to implement your very own Location-Service, that handles GPS/Network positions more sophisticated but still takes care of
	 * life-cycle events
	 */
	public static interface ILocationProvider {

		/**
		 * Call when host-activity is resumed (usually within systems life-cycle method)
		 */
		public void onResume();

		/**
		 * Call when host-activity is paused (usually within systems life-cycle method)
		 */
		public void onPause();

	}

	/**
	 * @return Implementation of ArchitectWorldLoadedListener. That way you know when a AR world is finished loading or when it failed to load.
     */
	ArchitectView.ArchitectWorldLoadedListener getWorldLoadedListener();
}

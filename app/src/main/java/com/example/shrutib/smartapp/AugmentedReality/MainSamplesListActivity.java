package com.example.shrutib.smartapp.AugmentedReality;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.example.shrutib.smartapp.R;
import com.wikitude.architect.ArchitectView;
import com.wikitude.common.permission.PermissionManager;

import java.io.File;
import java.util.Arrays;

public class MainSamplesListActivity extends ListActivity {

	public static final String EXTRAS_KEY_ACTIVITY_TITLE_STRING = "activityTitle";
	public static final String EXTRAS_KEY_ACTIVITY_ARCHITECT_WORLD_URL = "activityArchitectWorldUrl";

	public static final String EXTRAS_KEY_ACTIVITY_IR = "activityIr";
	public static final String EXTRAS_KEY_ACTIVITY_GEO = "activityGeo";
	public static final String EXTRAS_KEY_ACTIVITY_INSTANT = "activityInstant";

	public static final String EXTRAS_KEY_ACTIVITIES_ARCHITECT_WORLD_URLS_ARRAY = "activitiesArchitectWorldUrls";
	public static final String EXTRAS_KEY_ACTIVITIES_TILES_ARRAY = "activitiesTitles";
	public static final String EXTRAS_KEY_ACTIVITIES_CLASSNAMES_ARRAY = "activitiesClassnames";

	public static final String EXTRAS_KEY_ACTIVITIES_IR_ARRAY = "activitiesIr";
	public static final String EXTRAS_KEY_ACTIVITIES_GEO_ARRAY = "activitiesGeo";
	public static final String EXTRAS_KEY_ACTIVITIES_INSTANT_ARRAY = "activitiesInstant";

	private int _lastSelectedListItemPosition = -1;

	private PermissionManager mPermissionManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(this.getContentViewId());

		this.setTitle(this.getActivityTitle());

		/* extract names of samples from res/arrays */
		final String[] values = this.getListLabels();

		/* use default list-ArrayAdapter */
		this.setListAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, android.R.id.text1, values));

		mPermissionManager = ArchitectView.getPermissionManager();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		/* get className of activity to call when clicking item at position x */
		_lastSelectedListItemPosition = position;

		String[] permissions = getActivitiesGeo()[_lastSelectedListItemPosition] ?
				new String[]{Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION} :
				new String[]{Manifest.permission.CAMERA};

		mPermissionManager.checkPermissions(this, permissions, PermissionManager.WIKITUDE_PERMISSION_REQUEST, new PermissionManager.PermissionManagerCallback() {
			@Override
			public void permissionsGranted(int requestCode) {
				loadExample();
			}

			@Override
			public void permissionsDenied(String[] deniedPermissions) {

				Toast.makeText(MainSamplesListActivity.this, "The Wikitude SDK needs the following permissions to enable an AR experience: " + Arrays.toString(deniedPermissions), Toast.LENGTH_SHORT).show();
			}

			@Override
			public void showPermissionRationale(final int requestCode, final String[] permissions) {
				AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainSamplesListActivity.this);
				alertBuilder.setCancelable(true);
				alertBuilder.setTitle("Wikitude Permissions");
				alertBuilder.setMessage("The Wikitude SDK needs the following permissions to enable an AR experience: " + Arrays.toString(permissions));
				alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mPermissionManager.positiveRationaleResult(requestCode, permissions);
					}
				});

				AlertDialog alert = alertBuilder.create();
				alert.show();
			}
		});
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		mPermissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}

	protected final String[] getListLabels() {
		return getIntent().getExtras().getStringArray(
				EXTRAS_KEY_ACTIVITIES_TILES_ARRAY);
	}

	protected String getActivityTitle() {
		return getIntent().getExtras().getString(
				EXTRAS_KEY_ACTIVITY_TITLE_STRING);
	}

	protected String[] getListActivities() {
		return getIntent().getExtras().getStringArray(
				EXTRAS_KEY_ACTIVITIES_CLASSNAMES_ARRAY);
	}

	protected String[] getArchitectWorldUrls() {
		return getIntent().getExtras().getStringArray(
				EXTRAS_KEY_ACTIVITIES_ARCHITECT_WORLD_URLS_ARRAY);
	}

	protected boolean[] getActivitiesIr() {
		return getIntent().getExtras().getBooleanArray(
				EXTRAS_KEY_ACTIVITIES_IR_ARRAY);
	}
	
	protected boolean[] getActivitiesGeo() {
		return getIntent().getExtras().getBooleanArray(
				EXTRAS_KEY_ACTIVITIES_GEO_ARRAY);
	}
	
	protected boolean[] getActivitiesInstant() {
		return getIntent().getExtras().getBooleanArray(
				EXTRAS_KEY_ACTIVITIES_INSTANT_ARRAY);
	}

	protected int getContentViewId() {
		return R.layout.list_sample;
	}

	private void loadExample() {
		try {

			if ( _lastSelectedListItemPosition >= 0 ) {

				final String className = getListActivities()[_lastSelectedListItemPosition];
				final Intent intent = new Intent(this, Class.forName(className));
				intent.putExtra(EXTRAS_KEY_ACTIVITY_TITLE_STRING,
						this.getListLabels()[_lastSelectedListItemPosition]);
				intent.putExtra(EXTRAS_KEY_ACTIVITY_ARCHITECT_WORLD_URL, "samples"
						+ File.separator + this.getArchitectWorldUrls()[_lastSelectedListItemPosition]
						+ File.separator + "index.html");
				intent.putExtra(EXTRAS_KEY_ACTIVITY_IR,
						this.getActivitiesIr()[_lastSelectedListItemPosition]);
				intent.putExtra(EXTRAS_KEY_ACTIVITY_GEO,
						this.getActivitiesGeo()[_lastSelectedListItemPosition]);
				intent.putExtra(EXTRAS_KEY_ACTIVITY_INSTANT,
						this.getActivitiesInstant()[_lastSelectedListItemPosition]);

				/* launch activity */
				this.startActivity(intent);
			}

		} catch (Exception e) {
			/*
			 * may never occur, as long as all SampleActivities exist and are
			 * listed in manifest
			 */
			final String className = getListActivities()[_lastSelectedListItemPosition];
			Toast.makeText(this, className + "\nnot defined/accessible",
					Toast.LENGTH_SHORT).show();
		}
	}
}

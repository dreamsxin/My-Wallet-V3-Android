package info.blockchain.merchant.directory;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import info.blockchain.wallet.R;

public class SuggestMerchant extends ActionBarActivity {

	private LocationManager locationManager = null;
	private LocationListener locationListener = null;
	private Location currLocation = null;
	private boolean gps_enabled = false;

	private ProgressBar progress = null;
	private FrameLayout mapView = null;
	private LinearLayout mapContainer = null;
	private LinearLayout confirmLayout = null;
	private GoogleMap map = null;

	TextView commandSave;
	DecimalFormat df = new DecimalFormat("#0.000000");

	private EditText edName = null;
	private EditText edDescription = null;
	private EditText edStreetAddress = null;
	private EditText edCity = null;
	private EditText edPostal = null;
	private EditText edTelephone = null;
	private EditText edWeb = null;
	private double selectedY;
	private double selectedX;

	ReverseGeocodingTask reverseGeocodingTask;
	UpdateLastLocationThread updateLastLocationThread;

	@Override
	public boolean onSupportNavigateUp() {
		onBackPressed();
		return true;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setContentView(R.layout.activity_suggest_merchant);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		Toolbar	toolbar = (Toolbar)findViewById(R.id.toolbar_general);
		setSupportActionBar(toolbar);

		this.setTitle(R.string.suggest_merchant);

		edName = (EditText)findViewById(R.id.merchant_name);
		edDescription = (EditText)findViewById(R.id.description);
		edStreetAddress = (EditText)findViewById(R.id.street_address);
		edCity = (EditText)findViewById(R.id.city);
		edPostal = (EditText)findViewById(R.id.zip);
		edTelephone = (EditText)findViewById(R.id.telephone);
		edWeb = (EditText)findViewById(R.id.web);

		mapContainer = (LinearLayout)findViewById(R.id.map_container);
		mapView = (FrameLayout)findViewById(R.id.map_layout);
		confirmLayout = (LinearLayout)findViewById(R.id.confirm_layout);

		progress = (ProgressBar) findViewById(R.id.progressBar);
		progress.setVisibility(View.GONE);

		locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		locationListener = new MyLocationListener();

		map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
		map.getUiSettings().setZoomControlsEnabled(true);

		try {
			gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		} catch(Exception e) {
			gps_enabled = false;
		}

		if(gps_enabled) {
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
		}

		map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

			@Override
			public void onMapClick(LatLng point) {
				map.clear();
				MarkerOptions marker = new MarkerOptions().position(new LatLng(point.latitude, point.longitude));
				map.addMarker(marker);

				populateAddressViews(point);
			}
		});

		final List<String> categories = new ArrayList<String>();
		categories.add(getString(R.string.merchant_cat_hint));
		categories.add(getString(R.string.merchant_cat1));
		categories.add(getString(R.string.merchant_cat2));
		categories.add(getString(R.string.merchant_cat3));
		categories.add(getString(R.string.merchant_cat4));
		categories.add(getString(R.string.merchant_cat5));

		final Spinner spCategory = (Spinner)findViewById(R.id.merchant_category_spinner);
		ArrayAdapter<String> categorySpinnerArrayAdapter = new ArrayAdapter<String>(this, R.layout.spinner_item, categories);
		categorySpinnerArrayAdapter.setDropDownViewResource(R.layout.spinner_item2);
		spCategory.setAdapter(categorySpinnerArrayAdapter);

		commandSave = (TextView)findViewById(R.id.command_save);
		commandSave.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				if(commandSave.getText().toString().equals(getResources().getString(R.string.save))) {
					confirmLayout.setVisibility(View.GONE);
					Toast.makeText(SuggestMerchant.this, "Coming soon", Toast.LENGTH_SHORT).show();

					final HashMap<Object,Object> params = new HashMap<Object,Object>();
					params.put("NAME", edName.getText().toString());
					params.put("DESCRIPTION", edDescription.getText().toString());
					params.put("STREET_ADDRESS", edStreetAddress.getText().toString());
					params.put("CITY", edCity.getText().toString());
					params.put("ZIP", edPostal.getText().toString());
					params.put("TELEPHONE", edTelephone.getText().toString());
					params.put("WEB", edWeb.getText().toString());
					params.put("LATITUDE", df.format(selectedY));
					params.put("LONGITUDE", df.format(selectedX));
					params.put("CATEGORY", Integer.toString(spCategory.getSelectedItemPosition()));
					params.put("SOURCE", "Android");

//					TODO final Handler handler = new Handler();
//
//					new Thread(new Runnable() {
//						@Override
//						public void run() {
//
//							Looper.prepare();
//
//							String res = null;
//							try {
//								//res = piuk.blockchain.android.util.WalletUtils.postURLWithParams("s://merchant-directory.blockchain.info/cgi-bin/btcp.pl", params);
////								WebUtil.getInstance().postURL("s://merchant-directory.blockchain.info/cgi-bin/btcp.pl", params);
//							}
//							catch(Exception e) {
//								Toast.makeText(SuggestMerchant.this, e.getMessage(), Toast.LENGTH_SHORT).show();
//							}
//
//							if(res.contains("\"result\":1")) {
//								Toast.makeText(SuggestMerchant.this, R.string.ok_writing_merchant, Toast.LENGTH_SHORT).show();
//								setResult(RESULT_OK);
//								finish();
//							}
//							else {
//								Toast.makeText(SuggestMerchant.this, R.string.error_writing_merchant, Toast.LENGTH_SHORT).show();
//							}
//
//							Looper.loop();
//
//						}
//
//					}).start();

					finish();
				}else{
					confirmLayout.setVisibility(View.VISIBLE);
					commandSave.setVisibility(View.VISIBLE);
					commandSave.setText(getResources().getString(R.string.save));
					mapView.setVisibility(View.GONE);
					edDescription.requestFocus();
				}
			}
		});

		mapView.setVisibility(View.GONE);
		confirmLayout.setVisibility(View.GONE);
		commandSave.setVisibility(View.GONE);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		locationManager.removeUpdates(locationListener);
	}

	private class MyLocationListener implements LocationListener {

		@Override
		public void onLocationChanged(Location location) {
			if(location != null) {
				currLocation = location;
			}

			if(progress != null && progress.isShown()) {
				progress.setVisibility(View.GONE);
			}

		}

		@Override
		public void onProviderDisabled(String provider) { ; }

		@Override
		public void onProviderEnabled(String provider) { ; }

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) { ; }
	}

	public void manualClicked(View view){

		if(reverseGeocodingTask!=null)reverseGeocodingTask.cancel(true);

		edDescription.requestFocus();
		edDescription.setText("");
		edStreetAddress.setText("");
		edPostal.setText("");
		edCity.setText("");
		edTelephone.setText("");
		edWeb.setText("");

		confirmLayout.setVisibility(View.VISIBLE);
		commandSave.setVisibility(View.VISIBLE);
		commandSave.setText(getResources().getString(R.string.save));
		mapView.setVisibility(View.GONE);
	}

	public void autoClicked(View view){

		if(reverseGeocodingTask!=null)reverseGeocodingTask.cancel(true);

		mapView.setVisibility(View.VISIBLE);
		progress.setVisibility(View.VISIBLE);
		confirmLayout.setVisibility(View.GONE);
		mapContainer.setVisibility(View.GONE);

		if(currLocation != null) {

			zoomToLocation(currLocation.getLongitude(), currLocation.getLatitude());
			commandSave.setVisibility(View.VISIBLE);
			commandSave.setText(getResources().getString(R.string.next));
		}
		else {
			if(gps_enabled) {

				if(updateLastLocationThread!=null)updateLastLocationThread.cancel(true);
				updateLastLocationThread = new UpdateLastLocationThread(this);
				updateLastLocationThread.execute();
			}
			else {

//				try {
//					gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
//				} catch(Exception e) {
//					gps_enabled = false;
//				}
//
//				if(gps_enabled) {
//					locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
//				}

			}
		}
	}

	private void zoomToLocation(double x, double y){

		mapView.setVisibility(View.VISIBLE);
		mapContainer.setVisibility(View.VISIBLE);
		progress.setVisibility(View.GONE);
		map.clear();
		map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(y, x), 13));
		CameraPosition cameraPosition = new CameraPosition.Builder().target(new LatLng(y, x)).zoom(17).build();
		map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
		map.addMarker(new MarkerOptions().position(new LatLng(y, x)));

		populateAddressViews(new LatLng(y,x));
	}

	private void populateAddressViews(LatLng latLng){

		selectedY = latLng.latitude;
		selectedX = latLng.longitude;

		try {
			reverseGeocodingTask = new ReverseGeocodingTask(SuggestMerchant.this);
			reverseGeocodingTask.execute(latLng);

		} catch (Exception e) {
			Log.e("","",e);
			Toast.makeText(this,getResources().getString(R.string.address_lookup_fail),Toast.LENGTH_SHORT).show();
		}
	}

	class UpdateLastLocationThread extends AsyncTask<Void, Void, Void> {

		public HashMap<String, String> result = new HashMap<String, String>();

		Context mContext;

		public UpdateLastLocationThread(Context context) {
			super();
			mContext = context;
		}

		@Override
		protected Void doInBackground(Void... params) {

			currLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

			if(currLocation != null) {

				SuggestMerchant.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						zoomToLocation(currLocation.getLongitude(),currLocation.getLatitude());
						commandSave.setVisibility(View.VISIBLE);
						commandSave.setText(getResources().getString(R.string.next));
					}
				});
			}

			return null;
		}
	}

	class ReverseGeocodingTask extends AsyncTask<LatLng, Void, Void> {

		Context mContext;

		public ReverseGeocodingTask(Context context) {
			super();
			mContext = context;
		}

		@Override
		protected Void doInBackground(LatLng... params) {

			SuggestMerchant.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					progress.setVisibility(View.VISIBLE);
				}
			});

			Geocoder gc = new Geocoder(mContext, Locale.getDefault());

			try {
				List<Address> addrList = gc.getFromLocation(params[0].latitude, params[0].longitude, 1);

				if (addrList.size() > 0) {
					final Address address = addrList.get(0);

					SuggestMerchant.this.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if(address.getMaxAddressLineIndex()>0 && address.getAddressLine(0)!=null){
								edStreetAddress.setText(address.getAddressLine(0));
								Toast.makeText(SuggestMerchant.this, address.getAddressLine(0), Toast.LENGTH_SHORT).show();
							}
							if(address.getPostalCode()!=null)edPostal.setText(address.getPostalCode());
							if(address.getLocality()!=null)edCity.setText(address.getLocality());

							progress.setVisibility(View.GONE);
						}
					});
				} else {
					return null;
				}
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}

			return null;
		}
	}
}
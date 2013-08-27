/* 
 * enviroCar 2013
 * Copyright (C) 2013  
 * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 * 
 */

package org.envirocar.app.activity;

import java.text.DecimalFormat;

import org.envirocar.app.R;
import org.envirocar.app.application.ECApplication;
import org.envirocar.app.event.CO2Event;
import org.envirocar.app.event.CO2EventListener;
import org.envirocar.app.event.EventBus;
import org.envirocar.app.event.LocationEvent;
import org.envirocar.app.event.LocationEventListener;
import org.envirocar.app.event.SpeedEvent;
import org.envirocar.app.event.SpeedEventListener;
import org.envirocar.app.storage.DbAdapter;
import org.envirocar.app.util.AndroidUtil;
import org.envirocar.app.views.RoundProgress;
import org.envirocar.app.views.TypefaceEC;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
/**
 * Dashboard page that displays the current speed, co2 and car.
 * @author jakob
 * @author gerald
 *
 */
public class DashboardFragment extends SherlockFragment {

	public static final int SENSOR_CHANGED_RESULT = 1337;
	
	// UI Items
	
	TextView speedTextView;
	RoundProgress roundProgressSpeed;
	TextView co2TextView;
	TextView positionTextView;
	RoundProgress roundProgressCO2;
	DbAdapter dbAdapter;
	private TextView sensor;
	View dashboardView;

	protected Location location;

	private LocationEventListener locationListener;

	private int speed;

	private double co2;

	private SpeedEventListener speedListener;

	private CO2EventListener co2Listener;

	private SharedPreferences preferences;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.dashboard, container, false);
	}
	
	/**
	 * Updates the sensor-textview
	 */
	public void updateSensorOnDashboard(){
		sensor.setText(getCurrentSensorString());
	}
	
	/**
	 * Returns the sensor properties as a string
	 * @return
	 */
	private String getCurrentSensorString() {
		SharedPreferences pref = AndroidUtil.getInstance().getDefaultSharedPreferences();
		String nonsens = "nosensor";
		if (pref.contains(ECApplication.PREF_KEY_SENSOR_ID) && 
				pref.contains(ECApplication.PREF_KEY_FUEL_TYPE) &&
				pref.contains(ECApplication.PREF_KEY_CAR_CONSTRUCTION_YEAR) &&
				pref.contains(ECApplication.PREF_KEY_CAR_MODEL) &&
				pref.contains(ECApplication.PREF_KEY_CAR_MANUFACTURER)) {
			
			String prefSensorid = pref.getString(ECApplication.PREF_KEY_SENSOR_ID, nonsens);
			String prefFuelType = pref.getString(ECApplication.PREF_KEY_FUEL_TYPE, nonsens);
			String prefYear = pref.getString(ECApplication.PREF_KEY_CAR_CONSTRUCTION_YEAR, nonsens);
			String prefModel = pref.getString(ECApplication.PREF_KEY_CAR_MODEL, nonsens);
			String prefManu = pref.getString(ECApplication.PREF_KEY_CAR_MANUFACTURER, nonsens);
			
			if (prefSensorid.equals(nonsens) == false ||
					prefYear.equals(nonsens) == false ||
					prefFuelType.equals(nonsens) == false ||
					prefModel.equals(nonsens) == false ||
					prefManu.equals(nonsens) == false ) {
				return prefManu+" "+prefModel+" ("+prefFuelType+" "+prefYear+")";
			}
		}
		return getResources().getString(R.string.no_sensor_selected);

	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {

		super.onViewCreated(view, savedInstanceState);
		
		initializeEventListeners();
		
		dashboardView = getView();

		preferences = AndroidUtil.getInstance().getDefaultSharedPreferences();
		// Include application and adapter
		
		dbAdapter = ((ECApplication) getActivity().getApplication())
				.getDbAdapterLocal();
		
		// Setup UI elements

		co2TextView = (TextView) getView().findViewById(R.id.co2TextView);
		speedTextView = (TextView) getView().findViewById(
				R.id.textViewSpeedDashboard);
		roundProgressCO2 = (RoundProgress) getView().findViewById(
				R.id.blue_progress_bar);
		roundProgressSpeed = (RoundProgress) getView().findViewById(
				R.id.blue_progress_bar2);
		sensor = (TextView) getView().findViewById(R.id.dashboard_current_sensor);
		
		positionTextView = (TextView) getView().findViewById(R.id.positionTextView);
		
		updateSensorOnDashboard();
		
		sensor.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
		        MyGarage garageFragment = new MyGarage();
		        getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, garageFragment).addToBackStack(null).commit();
			}
		});
		
		// Handle the UI updates

		final Handler handler = new Handler();
		Runnable runnable = new Runnable() {
			@Override
			public void run() {

				// Deal with the speed values

				int speed = getSpeedMeasurement();
				int speedProgress;
				if (preferences.getBoolean(SettingsActivity.IMPERIAL_UNIT, false)) {
					speedTextView.setText(speed + " km/h");
					if (speed <= 0)
						speedProgress = 0;
					else if (speed > 200)
						speedProgress = 100;
					else
						speedProgress = speed / 2;
					roundProgressSpeed.setProgress(speedProgress);
				} else {
					speedTextView.setText(speed/1.6 + " mph");
					if (speed <= 0)
						speedProgress = 0;
					else if (speed > 150)
						speedProgress = 100;
					else
						speedProgress = (int) (speed / 1.5);
					roundProgressSpeed.setProgress(speedProgress);
				}
				

				// Deal with the co2 values

				double co2 = getCo2Measurement();
				double co2Progress;
				
				DecimalFormat twoDForm = new DecimalFormat("#.##");
				
				co2TextView.setText(twoDForm.format(co2) + " kg/h"); 
				if (co2 <= 0)
					co2Progress = 0;
				else if (co2 > 100)
					co2Progress = 100;
				else
					co2Progress = co2;
				roundProgressCO2.setProgress(co2Progress);
				
				if (co2Progress>30){
					dashboardView.setBackgroundColor(Color.RED);
				} else {
					dashboardView.setBackgroundColor(Color.WHITE);
				}
				
				// set location
				
				Location location = getLocation();
				if (location != null && location.getLongitude() != 0 && location.getLatitude() != 0) {
					StringBuffer sb = new StringBuffer();
					sb.append("Provider: " + location.getProvider() + "\n");
					sb.append("Lat: " + location.getLatitude() + "\n");
					sb.append("Long: " + location.getLongitude() + "\n");
					sb.append("Acc: " + location.getAccuracy() + "\n");
					sb.append("Speed: " + location.getSpeed() + "\n");
					positionTextView.setText(sb.toString());
					positionTextView.setTextColor(Color.BLACK);
					positionTextView.setBackgroundColor(Color.WHITE);
				} else {
					positionTextView.setText(R.string.positioning_Info);
					positionTextView.setTextColor(Color.WHITE);
					positionTextView.setBackgroundColor(Color.RED);
				}

				// Repeat this in x ms
				handler.postDelayed(this, 1000);
			}
		};
		
		// Repeat the UI update every second (1000ms)
		
		handler.postDelayed(runnable, 1000);

		TypefaceEC.applyCustomFont((ViewGroup) view,
				TypefaceEC.Newscycle(getActivity()));

	}

	private void initializeEventListeners() {
		this.locationListener = new LocationEventListener() {
			@Override
			public void receiveEvent(LocationEvent event) {
				location = event.getPayload();
			}
		};
		this.speedListener = new SpeedEventListener() {
			@Override
			public void receiveEvent(SpeedEvent event) {
				speed = event.getPayload(); 
			}
		};
		this.co2Listener = new CO2EventListener() {
			@Override
			public void receiveEvent(CO2Event event) {
				co2 = event.getPayload();
			}
		};
		EventBus.getInstance().registerListener(locationListener);
		EventBus.getInstance().registerListener(speedListener);
		EventBus.getInstance().registerListener(co2Listener);
	}

	protected int getSpeedMeasurement() {
		return speed;
	}

	protected double getCo2Measurement() {
		return co2;
	}

	protected Location getLocation() {
		return location;
	}
}

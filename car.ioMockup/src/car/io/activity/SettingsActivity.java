package car.io.activity;

import java.util.ArrayList;
import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.Window;
import android.widget.Toast;
import car.io.R;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

public class SettingsActivity extends SherlockPreferenceActivity {

	public static final String BLUETOOTH_KEY = "bluetooth_list";
	
	/**
	 * Helper method that cares about the bluetooth list
	 */

	private void initializeBluetoothList() {

		// Init Lists

		ArrayList<CharSequence> possibleDevices = new ArrayList<CharSequence>();
		ArrayList<CharSequence> entryValues = new ArrayList<CharSequence>();

		// Get the bluetooth preference if possible

		ListPreference bluetoothDeviceList = (ListPreference) getPreferenceScreen()
				.findPreference(BLUETOOTH_KEY);

		// Get the default adapter

		final BluetoothAdapter bluetoothAdapter = BluetoothAdapter
				.getDefaultAdapter();

		// No Bluetooth available...

		if (bluetoothAdapter == null) {
			bluetoothDeviceList.setEntries(possibleDevices
					.toArray(new CharSequence[0]));
			bluetoothDeviceList.setEntryValues(entryValues
					.toArray(new CharSequence[0]));

			Toast.makeText(this, "No Bluetooth available!", Toast.LENGTH_SHORT);

			return;
		}

		// Prepare getting the devices

		final Activity thisSettingsActivity = this;
		bluetoothDeviceList.setEntries(new CharSequence[1]);
		bluetoothDeviceList.setEntryValues(new CharSequence[1]);

		// Listen for clicks on the list

		bluetoothDeviceList
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					public boolean onPreferenceClick(Preference preference) {

						if (bluetoothAdapter == null
								|| !bluetoothAdapter.isEnabled()) {
							Toast.makeText(thisSettingsActivity,
									"No Bluetooth support. Is Bluetooth on?",
									Toast.LENGTH_SHORT).show();
							return false;
						}
						return true;
					}
				});

		// Get all paired devices...

		Set<BluetoothDevice> availablePairedDevices = bluetoothAdapter
				.getBondedDevices();

		// ...and add them to the list of available paired devices

		if (availablePairedDevices.size() > 0) {
			for (BluetoothDevice device : availablePairedDevices) {
				possibleDevices.add(device.getName() + "\n"
						+ device.getAddress());
				entryValues.add(device.getAddress());
			}
		}

		bluetoothDeviceList.setEntries(possibleDevices
				.toArray(new CharSequence[0]));
		bluetoothDeviceList.setEntryValues(entryValues
				.toArray(new CharSequence[0]));
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		initializeBluetoothList();
	}

}

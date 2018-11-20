package com.seniorzhai.gridwichterle.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.seniorzhai.gridwichterle.bus.CancelGridBus;
import com.seniorzhai.gridwichterle.bus.BusProvider;
import com.seniorzhai.gridwichterle.bus.GridOnOffBus;
import com.seniorzhai.gridwichterle.bus.ShowSettingsBus;

/**
 * Created with IntelliJ IDEA.
 * User: Michal Matl (michal.matl@inmite.eu)
 * Date: 7/21/13
 * Time: 9:00 PM
 */
public class NotificationReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {

		String action = intent.getAction();

		if ("notification_cancelled".equals(action)) {
			BusProvider.getInstance().post(new CancelGridBus());
		}

		if ("notification_settings".equals(action)) {
			Log.d(Constants.TAG, "Settings on");
			BusProvider.getInstance().post(new ShowSettingsBus());
		}

		if ("notification_settings_off".equals(action)) {
			BusProvider.getInstance().post(new GridOnOffBus(GridOnOffBus.ACTION_GRID_OFF));
		}
	}


}

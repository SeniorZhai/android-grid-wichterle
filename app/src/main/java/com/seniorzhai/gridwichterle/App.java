package com.seniorzhai.gridwichterle;

import android.app.Application;
import com.seniorzhai.gridwichterle.core.Config;

/**
 * Created with IntelliJ IDEA.
 * User: Michal Matl (michal.matl@inmite.eu)
 * Date: 10/12/13
 * Time: 10:53 PM
 */
public class App extends Application {

	private Config mConfig;

	@Override
	public Object getSystemService(String name) {

		if (Config.class.getName().equals(name)) {
			if (mConfig == null) {
				mConfig = new Config(getApplicationContext());
			}

			return mConfig;
		}

		return super.getSystemService(name);
	}

}

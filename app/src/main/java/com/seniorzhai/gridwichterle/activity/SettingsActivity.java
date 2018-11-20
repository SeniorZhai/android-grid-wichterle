package com.seniorzhai.gridwichterle.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.seniorzhai.gridwichterle.R;
import com.seniorzhai.gridwichterle.bus.BusProvider;
import com.seniorzhai.gridwichterle.bus.CancelGridBus;
import com.seniorzhai.gridwichterle.bus.ColorChangeBus;
import com.seniorzhai.gridwichterle.bus.GridOnOffBus;
import com.seniorzhai.gridwichterle.core.Config;
import com.seniorzhai.gridwichterle.core.Constants;
import com.seniorzhai.gridwichterle.core.Utils;
import com.seniorzhai.gridwichterle.dialogs.ColorsDialog;
import com.seniorzhai.gridwichterle.services.GridOverlayService;
import com.squareup.otto.Subscribe;

/**
 * Created with IntelliJ IDEA.
 * User: Michal Matl
 * Date: 21.10.13
 * Time: 20:10
 */
public class SettingsActivity extends AppCompatActivity {

  public TextView txtGridSize;
  public TextView txtVersion;
  public TextView txtSendFeedback;
  public LinearLayout txtTheCode;
  public SeekBar seekBar;
  public RelativeLayout layoutColor;
  public View viewColor;
  public CheckedTextView chckFullScreen;
  public LinearLayout layoutDevelopers;
  public Switch switchGrid;

  private Config mConfig;

  public static void call(Context context) {
    Intent intent = new Intent(context, SettingsActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    context.startActivity(intent);
  }

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_settings);

    Intent intent = new Intent(this, GridOverlayService.class);
    startService(intent);
    txtGridSize = findViewById(R.id.txtGridSize);
    txtVersion = findViewById(R.id.txtVersion);
    txtSendFeedback = findViewById(R.id.txtSendFeedback);
    txtTheCode = findViewById(R.id.txtTheCode);
    seekBar = findViewById(R.id.seekBar);
    layoutColor = findViewById(R.id.layoutColor);
    viewColor = findViewById(R.id.viewColor);
    chckFullScreen = findViewById(R.id.chckFullScreen);
    layoutDevelopers = findViewById(R.id.layoutDevelopers);
    switchGrid = findViewById(R.id.gridSwitch);
    setupViews();
  }

  private void setupViews() {

    mConfig = (Config) getApplicationContext().getSystemService(Config.class.getName());
    final String seekBarString = getString(R.string.settings_seek_bar);

    chckFullScreen.setChecked(mConfig.isFullScreenModeActivated());
    chckFullScreen.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        if (chckFullScreen.isChecked()) {
          chckFullScreen.setChecked(false);
          mConfig.setFullScreenMode(false);
        } else {
          chckFullScreen.setChecked(true);
          mConfig.setFullScreenMode(true);
        }
        applyNow();
      }
    });

    layoutDevelopers.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        Utils.openBrowser(SettingsActivity.this, "https://plus.google.com/110778431549186951626");
      }
    });

    txtTheCode.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        Utils.openBrowser(SettingsActivity.this,
            "https://github.com/seniorzhai/android-grid-wichterle");
      }
    });

    txtSendFeedback.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        Utils.sendEmail(SettingsActivity.this, "android@inmite.eu");
      }
    });

    txtVersion.setText(getVersionName(this));

    seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
        txtGridSize.setText(String.format(seekBarString, Integer.toString(progress + 4)));
      }

      @Override public void onStartTrackingTouch(SeekBar seekBar) {

      }

      @Override public void onStopTrackingTouch(SeekBar seekBar) {
        mConfig.setGridSideSize(seekBar.getProgress() + 4);
        applyNow();
      }
    });

    seekBar.setProgress(mConfig.getGridSideSize());
    txtGridSize.setText(String.format(seekBarString, Integer.toString(mConfig.getGridSideSize())));

    layoutColor.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        ColorsDialog.show(getSupportFragmentManager());
      }
    });

    viewColor.setBackgroundColor(mConfig.getColor());

    switchGrid.setChecked(GridOverlayService.sIsGridShown);
    switchGrid.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override public void onCheckedChanged(CompoundButton compoundButton, boolean switchOn) {
        if (switchOn) {
          gridOn();
        } else {
          BusProvider.getInstance().post(new GridOnOffBus(GridOnOffBus.ACTION_GRID_OFF));
        }
      }
    });
  }

  private void gridOn() {
    if (Build.VERSION.SDK_INT >= 23) {
      if (Settings.canDrawOverlays(this)) {
        BusProvider.getInstance().post(new GridOnOffBus(GridOnOffBus.ACTION_GRID_ON));
      } else {
        try {
          Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
          startActivityForResult(intent, 0x1111);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    } else {
      BusProvider.getInstance().post(new GridOnOffBus(GridOnOffBus.ACTION_GRID_ON));
    }
  }

  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == 0x1111) {
      if (Build.VERSION.SDK_INT >= 23) {
        if (!Settings.canDrawOverlays(this)) {
          Toast.makeText(this, R.string.lose_permission, Toast.LENGTH_SHORT).show();
        } else {
          BusProvider.getInstance().post(new GridOnOffBus(GridOnOffBus.ACTION_GRID_ON));
        }
      }
    }
  }

  private void applyNow() {
    if (switchGrid.isChecked()) {
      BusProvider.getInstance().post(new GridOnOffBus(GridOnOffBus.ACTION_GRID_OFF));
      BusProvider.getInstance().post(new GridOnOffBus(GridOnOffBus.ACTION_GRID_ON));
    }
  }

  private String getVersionName(Context ctx) {
    try {
      ComponentName comp = new ComponentName(ctx, ctx.getClass());
      PackageInfo pinfo = ctx.getPackageManager().getPackageInfo(comp.getPackageName(), 0);
      return pinfo.versionName;
    } catch (android.content.pm.PackageManager.NameNotFoundException e) {
      return "unknown";
    }
  }

  @Subscribe public void changeColor(ColorChangeBus colorChangeBus) {
    viewColor.setBackgroundColor(mConfig.getColor());
    applyNow();
  }

  @Subscribe public void cancelGrid(CancelGridBus cancelGridBus) {
    Log.d(Constants.TAG, "GridOverlayService.CancelGrid()");
    finish();
  }

  @Override protected void onStart() {
    super.onStart();
    BusProvider.getInstance().register(this);
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    BusProvider.getInstance().unregister(this);
  }
}

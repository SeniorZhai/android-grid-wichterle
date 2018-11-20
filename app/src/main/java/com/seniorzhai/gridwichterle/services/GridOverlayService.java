package com.seniorzhai.gridwichterle.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RemoteViews;
import androidx.core.app.NotificationCompat;
import com.seniorzhai.gridwichterle.R;
import com.seniorzhai.gridwichterle.activity.SettingsActivity;
import com.seniorzhai.gridwichterle.bus.BusProvider;
import com.seniorzhai.gridwichterle.bus.CancelGridBus;
import com.seniorzhai.gridwichterle.bus.GridOnOffBus;
import com.seniorzhai.gridwichterle.bus.ShowSettingsBus;
import com.seniorzhai.gridwichterle.core.Constants;
import com.seniorzhai.gridwichterle.core.NotificationReceiver;
import com.seniorzhai.gridwichterle.views.GridOverlay;
import com.squareup.otto.Subscribe;

import static android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
import static android.view.WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;

/**
 * Created with IntelliJ IDEA.
 * User: Michal Matl (michal.matl@inmite.eu)
 * Date: 7/19/13
 * Time: 11:22 AM
 */
public class GridOverlayService extends Service {

  private GridOverlay mGridOverlay;

  private static final int NOTIFICATION_ID = 1;

  public static boolean sIsServiceRunning = false;
  public static boolean sIsGridShown = false;

  @Override public void onCreate() {
    super.onCreate();

    BusProvider.getInstance().register(this);
  }

  private void showGrid() {
    Log.d(Constants.TAG, "GridOverlayService.showGrid()");
    GridOverlayService.sIsGridShown = true;
    final WindowManager.LayoutParams lp =
        new WindowManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      lp.type = TYPE_APPLICATION_OVERLAY;
    } else {
      lp.type = TYPE_SYSTEM_ALERT;
    }
    final WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);

    mGridOverlay = new GridOverlay(this);
    wm.addView(mGridOverlay, lp);
  }

  private void removeGrid() {
    Log.d(Constants.TAG, "GridOverlayService.removeGrid()");
    GridOverlayService.sIsGridShown = false;
    try {
      if (mGridOverlay != null && mGridOverlay.getParent() != null) {
        final WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        wm.removeView(mGridOverlay);
      }
    } catch (Exception e) {
      Log.e(Constants.TAG, "Remove grid failed");
    }
  }

  private void removeNotification() {
    NotificationManager notificationManager =
        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    notificationManager.cancel(NOTIFICATION_ID);
  }

  private void restartGrid() {
    Log.d(Constants.TAG, "GridOverlayService.restartGrid()");
    removeGrid();
    showGrid();
  }

  @Subscribe public void showSettings(ShowSettingsBus showSettingsBus) {
    Log.d(Constants.TAG, "GridOverlayService.showSettings()");
    SettingsActivity.call(getApplicationContext());
  }

  private void showNotification() {
    Log.d(Constants.TAG, "GridOverlayService.showNotification()");

    // custom notification layout
    RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.notification_layout);
    contentView.setOnClickPendingIntent(R.id.btnSettings, getDeleteIntent());

    final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "channel_node");
    builder.setSmallIcon(R.drawable.ic_launcher)
        .setOngoing(false)
        .setAutoCancel(true)
        .setContent(contentView)
        .setContentIntent(getSettingIntent());
    Notification notification = builder.build();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationChannel channel =
          new NotificationChannel("channel_node", "node", NotificationManager.IMPORTANCE_LOW);
      channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
      channel.setSound(null, null);
      channel.setShowBadge(false);
      ((NotificationManager) getSystemService(
          Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
    }
    startForeground(NOTIFICATION_ID, notification);
  }

  private PendingIntent getDeleteIntent() {
    Intent intent = new Intent(getApplicationContext(), NotificationReceiver.class);
    intent.setAction("notification_cancelled");
    return PendingIntent.getBroadcast(getApplicationContext(), 0, intent,
        PendingIntent.FLAG_CANCEL_CURRENT);
  }

  private PendingIntent getSettingIntent() {
    Intent intent = new Intent(getApplicationContext(), NotificationReceiver.class);
    intent.setAction("notification_settings");
    return PendingIntent.getBroadcast(getApplicationContext(), 0, intent,
        PendingIntent.FLAG_CANCEL_CURRENT);
  }

  private PendingIntent getSettingOffIntent() {
    Intent intent = new Intent(getApplicationContext(), NotificationReceiver.class);
    intent.setAction("notification_settings_off");
    return PendingIntent.getBroadcast(getApplicationContext(), 0, intent,
        PendingIntent.FLAG_CANCEL_CURRENT);
  }

  @Subscribe public void cancelGrid(CancelGridBus cancelGridBus) {
    Log.d(Constants.TAG, "GridOverlayService.CancelGrid()");

    sIsServiceRunning = false;
    sIsGridShown = false;

    removeGrid();
    stopForeground(true);
  }

  @Subscribe public void gridAction(GridOnOffBus settingsOnOffBus) {

    if (settingsOnOffBus.getAction() == GridOnOffBus.ACTION_GRID_ON) {
      showGrid();
    }

    if (settingsOnOffBus.getAction() == GridOnOffBus.ACTION_GRID_OFF) {
      removeGrid();
    }
  }

  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    if (!sIsServiceRunning) {
      sIsServiceRunning = true;
      showNotification();
    }
    return Service.START_NOT_STICKY;
  }

  @Override public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    //we need restart grid when a screen rotates
    if (mGridOverlay != null && mGridOverlay.getParent() != null) {
      restartGrid();
    }
  }

  @Override public void onDestroy() {
    super.onDestroy();

    BusProvider.getInstance().unregister(this);
  }

  @Override public IBinder onBind(Intent intent) {
    return null;
  }
}

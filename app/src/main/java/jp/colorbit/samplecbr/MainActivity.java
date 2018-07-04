package jp.colorbit.samplecbr;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.text.InputType;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ToggleButton;

import jp.colorbit.decodelib.CBAuth;

public class MainActivity extends Activity implements Camera.AutoFocusCallback {
	
	private static final long ONE_SHOT_FOCUS_TIMEOUT_MSEC = 10 * 1000;

	private GestureDetector gestureDetector;
	
	private CBController controller;

	private ToggleButton lightButton;
	private ToggleButton focusButton;
	private ToggleButton soundButton;
	
	private boolean oneShotFocusMode;
	
	private Timer timer;
	private Handler handler;
	private TimerTask focusTimeoutTask;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		timer = new Timer();
		handler = new Handler();
		
		gestureDetector = new GestureDetector(this,
				new GestureDetector.SimpleOnGestureListener() {
					@Override
					public boolean onSingleTapUp(MotionEvent e) {
						return MainActivity.this.onSingleTapUp(e);
					}
				});

		lightButton = (ToggleButton) findViewById(R.id.LightButton);
		focusButton = (ToggleButton) findViewById(R.id.AutoFocusButton);
		soundButton = (ToggleButton) findViewById(R.id.SoundButton);
		
		lightButton.setChecked(false);
		focusButton.setChecked(true);
		soundButton.setChecked(false);
		
		focusButton.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				return MainActivity.this.onAutoFocusButtonLongPress(v);
			}
		});

		controller = new CBController(this,
				(FrameLayout) findViewById(R.id.previewFrame));
	}

	@Override
	protected void onDestroy() {
		timer.cancel();
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		super.onResume();
		controller.onResume();
		setContinuousFocus();
		showLoginDialog();
	}

	@Override
	protected void onPause() {
		if (focusTimeoutTask != null) {
			focusTimeoutTask.cancel();
			focusTimeoutTask = null;
		}
		controller.onPause();
		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_settings) {
			Intent intent = new Intent(this, SettingsActivity.class);
			List<Camera.Size> captureSizeList = controller.getCaptureSizeList();
			if (captureSizeList != null) {
				String[] strings = new String[captureSizeList.size()];
				int i = 0;
				for (Camera.Size size: captureSizeList) {
					strings[i++] = String.format("%d x %d",
							size.width, size.height);
				}
				intent.putExtra(SettingsActivity.EXTRA_CAPTURE_SIZE_LIST,
						strings);
			}
			startActivity(intent);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	private void showLoginDialog() {
        long deviceID = CBAuth.getDeviceID(this);
        if (deviceID > 0) {
            // Login required
            final EditText editView = new EditText(this);
            editView.setInputType(InputType.TYPE_CLASS_NUMBER);
            editView.setHint("Password");
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle("Enter Password");
            dialog.setMessage("ID: " + deviceID);
            dialog.setView(editView);
            dialog.setPositiveButton("Login", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String pwstr = editView.getText().toString();
                    Log.d("CBR", "showLoginDialog: Login pw=" + pwstr);
                    doLogin(pwstr);
                }
            });
            dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Log.d("CBR", "showLoginDialog: Canceled");
                }
            });
            dialog.show();
        }
    }

    private void doLogin(String pwstr) {
        long pwnum = -1;
        try {
            pwnum = Long.parseLong(pwstr);
        } catch (NumberFormatException e) {
        }
        if (pwnum >= 0 && CBAuth.setPassword(this, pwnum)) {
            // login OK
            return;
        }

        // Show error dialog
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Error");
        dialog.setMessage("Bad password");
        dialog.setPositiveButton("OK", null);
        dialog.show();
    }

	public void onAutoFocusButton(View v) {
		Log.d("CBR", "onAutoFocusButton: " + focusButton.isChecked());
		doOneShotFocus();
	}
	
	private boolean onAutoFocusButtonLongPress(View v) {
		Log.d("CBR", "onAutoFocusButtonLongPress");
		setContinuousFocus();
		return true;
	}
	
	public void onLightButton(View v) {
		Log.d("CBR", "onLightButton: " + lightButton.isChecked());
		controller.setLight(lightButton.isChecked());
	}
	
	public void onSoundButton(View v) {
		Log.d("CBR", "onSoundButton: " + soundButton.isChecked());
		controller.setSoundOn(soundButton.isChecked());
	}

	private boolean onSingleTapUp(MotionEvent e) {
		Log.d("CBR", "onSingleTapUp");
		doOneShotFocus();
		return true;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return gestureDetector.onTouchEvent(event);
	}
	
	private void doOneShotFocus() {
		oneShotFocusMode = true;
		focusButton.setChecked(true);
		controller.doOneShotFocus(this);
		controller.setAutoExposureLock(false);
		
		if (focusTimeoutTask != null) {
			focusTimeoutTask.cancel();
		}
		focusTimeoutTask = new TimerTask() {
			@Override
			public void run() {
				handler.post(new Runnable() {
					@Override
					public void run() {
						Log.d("CBR", "One-shot focus timeout");
						MainActivity.this.setContinuousFocus();
					}
				});
			}
		};
		timer.schedule(focusTimeoutTask, ONE_SHOT_FOCUS_TIMEOUT_MSEC);
	}

	private void setContinuousFocus() {
		oneShotFocusMode = false;
		focusButton.setChecked(true);
		controller.setContinuousFocus();
		controller.setAutoExposureLock(false);
	}
	
	@Override
	public void onAutoFocus(boolean success, Camera camera) {
		Log.d("CBR", "Auto focus done: " + success);
		if (oneShotFocusMode) {
			focusButton.setChecked(false);
			controller.setAutoExposureLock(true);
		}
	}

}

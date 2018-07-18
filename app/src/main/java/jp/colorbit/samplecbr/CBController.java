package jp.colorbit.samplecbr;

import java.io.IOException;
import java.util.List;

import jp.colorbit.decodelib.CBCode;
import jp.colorbit.decodelib.CBDecoder;
import jp.colorbit.decodelib.CBUtils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.SoundPool;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;



public class CBController implements SurfaceHolder.Callback,
	Camera.PreviewCallback {
	
	private static final float TEXT_SIZE = 16;
	
	private final Activity activity;
	private final SurfaceTexture previewTexture;
	private final SurfaceView infoSurface;
	
	private float screenDensity;
	
	private Camera camera;
	private int resolutionIndex;
	private int camWidth, camHeight;
	private int canvasWidth, canvasHeight;
	private int imageOrientation;
	private Bitmap camBitmap;
	
	private String decodeParam;
	
	private final Paint textPaint;

	private final Paint linePaint;
	
	private final SoundPool soundPool;
	private int shutterSoundId;
	private boolean isSoundOn;

	static final String BR = System.getProperty("line.separator");/*改行コード*/

	public CBController(Activity activity, FrameLayout previewFrame)  {	//読み込んだ時に表示する枠やテキストの色の変更
		this.activity = activity;
		
		CBDecoder.initialize(CBDecoder.Preset.BASIC_CB);
		CBDecoder.setColorType(CBDecoder.ColorType.RGB_KM);

		previewTexture = new SurfaceTexture(0);

		infoSurface = new SurfaceView(activity);
		infoSurface.getHolder().addCallback(this);
		previewFrame.addView(infoSurface);
		
		screenDensity = activity.getResources().getDisplayMetrics().density;

		//テキスト文字
		textPaint = new Paint();
		textPaint.setColor(Color.BLACK);
		textPaint.setTextSize(TEXT_SIZE * screenDensity);
		textPaint.setTextAlign(Paint.Align.LEFT);
		textPaint.setTypeface(Typeface.DEFAULT_BOLD);

		//枠
		linePaint = new Paint();
		//linePaint.setColor(Color.WHITE);
		linePaint.setARGB(180,255,255,255);
		linePaint.setStyle(Paint.Style.FILL);
		//linePaint.setStrokeWidth(3);
		
		soundPool =	new SoundPool(4, AudioManager.STREAM_MUSIC, 0);		
		shutterSoundId = soundPool.load(activity, R.raw.shutter, 1);
	}

	public void onResume() {	//履歴関数？
		Log.d("CBR", "CBController: onResume");
		SharedPreferences prefs = PreferenceManager.
				getDefaultSharedPreferences(activity);
		String resolutionIndexStr = prefs.getString(
				SettingsActivity.KEY_RESOLUTION, "-1");
		resolutionIndex = -1;
		try {
			resolutionIndex = Integer.parseInt(resolutionIndexStr);
		} catch (NumberFormatException e) {
		}

		CBDecoder.Preset preset = (CBDecoder.Preset) getEnumPrefs(prefs,
				SettingsActivity.KEY_PRESET, CBDecoder.Preset.BASIC_CB);
		CBDecoder.ColorType colorType = (CBDecoder.ColorType) getEnumPrefs(prefs,
				SettingsActivity.KEY_COLOR_TYPE, CBDecoder.ColorType.RGB_KM);
		
		boolean downscale = prefs.getBoolean(SettingsActivity.KEY_DOWNSCALE,
				true);
		
		CBDecoder.initialize(preset);
		CBDecoder.setColorType(colorType);
		if (!downscale) {
			CBDecoder.setDownscale(1);
		}
		
		decodeParam = preset.name() + "," + colorType.name();
		if (!downscale) {
			decodeParam = decodeParam + ", downscale=1";
		}
		
		if (camera != null) {
			camera.startPreview();
		}
	}

	public void onPause() {	//止まるよ関数
		Log.d("CBR", "CBController: onPause");
		if (camera != null) {
			camera.stopPreview();
		}
	}
	
	public void setContinuousFocus() {
		if (camera != null) {
			Camera.Parameters params = camera.getParameters();
			List<String> focusModes = params.getSupportedFocusModes();
			if (focusModes
					.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
				params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
				camera.setParameters(params);
			} else {
				Log.e("CBR", "Continuous focus not supported");
			}
		}
	}
	
	public void doOneShotFocus(Camera.AutoFocusCallback callback) {	//オートフォーカス関連？
		if (camera != null) {
			Camera.Parameters params = camera.getParameters();
			List<String> focusModes = params.getSupportedFocusModes();
			if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
				params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
				camera.setParameters(params);
				camera.autoFocus(callback);
			} else {
				Log.e("CBR", "Auto focus not supported");
			}
		}
	}

	public void setAutoExposureLock(boolean lock) {
		if (camera != null) {
			Camera.Parameters params = camera.getParameters();
			if (params.isAutoExposureLockSupported()) {
				params.setAutoExposureLock(lock);
				camera.setParameters(params);
				Log.d("CBR", "setAutoExposureLock: " + lock);
			}
		}
	}

	public void setLight(boolean on) {	//フラッシュ関連？
		if (camera != null) {
			Camera.Parameters params = camera.getParameters();
			List<String> flashModes = params.getSupportedFlashModes();
			if (flashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
				if (on) {
					params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
				} else {
					params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
				}
				camera.setParameters(params);
			} else {
				Log.e("CBR", "Light not supported on this camera");
			}
		}
	}
	
	public void setSoundOn(boolean isSoundOn) {
		this.isSoundOn = isSoundOn;
	}

	public void onScreenTap(Camera.AutoFocusCallback callback) {
		if (camera != null) {
			camera.autoFocus(callback);
		}
	}

	private <T extends Enum<T>>	Enum<T> getEnumPrefs(SharedPreferences prefs,
			String key, Enum<T> defValue) {
		String str = prefs.getString(key, defValue.name());
		Enum<T> result = defValue;
		@SuppressWarnings("unchecked")
		Class<T> enumClass = (Class<T>) defValue.getClass();
		try {
			result = Enum.valueOf(enumClass, str);
		} catch (IllegalArgumentException e) {
			Log.e("CBR", "Unknown preference value: " + str +
					", key=" + key + ", defValue=" + defValue);
		}
		return result;
	}
	
	public List<Camera.Size> getCaptureSizeList() {
		if (camera != null) {
			Camera.Parameters p = camera.getParameters();
			return p.getSupportedPreviewSizes();
		} else {
			return null;
		}
	}
	
	private void drawInfoLayer(CBCode[] codes, long time)  {
		if (camBitmap == null) {
			return;
		}
		
		Canvas c = infoSurface.getHolder().lockCanvas();
		if (c == null) {
			Log.d("CBR", "Failed to lock info surface");
			return;
		}

		Matrix mat = new Matrix();
		if (imageOrientation == 90 || imageOrientation == 270) {
			mat.postScale((float) canvasHeight / (float) camBitmap.getWidth(),
					(float) canvasWidth / (float) camBitmap.getHeight());
			if (imageOrientation == 90) {
				mat.postRotate(90);
				mat.postTranslate(canvasWidth, 0);
			} else {
				mat.postRotate(270);
				mat.postTranslate(0, canvasHeight);
			}
		} else {
			mat.postScale((float) canvasWidth / (float) camBitmap.getWidth(),
					(float) canvasHeight / (float) camBitmap.getHeight());
			if (imageOrientation == 180) {
				mat.postRotate(180);
				mat.postTranslate(canvasWidth, canvasHeight);
			}
		}
		
		c.drawColor(0, PorterDuff.Mode.CLEAR);

		c.drawBitmap(camBitmap, mat, null);
		
		String infoText = "" + camBitmap.getWidth() + "x"
				+ camBitmap.getHeight() + ", "
				+ codes.length + " codes, " + time + " msec."
				+ " (" + CBDecoder.checkActivity() + ")";

		c.drawText(infoText, canvasWidth / 2, TEXT_SIZE * screenDensity,
				textPaint);
		c.drawText(decodeParam, canvasWidth / 2,
				2 * TEXT_SIZE * screenDensity, textPaint);
		c.drawText(CBDecoder.getDecoderName(), canvasWidth / 2,
                3 * TEXT_SIZE * screenDensity, textPaint);
		
		for (CBCode code: codes) {
			Point[] detectPolygon = code.getDetectPolygon();
			float[] points = new float[2 * detectPolygon.length];
			int i = 0;

			for (Point pt: detectPolygon) {
				points[i++] = pt.x;
				points[i++] = pt.y;
			}
			mat.mapPoints(points);

			Path path = new Path();
			float cx = 0, cy = 0;
			boolean first = true;
			for (i = 0; i < points.length; i += 2) {
				if (first) {
					path.moveTo(points[i], points[i + 1]);
					first = false;
				} else {
					path.lineTo(points[i], points[i + 1]);
				}
				cx += points[i];
				cy += points[i + 1];
			}
			path.close();
			c.drawPath(path, linePaint);

			cx /= detectPolygon.length;
			cy /= detectPolygon.length;

			//カラービットの内容表示？
			String memoid = CodeToString(code);
			String memo = loadData(memoid);
			String sbr;
			int j,k=1;
			//if(memo.indexOf(BR)>=1)memo="aaa";
			/*複数行ある時は改行*/
			for(j=0,i=0;i < memo.length();i++){
				sbr = String.valueOf(memo.charAt(i));
				if(sbr.equals(BR)) {
					c.drawText(memo.substring(j,i), cx-10, cy+k, textPaint);
					j=i+1;k+=50;
				}
			}
			if(j>=0) c.drawText(memo.substring(j,i), cx-10, cy+k, textPaint);
				else c.drawText(memo, cx, cy, textPaint);
			//c.drawText(memo, cx, cy, textPaint);
		}

		infoSurface.getHolder().unlockCanvasAndPost(c);
	}

	public String loadData(String find){
		CustomOpenHelper helper = new CustomOpenHelper(this.activity);
		SQLiteDatabase db = helper.getReadableDatabase();

		String query ="select body from MEMO_TABLE where status=0 AND uuid =" +find ;
		String result ="";

		if(db!=null){
			Cursor c =db.rawQuery(query,null);
			if(c.moveToFirst()){
				result=c.getString(0);
			}
		}
		//String result=find;
		if(result==""){
			result="メモが登録されていません";
		}
		return result;

	}
	// Camera.PreviewCallback
	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		CBUtils.convertNV21toBitmap(data, camWidth, camHeight, camBitmap);

		CBCode[] codes;
		long t0 = System.currentTimeMillis();
		codes = CBDecoder.decode(camBitmap);
		long d = System.currentTimeMillis() - t0;

		if (isSoundOn && codes.length > 0) {
			soundPool.play(shutterSoundId, 1, 1, 0, 0, 1);
		}

		drawInfoLayer(codes, d);


		camera.addCallbackBuffer(data);
	}
	
	// SurfaceHolder.Callback

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		canvasWidth = w;
		canvasHeight = h;
		float canvasAspect;
		if (imageOrientation == 90 || imageOrientation == 270) {
			canvasAspect = (float) h / (float) w;
		} else {
			canvasAspect = (float) w / (float) h;
		}
		int bmWidth = Math.min(camWidth, Math.round(camHeight * canvasAspect));
		int bmHeight = Math.min(camHeight, Math.round(bmWidth / canvasAspect));
		
		// image width/height must be multiples of 4.
		bmWidth &= ~3;
		bmHeight &= ~3;
		
		Log.d("CBR", "CBController: surfaceChanged:" +
				"canvasAspect=" + canvasAspect +
				", bmSize=" + bmWidth +	"x" + bmHeight);
		
		if (camBitmap != null) {
			if (camBitmap.getWidth() != bmWidth ||
				camBitmap.getHeight() != bmHeight) {
				camBitmap.recycle();
				camBitmap = null;
			}
		}
		if (camBitmap == null) {
			camBitmap = Bitmap.createBitmap(bmWidth, bmHeight,
					Bitmap.Config.ARGB_8888);
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d("CBR", "CBController: surfaceCreated");

		Camera.CameraInfo info = new Camera.CameraInfo();
		int numberOfCameras = Camera.getNumberOfCameras();
		// find first back-facing camera
		camera = null;
		for (int i = 0; i < numberOfCameras; i++) {
			Camera.getCameraInfo(i, info);
			if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
				camera = Camera.open(i);
				break;
			}
		}
		if (camera != null) {
			
			checkImageOrientation(info);
			try {
				camera.setPreviewTexture(previewTexture);
			} catch (IOException e) {
				throw new Error("setPreviewDisplay failed", e);
			}
			
			List<Camera.Size> sizeList = getCaptureSizeList();
			if (resolutionIndex < 0 || resolutionIndex >= sizeList.size()) {
				// Select default resolution:
				// Use highest resolution but not bigger than 720px
				int bestHeight = 0;
				for (int i = 0; i < sizeList.size(); i++) {
					Camera.Size size = sizeList.get(i);
					if (size.height <= 720 && size.height > bestHeight) {
						resolutionIndex = i;
						bestHeight = size.height;
					}
				}
				if (bestHeight == 0) {
					// No good resolution found, use first one
					resolutionIndex = 0;
				} else {
					SharedPreferences prefs = PreferenceManager.
							getDefaultSharedPreferences(activity);
					SharedPreferences.Editor ed = prefs.edit();
					ed.putString(SettingsActivity.KEY_RESOLUTION,
							"" + resolutionIndex);
					ed.commit();
				}
			}
			Camera.Size size = sizeList.get(resolutionIndex);
			camWidth = size.width;
			camHeight = size.height;
			
			int buflen = (int) Math.ceil(camWidth * camHeight * 1.5);
			camera.addCallbackBuffer(new byte[buflen]); 
			camera.addCallbackBuffer(new byte[buflen]); 
			camera.setPreviewCallbackWithBuffer(this);
			Camera.Parameters p = camera.getParameters();
			p.setPreviewSize(camWidth, camHeight);
			camera.setParameters(p);
			camera.startPreview();
			
			setContinuousFocus();
		}
	}

	private void checkImageOrientation(Camera.CameraInfo info) {
		int rotation = activity.getWindowManager().getDefaultDisplay()
				.getRotation();
		int degrees = 0;
		switch (rotation) {
		case Surface.ROTATION_0: degrees = 0; break;
		case Surface.ROTATION_90: degrees = 90; break;
		case Surface.ROTATION_180: degrees = 180; break;
		case Surface.ROTATION_270: degrees = 270; break;
		}
		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360; // compensate the mirror
		} else { // back-facing
			result = (info.orientation - degrees + 360) % 360;
		}		
		imageOrientation = result;
		Log.d("CBR", "imageOrientation=" + imageOrientation);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d("CBR", "CBController: surfaceDestroyed");
		if (camera != null) {
			camera.stopPreview();
			camera.release();
			camera = null;
		}
	}

	public String CodeToString(CBCode  codes){
		String memocode;
		memocode=Long.toString(codes.getId());
		return memocode;
	}


}

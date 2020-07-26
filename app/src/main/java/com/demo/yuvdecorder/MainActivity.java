package com.demo.yuvdecorder;

import android.R.bool;
import android.app.ActionBar;
import android.app.Activity;
import android.opengl.Visibility;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.os.Bundle;
import android.app.Activity;
import android.content.pm.ActivityInfo;

public class MainActivity extends ActionBarActivity implements OnItemSelectedListener,
		OnClickListener, IDecordState {
	private final int FileOpenFail = -1;
	private final int FileOpenSuccessful = 0;
	GL2JNIView mView;
	private String root = Environment.getExternalStorageDirectory().getPath();
	// private Button btDecorder;
	private Spinner spDecorderPath;
	private GL2JNIView view;
	private String filepath;
	private ProgressBar pbLoading;
	private ToggleButton tbDecorde;
	private ToggleButton tbPause;
	private boolean isFull=false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		view = (GL2JNIView) findViewById(R.id.gLJNIView1);
		view.setStateListener(this);
        //ActionBar aBar = getActionBar();
        //if(aBar!=null) {
           // aBar.hide();
       // }
		init();
		log("into oncreat.");

	}

	private void init() {
		spDecorderPath = (Spinner) findViewById(R.id.spDecordePath);
		spDecorderPath.setOnItemSelectedListener(this);
		pbLoading = (ProgressBar) findViewById(R.id.pbLoading);
		tbDecorde = (ToggleButton) findViewById(R.id.tbDecorde);
		tbDecorde.setOnClickListener(this);
		tbPause = (ToggleButton) findViewById(R.id.tbPause);
		tbPause.setOnClickListener(this);
		view.setOnClickListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		view.pauseDecorde(true);
		// view.onPause();
		log("onPause...");
	}

	@Override
	protected void onResume() {
		super.onResume();
		view.pauseDecorde(false);
		// view.onResume();
		log("onResume...");
	}

	void log(String str) {
		Log.d("chenxi", str + "  @MainActivity");
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {
		// TODO Auto-generated method stub

		switch (parent.getId()) {
		case R.id.spDecordePath:

			switch (position) {
			case 0:// 道通rtsp地址
				filepath = "rtsp://192.168.1.200:8557/PSIA/Streaming/channels/2?videoCodecType=H.264";
				break;
			case 1:// 无线相机模组rtsp地址
				filepath = "rtsp://192.168.1.200:554";
				break;
			case 2:// 无线相机模组rtsp地址
				filepath = "rtsp://192.168.1.201:554";
				break;
			case 3:// 本地视频地址
				filepath = root + "/sintel.ts";
				break;
			case 4:// 其他地址
				filepath="rtsp://184.72.239.149/vod/mp4:BigBuckBunny_115k.mov";
				break;
			default:
				break;
			}
			break;

		default:
			break;
		}

	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setRequestedOrientation(int requestedOrientation) {
		// TODO Auto-generated method stub
		
        switch (requestedOrientation) {  
        case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:  //横屏
            log("SCREEN_ORIENTATION_LANDSCAPE");  
            //setContentView(R.layout.main2);
    		//view = (GL2JNIView) findViewById(R.id.gLJNIView1);
    		//view.setStateListener(this);
    		//init();
            break;  
        case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:   //竖屏
        	 log("SCREEN_ORIENTATION_PORTRAIT"); 
        	 //setContentView(R.layout.activity_main);
     		//view = (GL2JNIView) findViewById(R.id.gLJNIView1);
    		//view.setStateListener(this);
    		//init();
        	 
            break;  
        default:  
            break;  
        }  
        super.setRequestedOrientation(requestedOrientation);
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		/*
		 * case R.id.btDecorde: view.deCorder(filepath);
		 * pbLoading.setVisibility(View.VISIBLE); break; case R.id.btStop:
		 * view.stopDecorde(); break; case R.id.btPause:
		 * view.pauseDecorde(true); break; case R.id.btPlay:
		 * view.pauseDecorde(false); break;
		 */
		case R.id.tbDecorde:
			if (tbDecorde.isChecked()) {
				view.deCorder(filepath);
				//pbLoading.setVisibility(View.VISIBLE);
			} else {
				view.stopDecorde();
				pbLoading.setVisibility(View.GONE);
			}
			break;
		case R.id.tbPause:
			if (tbPause.isChecked()) {
				view.pauseDecorde(true);
			} else {
				view.pauseDecorde(false);
			}
			break;
		case R.id.gLJNIView1:
			if (!isFull) {//进入全屏代码
				log("full");
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);  
			} else {//退出全屏代码
				log("no full");
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);  
			}
			isFull=!isFull;
			break;
		default:
			break;
		}
	}

	/*
	 * public void hideLoading() { pbLoading.setVisibility(View.GONE); }
	 */

	@Override
	public void openSusseccful() {
		// TODO Auto-generated method stub
		// pbLoading.setVisibility(View.GONE);
		Message msg = Message.obtain();
		msg.what = 0;
		handler.sendMessage(msg);
		log("openSusseccful~~~~~~~");
	}

	public Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case FileOpenFail:
				pbLoading.setVisibility(View.GONE);
				break;
			case FileOpenSuccessful:
				pbLoading.setVisibility(View.GONE);
				break;

			default:
				break;
			}
		}
	};

	@Override
	public void openFail() {
		// TODO Auto-generated method stub
		Message msg = Message.obtain();
		msg.what = -1;
		handler.sendMessage(msg);
		log("openfail~~~~~~~");
	}
}

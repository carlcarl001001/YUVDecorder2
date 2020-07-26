package com.demo.yuvdecorder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.R.integer;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;


class GL2JNIView extends GLSurfaceView implements GLSurfaceView.Renderer {
	private static String TAG = "GL2JNIView";
	private static final boolean DEBUG = false;
	private static Context mContext;
	private boolean surfaceCreated = false;
	private boolean decording = false;
	private boolean isFirst=true;
	private String root= Environment.getExternalStorageDirectory().getPath();
	private String filepath=root+"/yuv"+"/jpgimage1.yuv";//640*480

	private IDecordState openState;
/*	Bitmap bitmap = ((BitmapDrawable) context.getResources().getDrawable(
			R.drawable.red)).getBitmap();
	int col = bitmap.getWidth();
	int row = bitmap.getHeight();*/

	//int[] pix = new int[col * row];
/*	byte[] buf = GL2JNIView.getFromRaw(R.drawable.red);
	byte[] buf2 = GL2JNIView.getFromRaw(R.drawable.blu);
	byte[] YUV = GL2JNIView.getFromRaw(R.drawable.jpgimage1_image_640_480);
	byte[] YUV2 = GL2JNIView.getFromRaw(R.drawable.img640_480);
	byte[] YUV3 = GL2JNIView.getFromRaw(R.drawable.img640_480_2);*/

	public GL2JNIView(Context context) {
		super(context);
		surfaceCreated = true;
		mContext = context;
		//log("GL2JNIView");
		init(false, 0, 0);

	}

	public GL2JNIView(Context context, boolean translucent, int depth,
			int stencil) {
		super(context);
		surfaceCreated = true;
		mContext = context;
		//log("GL2JNIView");
		init(false, 0, 0);

	}

	public GL2JNIView(Context context, AttributeSet attrs) {
		super(context, attrs);
		surfaceCreated = true;
		mContext = context;
		log("GL2JNIView creat.");
		init(false, 0, 0);

	}

	private void init(boolean translucent, int depth, int stencil) {

		/*
		 * By default, GLSurfaceView() creates a RGB_565 opaque surface. If we
		 * want a translucent one, we should change the surface's format here,
		 * using PixelFormat.TRANSLUCENT for GL Surfaces is interpreted as any
		 * 32-bit surface with alpha by SurfaceFlinger.
		 */
		if (translucent) {
			this.getHolder().setFormat(PixelFormat.TRANSLUCENT);
		}

		/*
		 * Setup the context factory for 2.0 rendering. See ContextFactory class
		 * definition below
		 */
		setEGLContextFactory(new ContextFactory());

		/*
		 * We need to choose an EGLConfig that matches the format of our surface
		 * exactly. This is going to be done in our custom config chooser. See
		 * ConfigChooser class definition below.
		 */
		setEGLConfigChooser(translucent ? new ConfigChooser(8, 8, 8, 8, depth,
				stencil) : new ConfigChooser(5, 6, 5, 0, depth, stencil));

		/* Set the renderer responsible for frame rendering */
		setRenderer(this);
		setRenderMode(RENDERMODE_WHEN_DIRTY);
	}

	private static class ContextFactory implements
			EGLContextFactory {
		private static int EGL_CONTEXT_CLIENT_VERSION = 0x3098;

		public EGLContext createContext(EGL10 egl, EGLDisplay display,
				EGLConfig eglConfig) {
			Log.w(TAG, "creating OpenGL ES 2.0 context");
			checkEglError("Before eglCreateContext", egl);
			int[] attrib_list = { EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE };
			EGLContext context = egl.eglCreateContext(display, eglConfig,
					EGL10.EGL_NO_CONTEXT, attrib_list);
			checkEglError("After eglCreateContext", egl);
			return context;
		}

		public void destroyContext(EGL10 egl, EGLDisplay display,
				EGLContext context) {
			Log.d("chenxi", "into destroyContext.");
			egl.eglDestroyContext(display, context);
		}
	}

	private static void checkEglError(String prompt, EGL10 egl) {
		int error;
		while ((error = egl.eglGetError()) != EGL10.EGL_SUCCESS) {
			Log.e(TAG, String.format("%s: EGL error: 0x%x", prompt, error));
		}
	}

	private static class ConfigChooser implements
			EGLConfigChooser {

		public ConfigChooser(int r, int g, int b, int a, int depth, int stencil) {
			mRedSize = r;
			mGreenSize = g;
			mBlueSize = b;
			mAlphaSize = a;
			mDepthSize = depth;
			mStencilSize = stencil;
		}

		/*
		 * This EGL config specification is used to specify 2.0 rendering. We
		 * use a minimum size of 4 bits for red/green/blue, but will perform
		 * actual matching in chooseConfig() below.
		 */
		private static int EGL_OPENGL_ES2_BIT = 4;
		private static int[] s_configAttribs2 = { EGL10.EGL_RED_SIZE, 4,
				EGL10.EGL_GREEN_SIZE, 4, EGL10.EGL_BLUE_SIZE, 4,
				EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT, EGL10.EGL_NONE };

		public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {

			/*
			 * Get the number of minimally matching EGL configurations
			 */
			int[] num_config = new int[1];
			egl.eglChooseConfig(display, s_configAttribs2, null, 0, num_config);

			int numConfigs = num_config[0];

			if (numConfigs <= 0) {
				throw new IllegalArgumentException(
						"No configs match configSpec");
			}

			/*
			 * Allocate then read the array of minimally matching EGL configs
			 */
			EGLConfig[] configs = new EGLConfig[numConfigs];
			egl.eglChooseConfig(display, s_configAttribs2, configs, numConfigs,
					num_config);

			if (DEBUG) {
				printConfigs(egl, display, configs);
			}
			/*
			 * Now return the "best" one
			 */
			return chooseConfig(egl, display, configs);
		}

		public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display,
				EGLConfig[] configs) {
			for (EGLConfig config : configs) {
				int d = findConfigAttrib(egl, display, config,
						EGL10.EGL_DEPTH_SIZE, 0);
				int s = findConfigAttrib(egl, display, config,
						EGL10.EGL_STENCIL_SIZE, 0);

				// We need at least mDepthSize and mStencilSize bits
				if (d < mDepthSize || s < mStencilSize)
					continue;

				// We want an *exact* match for red/green/blue/alpha
				int r = findConfigAttrib(egl, display, config,
						EGL10.EGL_RED_SIZE, 0);
				int g = findConfigAttrib(egl, display, config,
						EGL10.EGL_GREEN_SIZE, 0);
				int b = findConfigAttrib(egl, display, config,
						EGL10.EGL_BLUE_SIZE, 0);
				int a = findConfigAttrib(egl, display, config,
						EGL10.EGL_ALPHA_SIZE, 0);

				if (r == mRedSize && g == mGreenSize && b == mBlueSize
						&& a == mAlphaSize)
					return config;
			}
			return null;
		}

		private int findConfigAttrib(EGL10 egl, EGLDisplay display,
				EGLConfig config, int attribute, int defaultValue) {

			if (egl.eglGetConfigAttrib(display, config, attribute, mValue)) {
				return mValue[0];
			}
			return defaultValue;
		}

		private void printConfigs(EGL10 egl, EGLDisplay display,
				EGLConfig[] configs) {
			int numConfigs = configs.length;
			Log.w(TAG, String.format("%d configurations", numConfigs));
			for (int i = 0; i < numConfigs; i++) {
				Log.w(TAG, String.format("Configuration %d:\n", i));
				printConfig(egl, display, configs[i]);
			}
		}

		private void printConfig(EGL10 egl, EGLDisplay display, EGLConfig config) {
			int[] attributes = { EGL10.EGL_BUFFER_SIZE, EGL10.EGL_ALPHA_SIZE,
					EGL10.EGL_BLUE_SIZE,
					EGL10.EGL_GREEN_SIZE,
					EGL10.EGL_RED_SIZE,
					EGL10.EGL_DEPTH_SIZE,
					EGL10.EGL_STENCIL_SIZE,
					EGL10.EGL_CONFIG_CAVEAT,
					EGL10.EGL_CONFIG_ID,
					EGL10.EGL_LEVEL,
					EGL10.EGL_MAX_PBUFFER_HEIGHT,
					EGL10.EGL_MAX_PBUFFER_PIXELS,
					EGL10.EGL_MAX_PBUFFER_WIDTH,
					EGL10.EGL_NATIVE_RENDERABLE,
					EGL10.EGL_NATIVE_VISUAL_ID,
					EGL10.EGL_NATIVE_VISUAL_TYPE,
					0x3030, // EGL10.EGL_PRESERVED_RESOURCES,
					EGL10.EGL_SAMPLES,
					EGL10.EGL_SAMPLE_BUFFERS,
					EGL10.EGL_SURFACE_TYPE,
					EGL10.EGL_TRANSPARENT_TYPE,
					EGL10.EGL_TRANSPARENT_RED_VALUE,
					EGL10.EGL_TRANSPARENT_GREEN_VALUE,
					EGL10.EGL_TRANSPARENT_BLUE_VALUE,
					0x3039, // EGL10.EGL_BIND_TO_TEXTURE_RGB,
					0x303A, // EGL10.EGL_BIND_TO_TEXTURE_RGBA,
					0x303B, // EGL10.EGL_MIN_SWAP_INTERVAL,
					0x303C, // EGL10.EGL_MAX_SWAP_INTERVAL,
					EGL10.EGL_LUMINANCE_SIZE, EGL10.EGL_ALPHA_MASK_SIZE,
					EGL10.EGL_COLOR_BUFFER_TYPE, EGL10.EGL_RENDERABLE_TYPE,
					0x3042 // EGL10.EGL_CONFORMANT
			};
			String[] names = { "EGL_BUFFER_SIZE", "EGL_ALPHA_SIZE",
					"EGL_BLUE_SIZE", "EGL_GREEN_SIZE", "EGL_RED_SIZE",
					"EGL_DEPTH_SIZE", "EGL_STENCIL_SIZE", "EGL_CONFIG_CAVEAT",
					"EGL_CONFIG_ID", "EGL_LEVEL", "EGL_MAX_PBUFFER_HEIGHT",
					"EGL_MAX_PBUFFER_PIXELS", "EGL_MAX_PBUFFER_WIDTH",
					"EGL_NATIVE_RENDERABLE", "EGL_NATIVE_VISUAL_ID",
					"EGL_NATIVE_VISUAL_TYPE", "EGL_PRESERVED_RESOURCES",
					"EGL_SAMPLES", "EGL_SAMPLE_BUFFERS", "EGL_SURFACE_TYPE",
					"EGL_TRANSPARENT_TYPE", "EGL_TRANSPARENT_RED_VALUE",
					"EGL_TRANSPARENT_GREEN_VALUE",
					"EGL_TRANSPARENT_BLUE_VALUE", "EGL_BIND_TO_TEXTURE_RGB",
					"EGL_BIND_TO_TEXTURE_RGBA", "EGL_MIN_SWAP_INTERVAL",
					"EGL_MAX_SWAP_INTERVAL", "EGL_LUMINANCE_SIZE",
					"EGL_ALPHA_MASK_SIZE", "EGL_COLOR_BUFFER_TYPE",
					"EGL_RENDERABLE_TYPE", "EGL_CONFORMANT" };
			int[] value = new int[1];
			for (int i = 0; i < attributes.length; i++) {
				if (egl.eglGetConfigAttrib(display, config, attributes[i],
						value)) {
					Log.w(TAG, String.format("  %s: %d\n", names[i], value[0]));
				} else {
					// Log.w(TAG, String.format("  %s: failed\n", name));
					while (egl.eglGetError() != EGL10.EGL_SUCCESS)
						;
				}
			}
		}

		// Subclasses can adjust these values:
		protected int mRedSize;
		protected int mGreenSize;
		protected int mBlueSize;
		protected int mAlphaSize;
		protected int mDepthSize;
		protected int mStencilSize;
		private int[] mValue = new int[1];
	}
	@Override
	public void onDrawFrame(GL10 gl) {
		GL2JNILib.drawTexture();
	}
	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {

		// YUV=GL2JNIView.getFromRaw(R.drawable.img640_480);
		GL2JNILib.init(getWidth(), getHeight());
		log("into onSurfaceChanged");
		

	}
	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		//log("isFirst:"+isFirst);
		//if(isFirst)
		//{
		//GL2JNILib.init(getWidth(), getHeight());
		//isFirst=false;
		//log("into onSurfaceCreated");
		//}
	}

	public void ReDraw() {// jni层解码以后的数据回调，然后由系统调用onDrawFrame显示
		if (surfaceCreated) {
			this.requestRender();
		}
	}
	public void setStateListener(IDecordState Listener)
	{
		this.openState=Listener;
	}
	public void OpenSuccessful()//********************************************************
	{
		log("into OpenSuccessful().");
		openState.openSusseccful();
	}
	public void OpenFail()//********************************************************
	{
		log("into Fail().");
		openState.openSusseccful();
	}
	Thread decThread;
	public void deCorder(String path) {
		if (!decording) {
			decording = true;
			decordeRunnable dec = new decordeRunnable(path, this);
			decThread=new Thread(dec);
			decThread.start();
		}
	}
	public void stopDecorde()
	{
		if(decThread != null && decThread.isAlive()){
            //Log.e("readCacheThread", "thread interrupt_1");
			GL2JNILib.stopDecorde();
			decThread.interrupt();
            //Log.e("status", ""+readCacheThread.isInterrupted());
			log("stop Decorder.");
			decThread=null;
        }
		
	}
	public void pauseDecorde(boolean b)
	{	
		if (decording == true)
		GL2JNILib.pauseDecorde(b);
	}

	public class decordeRunnable implements Runnable {

		String path;
		Object glSurface;

		public decordeRunnable(String path, Object glSurface) {
			this.path = path;
			this.glSurface = glSurface;
		}

		@Override
		public void run() {
			synchronized (this) {
				//解码视频
				int data = GL2JNILib.decorde2Show(path, glSurface);
				Message msg = Message.obtain();
				msg.what = data;
				handler.sendMessage(msg);
				//解码图片
			/*	log("into run.");
				byte[]YUV=getFromSDCard(filepath);
				GL2JNILib.yuv2Show(YUV,640,480,glSurface);*/
			}
		}
	}

	public Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0:
				log("Decorde successfull.");
				decording = false;
				break;
			case -1:
				Toast.makeText(mContext, "Couldn't open input stream.",
						Toast.LENGTH_LONG).show();
				decording = false;
				break;
			case -2:
				Toast.makeText(mContext, "Couldn't find stream information.",
						Toast.LENGTH_LONG).show();
				decording = false;
				break;
			case -3:
				Toast.makeText(mContext, "Didn't find a video stream.",
						Toast.LENGTH_LONG).show();
				decording = false;
				break;
			case -4:
				Toast.makeText(mContext, "Codec not found.", Toast.LENGTH_LONG)
						.show();
				decording = false;
				break;
			case -5:
				Toast.makeText(mContext, "Could not open codec.",
						Toast.LENGTH_LONG).show();
				decording = false;
				break;

			default:
				decording = false;
				break;
			}
		}
	};
	public byte[] getFromSDCard(String path) {
		log("path:"+path);
		File file = new File(path);
		if (!file.exists()) {
			log("文件不存在。");
		}else {
			InputStream in = null;
			try {
				in = new FileInputStream(file);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			int length = 0;
			try {
				length = in.available();
			} catch (IOException e) {
				e.printStackTrace();
			}
			byte[] buffer = new byte[length];
			try {
				in.read(buffer);
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return buffer;
		}


		return null;
	}
	public static byte[] getFromRaw(int id) {
		try {
			InputStream in = mContext.getResources().openRawResource(id);
			int length = in.available();
			byte[] buffer = new byte[length];
			in.read(buffer);
			in.close();

			return buffer;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	void log(String str) {
		Log.d("chenxi", str + "  @GL2JNIView");
	}
}

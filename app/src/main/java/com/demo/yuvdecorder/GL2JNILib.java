package com.demo.yuvdecorder;

public class GL2JNILib {
static{
    System.loadLibrary("Decorder2Show");
}
public static native void init(int width,int height);
public static native void drawTexture();
public static native void stopDecorde();
public static native void pauseDecorde(boolean b);
public static native int decorde2Show(String path,Object glSurface);
public static native void yuv2Show(byte []data,int width,int height,Object glSurface);
}
#include <jni.h>
#include <android/log.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <pthread.h>
#include <unistd.h>//sleep的头文件
#include "GL2JNILib.h"

extern "C"
{
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libswscale/swscale.h>
}
#define  LOG_TAG    "chenxi"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)

using namespace std;
pthread_t thread;
#define  uchar unsigned char
static void printGLString(const char* name, GLenum s){
    const char* v = (const char *) glGetString(s);
    LOGI("GL %s = %s\n", name, v);
}


void checkGlError(const char* op) {
    for (GLint error = glGetError();
    error;
    error = glGetError()) {
        LOGI("after %s() glError (0x%x)\n", op, error);
    }
}

bool beginDecorde =false;
bool stopDecorde=false;
bool pauseDecorde=false;
static const char gVertexShader[] =
    "uniform   mat4 u_matrix;       \n"
"attribute vec4 a_position;     \n"
"attribute vec2 a_texture;      \n"
"varying   vec2 v_texture;      \n"
    "void main() {                  \n"
    "  gl_Position = a_position;    \n"
"  v_texture  = a_texture;      \n"
    "}                              \n";
static const char gFragmentShader[] =
    "precision mediump      float;                     \n"
"varying      vec2  v_texture;                     \n"
"uniform sampler2D  u_texture;                     \n"
    "void main() {                                     \n"
    "  gl_FragColor = texture2D(u_texture,v_texture);  \n"
    "}                                                 \n";




const GLfloat gImageVertices[] = {
0.0f,  0.0f,//0.0f,  0.0f,
-1.0f, -1.0f,//-0.9f,-0.9f
1.0f, -1.0f,//0.9f, -0.9f,
1.0f,  1.0f,//0.9f,  0.9f,
-1.0f,  1.0f,//-0.9f,  0.9f,
-1.0f, -1.0f,//-0.9f, -0.9f,
};
const GLfloat gImageFragment[]={
0.5f,  0.5f,//0.5f,  0.5f,
0.0f,  1.0f,
1.0f,  1.0f,
1.0f,  0.0f,
0.0f,  0.0f,
0.0f,  1.0f
};
/*struct decordePara
{
	const char* path;
    JNIEnv * env;
    jobject surface;
    int num;
};*/

uchar *textureBuf;
int fwidth,fheight;
int winWidth,winHeight;

/*//#if 1
JNIEXPORT void JNICALL Java_com_demo_yuvdecorder_GL2JNILib_drawTexture(
JNIEnv * env, jclass obj)
{

}
JNIEXPORT void JNICALL Java_com_demo_yuvdecorder_GL2JNILib_stopDecorde(
JNIEnv * env, jclass obj)
{

}
JNIEXPORT void JNICALL Java_com_demo_yuvdecorder_GL2JNILib_pauseDecorde(
JNIEnv * env, jclass obj,jboolean b)
{

}
JNIEXPORT void JNICALL Java_com_demo_yuvdecorder_GL2JNILib_init(
JNIEnv * env, jclass obj,  jint width, jint height)
{
    LOGD("into GL2JNILib init.");
}


JNIEXPORT int JNICALL Java_com_demo_yuvdecorder_GL2JNILib_decorde2Show//~~~~~~~~~~~~~decorder~~~~~~~~~~~~~~
  (JNIEnv * env, jclass,jstring path,jobject surface)
{

			return 0;

}*/

/*#else*/
JNIEXPORT void JNICALL Java_com_demo_yuvdecorder_GL2JNILib_drawTexture(
JNIEnv * env, jclass obj)
{
	LOGD("into drawTexture fwidth:%d,fheight:%d",fwidth,fheight);
	if((fwidth!=0)&&(fheight!=0))
	{

		RenderTexture(textureBuf, fwidth,fheight);
	}

}
JNIEXPORT void JNICALL Java_com_demo_yuvdecorder_GL2JNILib_stopDecorde(
JNIEnv * env, jclass obj)
{
	stopDecorde=true;
	LOGD("you into stopDecorde.");
}
JNIEXPORT void JNICALL Java_com_demo_yuvdecorder_GL2JNILib_pauseDecorde(
JNIEnv * env, jclass obj,jboolean b)
{
	pauseDecorde=b;
//	LOGD("pauseDecorde:%d.",pauseDecorde);
}
JNIEXPORT void JNICALL Java_com_demo_yuvdecorder_GL2JNILib_init(
JNIEnv * env, jclass obj,  jint width, jint height)
{
	winWidth=width;
	winHeight=height;
	LOGD("winWidth:%d,winHeight:%d",winWidth,winHeight);
	InitSetup(width,height);
}


JNIEXPORT int JNICALL Java_com_demo_yuvdecorder_GL2JNILib_decorde2Show//~~~~~~~~~~~~~decorder~~~~~~~~~~~~~~
  (JNIEnv * env, jclass,jstring path,jobject surface)
{
    LOGD("into decorde2Show.6565...");
	//starThread();
	beginDecorde=true;
	stopDecorde=false;
	pauseDecorde=false;
	LOGD("into decorde2Show....");
	jmethodID redrawCid=getJavaFuncID(env,"com/demo/yuvdecorder/GL2JNIView","ReDraw","()V");
	jmethodID OpenSuccessfulCid=getJavaFuncID(env,"com/demo/yuvdecorder/GL2JNIView","OpenSuccessful","()V");
	jmethodID OpenFailCid=getJavaFuncID(env,"com/demo/yuvdecorder/GL2JNIView","OpenFail","()V");
	AVFormatContext   * pFormatCtx;
	int                      i, videoindex;
	AVCodecContext    * pCodecCtx;
	AVCodec                  *pCodec;
	AVFrame     *pFrame, *pFrameYUV;
	uint8_t * out_buffer;
	AVPacket * packet;
	int y_size;
	int ret, got_picture;
	struct SwsContext *img_convert_ctx;
	//注册所有组件
	av_register_all();
	//初始化所有网络组件
	avformat_network_init();
	//为formateContext分配内存
	pFormatCtx = avformat_alloc_context();

	const char *filename = env->GetStringUTFChars(path, NULL);
	//const char *filename="/storage/emulated/0/sintel.ts";//
	//const char *filename="rtsp://192.168.1.200:554";//
	//const char *filename="rtsp://192.168.1.200:8557/PSIA/Streaming/channels/2?videoCodecType=H.264";//

	LOGD("Open file:%s",filename);
	//打开一个视频/音频数据流,并且读取该流的头文件信息
	if (avformat_open_input(&pFormatCtx, filename, NULL, NULL) != 0)
	{
		LOGD("Couldn't open input stream.");
		env->CallVoidMethod(surface, OpenFailCid);///调用java class
		return -1;
	}
	//获得一帧压缩编码数据流信息
	if (avformat_find_stream_info(pFormatCtx, NULL)<0)
	{
		LOGD("Couldn't find stream information.");
		env->CallVoidMethod(surface, OpenFailCid);///调用java class
		return -2;
	}
	//查找视频流的通道
	videoindex = -1;
	for (i = 0; i< pFormatCtx->nb_streams; i++)
	if (pFormatCtx->streams[i]->codec->codec_type == AVMEDIA_TYPE_VIDEO)
	{
		videoindex = i;
		break;
	}
	if (videoindex == -1)
	{
		LOGD("Didn't find a video stream.\n");
		env->CallVoidMethod(surface, OpenFailCid);///调用java class
		return -3;
	}
	//获得视频流的编码信息
	pCodecCtx = pFormatCtx->streams[videoindex]->codec;

	//查找视解码器的信息
	pCodec = avcodec_find_decoder(pCodecCtx->codec_id);
	if (pCodec == NULL)
	{
		LOGD("Codec not found.\n");
		env->CallVoidMethod(surface, OpenFailCid);///调用java class
		return -4;
	}
	//打开解码器
	if (avcodec_open2(pCodecCtx, pCodec, NULL)<0)
	{
		LOGD("Could not open codec.\n");
		env->CallVoidMethod(surface, OpenFailCid);///调用java class
		return -5;
	}

	env->CallVoidMethod(surface, OpenSuccessfulCid);///调用java class
			LOGD("file width:%d,height:%d",pCodecCtx->width,pCodecCtx->height);
			fwidth=pCodecCtx->width;
			fheight=pCodecCtx->height;
			textureBuf= (uchar*)malloc(sizeof(uchar) * (fwidth*fheight*3));
			// 分配一帧的内存空间
			pFrame = av_frame_alloc();
			pFrameYUV = av_frame_alloc();
			out_buffer = (uint8_t *)av_malloc(avpicture_get_size(PIX_FMT_YUV420P, pCodecCtx->width, pCodecCtx->height));
			//
			avpicture_fill((AVPicture *)pFrameYUV, out_buffer, PIX_FMT_YUV420P, pCodecCtx->width, pCodecCtx->height);


			int frame_cnt;
			int pixel_w = pCodecCtx->width, pixel_h = pCodecCtx->height;
			packet = (AVPacket *)av_malloc(sizeof(AVPacket));
			av_dump_format(pFormatCtx, 0, filename, 0);
			img_convert_ctx = sws_getContext(pCodecCtx->width, pCodecCtx->height, pCodecCtx->pix_fmt,
			pCodecCtx->width, pCodecCtx->height, PIX_FMT_YUV420P, SWS_BICUBIC, NULL, NULL, NULL);
			while(!stopDecorde)
			{
				//LOGD("Decording.....\n");//不加这句
				if(!pauseDecorde)
				{	//LOGD("pauseDecorde=false\n");
					while ((av_read_frame(pFormatCtx, packet) >= 0)&&(!pauseDecorde))//stopDecorde=true时，停止解码
					{
						//LOGD(".....................dd");s
						//LOGD("av_read_frame(pFormatCtx, packet):%d",av_read_frame(pFormatCtx, packet));
						if (packet->stream_index == videoindex)
						{
							ret = avcodec_decode_video2(pCodecCtx, pFrame, &got_picture, packet);
							if (ret < 0)
							{
								LOGD("Decode Error.\n");
								break;
							}
							if (got_picture)
							{
								sws_scale(img_convert_ctx, (const uint8_t * const*)pFrame->data, pFrame->linesize, 0, pCodecCtx->height, pFrameYUV->data, pFrameYUV->linesize);
								textureBuf=pFrameYUV->data[0];
								//usleep(40000);
								env->CallVoidMethod(surface, redrawCid);///调用java class
							}
						}
						if(stopDecorde){
							break;
						}
					}
					av_free_packet(packet);
				}
				usleep(1);
				//break;
			}
			LOGD("Decode finish.\n");
			beginDecorde=false;
			sws_freeContext(img_convert_ctx);

			av_frame_free(&pFrameYUV);
			av_frame_free(&pFrame);
			avcodec_close(pCodecCtx);
			avformat_close_input(&pFormatCtx);
			free(textureBuf);
			return 0;

}
//#endif*/
/*
 * Class:     com_demo_yuvdecorder_GL2JNILib
 * Method:    yuv2Show
 * Signature: ([B)V
 */
JNIEXPORT void JNICALL Java_com_demo_yuvdecorder_GL2JNILib_yuv2Show
(JNIEnv * env, jclass, jbyteArray data,int32_t width, int32_t height,jobject surface){
	LOGI("into yuv2Show");
	fwidth=width;
	fheight=height;
	jmethodID redrawCid=getJavaFuncID(env,"com/demo/yuvdecorder/GL2JNIView","ReDraw","()V");
	textureBuf= (uchar*)malloc(sizeof(uchar) * (fwidth*fheight*3));
	textureBuf=byteArrayToByte(env,data);
	while(1){
	env->CallVoidMethod(surface, redrawCid);///调用java class
	}
}
jmethodID getJavaFuncID(JNIEnv * env,const char *classname,const char *function,const char *style)
{
	 jclass _javaRenderClass;
	 jobject _javaRenderobject;
	 jmethodID      _redrawCid;
	 //jclass javaRenderClassLocal = reinterpret_cast<jclass> (env->FindClass("com/demo/openglshow/GL2JNIView"));
	 jclass javaRenderClassLocal = reinterpret_cast<jclass> (env->FindClass(classname));
	 if (!javaRenderClassLocal) {
               LOGE("could not find GL2JNIView");
	 }
	 _javaRenderClass = reinterpret_cast<jclass> (env->NewGlobalRef(javaRenderClassLocal));
	 _javaRenderobject = reinterpret_cast<jobject> (env->NewGlobalRef(javaRenderClassLocal));
	 if (!_javaRenderClass) {
		 LOGE("could not create Java SurfaceHolder class reference");
	 }
	 //_redrawCid = env->GetMethodID(_javaRenderClass, "ReDraw", "()V");
	 _redrawCid = env->GetMethodID(_javaRenderClass, function, style);
	 if (_redrawCid == NULL) {
    	LOGE("could not get Function ID");
	 }
	 else
	 {
    	LOGD("successful get Function ID");
	 }
	 return _redrawCid;
}
unsigned char *byteArrayToByte(JNIEnv* env, jbyteArray byteArray) {

	jbyte *pjb = (jbyte *)env->GetByteArrayElements(byteArray, 0);
	jsize jlen = env->GetArrayLength(byteArray);
	int len = (int) jlen;
	uchar *byBuf = NULL;
	if (len > 0) {
		byBuf = (uchar*) malloc(len + 1);
		memcpy(byBuf, pjb, len);
		byBuf[len] = '\0';
	}
	else {
		byBuf = (uchar*) malloc(1);
		byBuf[0] = '\0';
	}
	env->ReleaseByteArrayElements(byteArray, pjb, 0);
	return byBuf;
}

//********************************************************************************************
int32_t _id;
GLuint _textureIds[3]; // Texture id of Y,U and V texture.
GLuint _program;
GLuint _vPositionHandle;
GLsizei _textureWidth;
GLsizei _textureHeight;

GLfloat _vertices[20];

GLuint a_position_h;//a_position;
GLuint a_texture_h;//a_texture;
static const char g_indices[]={ 0, 3, 2, 0, 2, 1 };
static const char g_vertextShader[] = {
	    "attribute vec4 aPosition;\n"
	    "attribute vec2 aTextureCoord;\n"
	    "varying vec2 vTextureCoord;\n"
	    "void main() {\n"
	    "  gl_Position = aPosition;\n"
	    "  vTextureCoord = aTextureCoord;\n"
	    "}\n" };
static const char g_fragmentShader[] = {
	    "precision mediump float;\n"
	    "uniform sampler2D Ytex;\n"
	    "uniform sampler2D Utex,Vtex;\n"
	    "varying vec2 vTextureCoord;\n"
	    "void main(void) {\n"
	    "  float nx,ny,r,g,b,y,u,v;\n"
	    "  mediump vec4 txl,ux,vx;"
	    "  nx=vTextureCoord[0];\n"
	    "  ny=vTextureCoord[1];\n"
	    "  y=texture2D(Ytex,vec2(nx,ny)).r;\n"
	    "  u=texture2D(Utex,vec2(nx,ny)).r;\n"
	    "  v=texture2D(Vtex,vec2(nx,ny)).r;\n"

	    //"  y = v;\n"+
	    "  y=1.1643*(y-0.0625);\n"
	    "  u=u-0.5;\n"
	    "  v=v-0.5;\n"

	    "  r=y+1.5958*v;\n"
	    "  g=y-0.39173*u-0.81290*v;\n"
	    "  b=y+2.017*u;\n"
	    "  gl_FragColor=vec4(r,g,b,1.0);\n"
	    "}\n" };

int32_t InitSetup(int32_t width, int32_t height) {//这个宽度和高度是播放器的宽高
	LOGD("InitSetup width:%d,height:%d",width,height);
/*    printGLString("Version", GL_VERSION);
    printGLString("Vendor", GL_VENDOR);
    printGLString("Renderer", GL_RENDERER);
    printGLString("Extensions", GL_EXTENSIONS);*/

    int maxTextureImageUnits[2];
    int maxTextureSize[2];
    glGetIntegerv(GL_MAX_TEXTURE_IMAGE_UNITS, maxTextureImageUnits);
    glGetIntegerv(GL_MAX_TEXTURE_SIZE, maxTextureSize);

    _program = createProgram(g_vertextShader, g_fragmentShader);
    if (!_program) {
    	LOGE("Could not create program.");
        return -1;
    }

    a_position_h = glGetAttribLocation(_program, "aPosition");
    checkGlError("glGetAttribLocation aPosition");
    if (a_position_h == -1) {
    	LOGE("Could not get aPosition handle.");
        return -1;
    }

    a_texture_h = glGetAttribLocation(_program, "aTextureCoord");
    checkGlError("glGetAttribLocation aTextureCoord");
    if (a_texture_h == -1) {
    	LOGE("Could not get aTextureCoord handle./r/n");
        return -1;
    }

    // set the vertices array in the shader
    // _vertices contains 4 vertices with 5 coordinates.
    // 3 for (xyz) for the vertices and 2 for the texture
    glVertexAttribPointer(a_position_h, 3, GL_FLOAT, false,
                          5 * sizeof(GLfloat), _vertices);
    checkGlError("glVertexAttribPointer aPosition");

    glEnableVertexAttribArray(a_position_h);
    checkGlError("glEnableVertexAttribArray positionHandle");

    // set the texture coordinate array in the shader
    // _vertices contains 4 vertices with 5 coordinates.
    // 3 for (xyz) for the vertices and 2 for the texture
    glVertexAttribPointer(a_texture_h, 2, GL_FLOAT, false, 5
                          * sizeof(GLfloat), &_vertices[3]);
    checkGlError("glVertexAttribPointer maTextureHandle");
    glEnableVertexAttribArray(a_texture_h);
    checkGlError("glEnableVertexAttribArray textureHandle");

    glUseProgram(_program);
    int i = glGetUniformLocation(_program, "Ytex");
    checkGlError("glGetUniformLocation");
    glUniform1i(i, 0); //Bind Ytex to texture unit 0
    checkGlError("glUniform1i Ytex");

    i = glGetUniformLocation(_program, "Utex");
    checkGlError("glGetUniformLocation Utex");
    glUniform1i(i, 1); // Bind Utex to texture unit 1
    checkGlError("glUniform1i Utex");

    i = glGetUniformLocation(_program, "Vtex");
    checkGlError("glGetUniformLocation");
    glUniform1i(i, 2); //Bind Vtex to texture unit 2
    checkGlError("glUniform1i");

    glViewport(0, 0, width, height);
    checkGlError("glViewport");
    return 0;
}
void InitializeTexture(int name, int id, int width, int height) {
    glActiveTexture(name);
    glBindTexture(GL_TEXTURE_2D, id);
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, width, height, 0,
                 GL_LUMINANCE, GL_UNSIGNED_BYTE, NULL);
}
void SetupTextures(int32_t width, int32_t height)
{
    glDeleteTextures(3, _textureIds);
    glGenTextures(3, _textureIds); //Generate  the Y, U and V texture
    InitializeTexture(GL_TEXTURE0, _textureIds[0], width, height);
    InitializeTexture(GL_TEXTURE1, _textureIds[1], width / 2, height / 2);
    InitializeTexture(GL_TEXTURE2, _textureIds[2], width / 2, height / 2);

    checkGlError("SetupTextures");

    _textureWidth = width;
    _textureHeight = height;
}
void UpdateTextures(void* data, int32_t widht, int32_t height)
{
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, _textureIds[0]);
    glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, widht, height, GL_LUMINANCE, GL_UNSIGNED_BYTE,
                    data);

    glActiveTexture(GL_TEXTURE1);
    glBindTexture(GL_TEXTURE_2D, _textureIds[1]);
    glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, widht / 2, height / 2, GL_LUMINANCE,
                    GL_UNSIGNED_BYTE, (char *)data + widht * height);

    glActiveTexture(GL_TEXTURE2);
    glBindTexture(GL_TEXTURE_2D, _textureIds[2]);
    glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, widht / 2, height / 2, GL_LUMINANCE,
                    GL_UNSIGNED_BYTE, (char *)data + widht * height * 5 / 4);

    checkGlError("UpdateTextures");
}
int32_t RenderTexture(void * data, int32_t widht, int32_t height)
{

    glUseProgram(_program);
    checkGlError("glUseProgram");

    if (_textureWidth != (GLsizei) widht || _textureHeight != (GLsizei) height) {
        SetupTextures(widht, height);
    }
    if(beginDecorde==true)
    {
    UpdateTextures(data, widht, height);

    glVertexAttribPointer(a_position_h, 2, GL_FLOAT, GL_FALSE, 0, gImageVertices);//ap
    glEnableVertexAttribArray(a_position_h);//eva
    glVertexAttribPointer(a_texture_h,  2, GL_FLOAT, GL_FALSE, 0, gImageFragment);
    glEnableVertexAttribArray(a_texture_h);
    glDrawArrays(GL_TRIANGLE_FAN, 0, 6);

    checkGlError("glDrawArrays");
    }
    return 0;
}
GLuint loadShader(GLenum shaderType, const char* pSource) {
    GLuint shader = glCreateShader(shaderType);
    if (shader) {
        glShaderSource(shader, 1, &pSource, NULL);
        glCompileShader(shader);
        GLint compiled = 0;
        glGetShaderiv(shader, GL_COMPILE_STATUS, &compiled);
        if (!compiled) {
            GLint infoLen = 0;
            glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &infoLen);
            if (infoLen) {
                char* buf = (char*) malloc(infoLen);
                if (buf) {
                    glGetShaderInfoLog(shader, infoLen, NULL, buf);
                    LOGE("Could not compile shader %d:\n%s\n",
                            shaderType, buf);
                    free(buf);
                }
                glDeleteShader(shader);
                shader = 0;
            }
        }
    }
    return shader;
}
GLuint createProgram(const char* pVertexSource,
                                       const char* pFragmentSource) {
    GLuint vertexShader = loadShader(GL_VERTEX_SHADER, pVertexSource);
    if (!vertexShader) {
        return 0;
    }

    GLuint pixelShader = loadShader(GL_FRAGMENT_SHADER, pFragmentSource);
    if (!pixelShader) {
        return 0;
    }

    GLuint program = glCreateProgram();
    if (program) {
        glAttachShader(program, vertexShader);
        checkGlError("glAttachShader");
        glAttachShader(program, pixelShader);
        checkGlError("glAttachShader");
        glLinkProgram(program);
        GLint linkStatus = GL_FALSE;
        glGetProgramiv(program, GL_LINK_STATUS, &linkStatus);
        if (linkStatus != GL_TRUE) {
            GLint bufLength = 0;
            glGetProgramiv(program, GL_INFO_LOG_LENGTH, &bufLength);
            if (bufLength) {
                char* buf = (char*) malloc(bufLength);
                if (buf) {
                    glGetProgramInfoLog(program, bufLength, NULL, buf);
                    LOGE("Could not link program");
                    free(buf);
                }
            }
            glDeleteProgram(program);
            program = 0;
        }
    }
    return program;
}





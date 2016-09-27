package idv.judger.glcamera;

public class GlCameraPreviewRendererJniWrapper {
	static {
        System.loadLibrary("GlCameraPreviewRenderer");
    }
 
    public static native int on_surface_created();
 
    public static native void on_surface_changed(int width, int height);
 
    public static native void on_draw_frame();
}

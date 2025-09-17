package top.eiyooooo.easycontrol.server.wrappers;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.os.Build;
import android.os.IBinder;
import android.view.Surface;

import top.eiyooooo.easycontrol.server.utils.L;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@SuppressLint("PrivateApi")
public final class SurfaceControl {
    private static Class<?> CLASS;
    private static Class<?> displayControlClass = null;

    private static Method getBuiltInDisplayMethod = null;
    private static Method setDisplayPowerModeMethod = null;
    private static Method getPhysicalDisplayTokenMethod = null;
    private static Method getPhysicalDisplayIdsMethod = null;

    public static void init() throws ReflectiveOperationException {
        CLASS = Class.forName("android.view.SurfaceControl");
    }

    // 安卓14之后部分函数转移到了DisplayControl
    @SuppressLint({"PrivateApi", "SoonBlockedPrivateApi", "BlockedPrivateApi"})
    private static void loadDisplayControlClass() {
        try {
            Method createClassLoaderMethod = Class.forName("com.android.internal.os.ClassLoaderFactory").getDeclaredMethod("createClassLoader", String.class, String.class, String.class, ClassLoader.class, int.class, boolean.class, String.class);
            ClassLoader classLoader = (ClassLoader) createClassLoaderMethod.invoke(null, "/system/framework/services.jar", null, null, ClassLoader.getSystemClassLoader(), 0, true, null);
            displayControlClass = classLoader.loadClass("com.android.server.display.DisplayControl");
            Method loadMethod = Runtime.class.getDeclaredMethod("loadLibrary0", Class.class, String.class);
            loadMethod.setAccessible(true);
            if ((Build.BRAND.toLowerCase() + Build.MANUFACTURER.toLowerCase()).contains("honor")) throw new Exception("Honor device");
            loadMethod.invoke(Runtime.getRuntime(), displayControlClass, "android_servers");
        } catch (Throwable ignored) {
            L.d("Failed to load DisplayControl class");
        }
    }

    public static void openTransaction() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CLASS.getMethod("openTransaction").invoke(null);
    }

    public static void closeTransaction() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CLASS.getMethod("closeTransaction").invoke(null);
    }

    public static void setDisplayProjection(IBinder displayToken, int orientation, Rect layerStackRect, Rect displayRect) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CLASS.getMethod("setDisplayProjection", IBinder.class, int.class, Rect.class, Rect.class).invoke(null, displayToken, orientation, layerStackRect, displayRect);
    }

    public static void setDisplayLayerStack(IBinder displayToken, int layerStack) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CLASS.getMethod("setDisplayLayerStack", IBinder.class, int.class).invoke(null, displayToken, layerStack);
    }

    public static void setDisplaySurface(IBinder displayToken, Surface surface) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CLASS.getMethod("setDisplaySurface", IBinder.class, Surface.class).invoke(null, displayToken, surface);
    }

    public static IBinder createDisplay(String name, boolean secure) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return (IBinder) CLASS.getMethod("createDisplay", String.class, boolean.class).invoke(null, name, secure);
    }

    public static void destroyDisplay(IBinder displayToken) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CLASS.getMethod("destroyDisplay", IBinder.class).invoke(null, displayToken);
    }

    public static IBinder getBuiltInDisplay() throws Exception {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (getBuiltInDisplayMethod == null) {
                getBuiltInDisplayMethod = CLASS.getMethod("getBuiltInDisplay", int.class);
            }
            return (IBinder) getBuiltInDisplayMethod.invoke(null, 0);
        } else {
            if (getBuiltInDisplayMethod == null) {
                getBuiltInDisplayMethod = CLASS.getMethod("getInternalDisplayToken");
            }
            return (IBinder) getBuiltInDisplayMethod.invoke(null);
        }
    }

    public static IBinder getPhysicalDisplayToken(long physicalDisplayId) throws Exception {
        if (getPhysicalDisplayTokenMethod == null) {
            try {
                getPhysicalDisplayTokenMethod = CLASS.getMethod("getPhysicalDisplayToken", long.class);
            } catch (Exception ignored) {
                L.d("Try to get method from DisplayControl");
                if (displayControlClass == null) loadDisplayControlClass();
                getPhysicalDisplayTokenMethod = displayControlClass.getMethod("getPhysicalDisplayToken", long.class);
            }
        }
        return (IBinder) getPhysicalDisplayTokenMethod.invoke(null, physicalDisplayId);
    }

    public static long[] getPhysicalDisplayIds() throws Exception {
        if (getPhysicalDisplayIdsMethod == null) {
            try {
                getPhysicalDisplayIdsMethod = CLASS.getMethod("getPhysicalDisplayIds");
            } catch (Exception ignored) {
                L.d("Try to get method from DisplayControl");
                if (displayControlClass == null) loadDisplayControlClass();
                getPhysicalDisplayIdsMethod = displayControlClass.getMethod("getPhysicalDisplayIds");
            }
        }
        return (long[]) getPhysicalDisplayIdsMethod.invoke(null);
    }

    public static void setDisplayPowerMode(IBinder displayToken, int mode) throws Exception {
        if (setDisplayPowerModeMethod == null) {
            setDisplayPowerModeMethod = CLASS.getMethod("setDisplayPowerMode", IBinder.class, int.class);
        }
        setDisplayPowerModeMethod.invoke(null, displayToken, mode);
    }
}
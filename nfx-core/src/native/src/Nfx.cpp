//
// Created by XDSSWAR on 4/13/2024.
//

#include "jni_h/xss_it_nfx_NfxUtil.h"
#include "utils/utils.h"

/**
 * Retrieves the native handle of the specified JavaFX stage window.
 *
 * @param env The JNI environment.
 * @param cls The Java class reference.
 * @param win The JavaFX stage window object.
 * @return The native handle of the window.
 */
extern "C"
JNIEXPORT jlong JNICALL Java_xss_it_nfx_NfxUtil_getNativeHandle(JNIEnv *env, jclass cls, jobject win){
    return get_hwnd_from_javafx_stage(env, win);
}

/**
 * Sets the color of the title bar of a native window.
 *
 * @param env   The JNI environment.
 * @param cls   The Java class reference.
 * @param hWnd  The native handle of the window.
 * @param red   The red component of the color
 * @param green The green component of the color
 * @param blue  The blue component of the color
 */
extern "C"
JNIEXPORT void JNICALL Java_xss_it_nfx_NfxUtil_setTitleBarColor
(JNIEnv *env, jclass cls, jlong hWnd, jdouble red, jdouble green, jdouble blue){
    HWND _hwnd = to_hwnd(hWnd);
    set_title_bar_color(_hwnd, from_javafx_color(red, green, blue));
}

/**
 * Sets the color of the title in a native window.
 *
 * @param env   The JNI environment.
 * @param cls   The Java class reference.
 * @param hWnd  The native handle of the window.
 * @param red   The red component of the color
 * @param green The green component of the color
 * @param blue  The blue component of the color
 */
extern "C"
JNIEXPORT void JNICALL Java_xss_it_nfx_NfxUtil_setTextColor
(JNIEnv *env, jclass cls, jlong hWnd, jdouble red, jdouble green, jdouble blue){
    HWND _hwnd = to_hwnd(hWnd);
    set_text_color(_hwnd, from_javafx_color(red, green, blue));
}


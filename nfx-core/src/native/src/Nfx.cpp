//
// Created by XDSSWAR on 4/13/2024.
//

#include <dwmapi.h>
#include <iostream>

#include "jni_h/xss_it_nfx_NfxUtil.h"
#include "utils/utils.h"
#include <windowsx.h>

/**
 * Retrieves the native handle of the specified JavaFX stage window.
 *
 * @param env The JNI environment.
 * @param cls The Java class reference.
 * @param win The JavaFX stage window object.
 * @return The native handle of the window.
 */
extern "C"
JNIEXPORT jlong JNICALL Java_xss_it_nfx_NfxUtil_getNativeHandle(JNIEnv *env, jclass cls, jobject win) {
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
(JNIEnv *env, jclass cls, jlong hWnd, jdouble red, jdouble green, jdouble blue) {
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
(JNIEnv *env, jclass cls, jlong hWnd, jdouble red, jdouble green, jdouble blue) {
    HWND _hwnd = to_hwnd(hWnd);
    set_text_color(_hwnd, from_javafx_color(red, green, blue));
}

/**
 * Sets the corner preference of the window.
 *
 * @param hwnd The handle of the window
 * @param pref The corner preference to set
 * @return True if successful, false otherwise
 */
extern "C"
JNIEXPORT jboolean JNICALL
Java_xss_it_nfx_NfxUtil_setCornerPreference
(JNIEnv *env, jobject obj, jlong hwnd, jint pref) {
    if (hwnd == 0)
        return FALSE;


    auto attr = (DWM_WINDOW_CORNER_PREFERENCE) pref;
    return ::DwmSetWindowAttribute(
               to_hwnd(hwnd),
               DWMWA_WINDOW_CORNER_PREFERENCE,
               &attr, sizeof(attr)) == S_OK;
}

/**
 * Sets the border color of the window.
 *
 * @param hWnd The handle of the window
 * @param r    The red component of the color
 * @param g    The green component of the color
 * @param b    The blue component of the color
 * @return True if successful, false otherwise
 */
extern "C"
JNIEXPORT jboolean JNICALL Java_xss_it_nfx_NfxUtil_setBorderColor
(JNIEnv *env, jobject obj, jlong hWnd, jint r, jint g, jint b) {
    if (hWnd == 0)
        return FALSE;

    COLORREF attr;
    if (r == -1)
        attr = DWMWA_COLOR_DEFAULT;
    else if (r == -2)
        attr = DWMWA_COLOR_NONE;
    else
        attr = RGB(r, g, b);
    return ::DwmSetWindowAttribute(
               to_hwnd(hWnd),
               DWMWA_BORDER_COLOR,
               &attr,
               sizeof(attr)) == S_OK;
}


/**
 * Native method implementation for focusing a window with the specified name.
 *
 * @param env The JNI environment pointer.
 * @param cls The Java class calling the native method.
 * @param str The name of the window to focus as a JNI string.
 */
extern "C"
JNIEXPORT void JNICALL Java_xss_it_nfx_NfxUtil_focusWindow
(JNIEnv *env, jclass cls, jstring str) {
    const char *nativeWindowName = env->GetStringUTFChars(str, nullptr);
    HWND hwnd = FindWindow(nullptr, nativeWindowName);
    if (hwnd != nullptr) {
        ShowWindow(hwnd, SW_RESTORE);
        SetForegroundWindow(hwnd);
    }
    env->ReleaseStringUTFChars(str, nativeWindowName);
}

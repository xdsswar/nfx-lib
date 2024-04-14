//
// Created by XDSSWAR on 4/13/2024.
//
#include "utils.h"
#include <windows.h>
#include <dwmapi.h>
#pragma comment(lib, "dwmapi.lib")

/**
 * Cast jlong to HWND
 * @return HWND
 */
HWND to_hwnd(const jlong hwnd) {
    return reinterpret_cast<HWND>(hwnd);
}

/**
 * Obtain the raw handle from a javafx Stage
 * @return
 */
jlong get_hwnd_from_javafx_stage(JNIEnv *env, jobject window) {
    if (window == nullptr) {
        return 0L;
    }
    jclass stageClass = env->FindClass("javafx/stage/Window");
    jmethodID getPeerMethod = env->GetMethodID(stageClass, "getPeer", "()Lcom/sun/javafx/tk/TKStage;");
    jclass tkStageClass = env->FindClass("com/sun/javafx/tk/TKStage");
    jmethodID getRawHandleMethod = env->GetMethodID(tkStageClass, "getRawHandle", "()J");
    jobject tkStageObject = env->CallObjectMethod(window, getPeerMethod);
    jlong hwnd = env->CallLongMethod(tkStageObject, getRawHandleMethod);
    return hwnd;
}


/**
 * Convert javafx color to win COLORREF
 * @return COLORREF
 */
COLORREF from_javafx_color(const jdouble red, const jdouble green, const jdouble blue) {
    const int r = static_cast<int>(red * 255);
    const int g = static_cast<int>(green * 255);
    const int b = static_cast<int>(blue * 255);
    return RGB(r, g, b);
}

/**
 * Sets the color of the title bar of a window specified by its handle.
 *
 * @param hWnd        The handle of the window whose title bar color is to be set.
 * @param titleBarColor The color reference representing the active color of the title bar.
 */
void set_title_bar_color(HWND hWnd, COLORREF titleBarColor) {
    DwmSetWindowAttribute(hWnd, DWMWA_CAPTION_COLOR, &titleBarColor, sizeof(titleBarColor));
}

/**
 * Sets the color of the title bar of a window specified by its handle.
 *
 * @param hWnd        The handle of the window whose title bar color is to be set.
 * @param textColor The color reference representing the active color of the title bar.
 */
void set_text_color(HWND hWnd, COLORREF textColor) {
    DwmSetWindowAttribute(hWnd, DWMWA_TEXT_COLOR, &textColor, sizeof(textColor));
}

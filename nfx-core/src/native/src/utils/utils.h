//
// Created by XDSSWAR on 4/13/2024.
//

#ifndef UTILS_H
#define UTILS_H

#pragma once
#include <windows.h>
#include <jni.h>

/**
 * Cast jlong to HWND
 * @return HWND
 */
HWND to_hwnd(jlong);

/**
 * Obtain the raw handle from a javafx Stage
 * @return
 */
jlong get_hwnd_from_javafx_stage(JNIEnv*, jobject);

/**
 * Convert javafx color to win COLORREF
 * @return COLORREF
 */
COLORREF from_javafx_color(jdouble, jdouble, jdouble);

/**
 * Sets the color of the title bar of a window specified by its handle.
 */
void set_title_bar_color(HWND , COLORREF);

/**
 * Sets the text color of a window specified by its handle.
 */
void set_text_color(HWND , COLORREF);



#endif //UTILS_H

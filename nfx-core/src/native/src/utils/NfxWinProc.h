//
// Created by XDSSWAR on 4/17/2024.
//
#ifndef NFXWINPROC_H
#define NFXWINPROC_H

#pragma once

#include <windows.h>
#include "HwndMap.h"
#include <jni.h>

#define JNI_ON_NC_HIT_TEST_MDI              "jniHitTest"
#define JNI_IS_FULL_SCREEN_MDI              "jniIsFullScreen"
#define JNI_IS_MAXIMIZED_MDI                "jniIsMaximized"
#define JNI_FIRE_STATE_CHANGE_MDI           "jniFireStateChanged"
#define JNI_INVALIDATE_MDI                  "jniInvalidateSpots"

#ifndef DWMWA_COLOR_DEFAULT
#define DWMWA_WINDOW_CORNER_PREFERENCE		33
#define DWMWA_BORDER_COLOR					34

typedef enum {
    DWMWCP_DEFAULT = 0,
    DWMWCP_DONOTROUND = 1,
    DWMWCP_ROUND = 2,
    DWMWCP_ROUNDSMALL = 3
} DWM_WINDOW_CORNER_PREFERENCE;


#define DWMWA_COLOR_DEFAULT 0xFFFFFFFF

#define DWMWA_COLOR_NONE    0xFFFFFFFE
#endif


class NfxWinProc {
public:
    /**
     * Installs the window using native code.
     *
     * @param env  The JNI environment
     * @param obj  The Java object instance
     * @param hwnd The handle of the window to install
     * @return The handle of the installed window
     */
    static HWND install(JNIEnv *env, jobject obj, HWND hwnd);


    /**
     * Uninstalls the window using native code.
     *
     * @param env  The JNI environment
     * @param obj  The Java object instance
     * @param hwnd The handle of the window to install
     * @return The handle of the installed window
     */
    static void uninstall(JNIEnv *env, jobject obj, HWND hwnd);

    /**
     * Updates the window state using native code.
     *
     * @param hwnd The handle of the window to update
     * @param max  True if the window is maximized, false otherwise
     * @param full True if the window is in full-screen mode, false otherwise
     */
    static void update(HWND hwnd, bool max, bool full);

    /**
     * Sets the background color of the window using native code.
     *
     * @param hwnd The handle of the window
     * @param r    The red component of the color
     * @param g    The green component of the color
     * @param b    The blue component of the color
     */
    static void setWindowBackground(HWND hwnd, int r, int g, int b);

private:
    /**
     * Represents the initialization status.
     */
    static int initialized;

    /**
     * Method ID for the onNcHitTest method.
     */
    static jmethodID onNcHitTestMID;

    /**
     * Method ID for the isFullscreen method.
     */
    static jmethodID isFullscreenMID;

    /**
     * Method ID for the isMaximized method.
     */
    static jmethodID isMaximizedMID;

    /**
     * Method ID for the fireStateChange method.
     */
    static jmethodID fireStateChangeMID;

    /**
     * Method ID for the onWmMouseLeave method.
     */
    static jmethodID onWmMouseLeaveMID;


    /**
     * Represents the mapping of window handles to Java objects.
     */
    static HwndMap *hwndMap;

    /**
     * The Java Virtual Machine instance.
     */
    JavaVM *jvm;

    /**
     * The JNI environment.
     */
    JNIEnv *env;

    /**
     * The Java object associated with the window.
     */
    jobject obj;

    /**
     * The handle of the window.
     */
    HWND hwnd;

    /**
     * The default window procedure.
     */
    WNDPROC defaultWndProc;

    /**
     * The wParam value for WM_SIZE message.
     */
    int wmSizeWParam;

    /**
     * The background brush of the window.
     */
    HBRUSH background;

    /**
     * Indicates if the window is moving or sizing.
     */
    bool isMovingOrSizing;

    /**
     * Indicates if the window is moving.
     */
    bool isMoving;

    /**
     * Constructor for NfxWinProc.
     */
    NfxWinProc();

    /**
     * Initializes Java callbacks.
     *
     * @param env The JNI environment
     * @param obj The Java object associated with the window
     */
    static void initializeJavaCallBacks(JNIEnv *env, jobject obj);

    /**
     * Static callback for window procedure.
     */
    static LRESULT CALLBACK StaticWindowProc(HWND hwnd, int uMsg, WPARAM wParam, LPARAM lParam);

    /**
     * Callback for window procedure.
     */
    LRESULT CALLBACK WindowProc(HWND hWnd, int uMsg, WPARAM wParam, LPARAM lParam);

    /**
     * Handles WM_DESTROY message.
     */
    LRESULT WmDestroy(HWND hWnd, int uMsg, WPARAM wParam, LPARAM lParam);

    /**
     * Handles WM_ERASEBKGND message.
     */
    LRESULT WmEraseBackground(HWND hWnd, int uMsg, WPARAM wParam, LPARAM lParam);

    /**
     * Handles WM_NCCALCSIZE message.
     */
    LRESULT WmNcCalcSize(HWND hWnd, int uMsg, WPARAM wParam, LPARAM lParam);

    /**
     * Handles WM_NCHITTEST message.
     */
    LRESULT WmNcHitTest(HWND hWnd, int uMsg, WPARAM wParam, LPARAM lParam);

    /**
     * Converts screen coordinates to window coordinates.
     */
    static LRESULT screenToWindowCoordinates(HWND hwnd, LPARAM lParam);

    /**
     * Gets the height of the resize handle.
     */
    int getResizeHandleHeight();

    /**
     * Checks if the taskbar has auto-hide feature.
     */
    static bool hasAutoHideTaskbar(int edge, RECT rcMonitor);

    /**
     * Checks if the window is in fullscreen mode.
     */
    BOOL isFullscreen();

    /**
    * Checks if the window is maximized.
    */
    BOOL isMaximized();

    /**
     * Handles the WM_NCHITTEST message.
     */
    int onNcHitTest(int x, int y, boolean isOnResizeBorder);

    /**
     * Handles the WM_MOUSELEAVE message.
     */
    void onWmMouseLeave(HWND hWnd);

    /**
     * Fires state change event later once.
     */
    void fireStateChangedLaterOnce();

    /**
     * Gets the JNI environment.
     */
    JNIEnv *getEnv();

    /**
     * Sends a message to the client area of the window.
     */
    static void sendMessageToClientArea(HWND hwnd, int uMsg, LPARAM lParam);

    /**
     * Opens the system menu at the specified coordinates.
     */
    static void openSystemMenu(HWND hwnd, int x, int y);

    /**
     * Sets the state of a menu item in the system menu.
     */
    static void setMenuItemState(HMENU systemMenu, int item, bool enabled);


    // Helper: screen LPARAM -> client POINT (physical px)
    static POINT lparamScreenToClient(HWND hWnd, LPARAM lp) {
        POINT pt{ GET_X_LPARAM(lp), GET_Y_LPARAM(lp) }; // screen px
        ::ScreenToClient(hWnd, &pt);                    // -> client px
        return pt;
    }

    // Helper: px -> DIP using the window's current DPI (Per-Monitor-V2 safe)
    static int px_to_dip(int px, UINT dpi) {
        return ::MulDiv(px, 96, static_cast<int>(dpi));  // round properly
    }


};

#endif //NFXWINPROC_H

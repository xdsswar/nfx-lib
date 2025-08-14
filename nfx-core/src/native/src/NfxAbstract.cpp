//
// Created by XDSSWAR on 4/17/2024.
//

#include "jni_h/xss_it_nfx_AbstractNfxUndecoratedWindow.h"
#include "utils/utils.h"
#include <windowsx.h>
#include <dwmapi.h>
#include <iostream>

#include "utils/attach.h"
#include "utils/HwndMap.h"
#include "utils/NfxWinProc.h"

#pragma comment(lib, "dwmapi.lib")


/**
 * Represents the initialization status.
 */
int NfxWinProc::initialized = 0;

/**
 * Method ID for the onNcHitTest method.
 */
jmethodID NfxWinProc::onNcHitTestMID;

/**
 * Method ID for the isFullscreen method.
 */
jmethodID NfxWinProc::isFullscreenMID;

/**
 * Method ID for the isMaximized method.
 */
jmethodID NfxWinProc::isMaximizedMID;

/**
 * Method ID for the fireStateChange method.
 */
jmethodID NfxWinProc::fireStateChangeMID;

/**
 * Method ID for the onWmMouseLeave method.
 */
jmethodID NfxWinProc::onWmMouseLeaveMID;

/**
 * Represents the mapping of window handles to Java objects.
 */
HwndMap *NfxWinProc::hwndMap;

/**
 * Constructor for NfxWinProc.
 */
NfxWinProc::NfxWinProc() {
    jvm = nullptr;
    env = nullptr;
    obj = nullptr;
    hwnd = nullptr;
    defaultWndProc = nullptr;
    wmSizeWParam = -1;
    background = nullptr;
    isMovingOrSizing = false;
    isMoving = false;
}

/**
 * Initializes Java callbacks.
 *
 * @param env The JNI environment
 * @param obj The Java object associated with the window
 */
void NfxWinProc::initializeJavaCallBacks(JNIEnv *env, jobject obj) {
    if (initialized) {
        return;
    }
    initialized = -1;
    jclass cls = env->GetObjectClass(obj);
    onNcHitTestMID = env->GetMethodID(cls, JNI_ON_NC_HIT_TEST_MDI, "(IIZ)I");
    isFullscreenMID = env->GetMethodID(cls, JNI_IS_FULL_SCREEN_MDI, "()Z");
    isMaximizedMID = env->GetMethodID(cls, JNI_IS_MAXIMIZED_MDI, "()Z");
    fireStateChangeMID = env->GetMethodID(cls, JNI_FIRE_STATE_CHANGE_MDI, "()V");
    onWmMouseLeaveMID = env->GetMethodID(cls, JNI_INVALIDATE_MDI, "()V");
    if (onNcHitTestMID != nullptr && isFullscreenMID != nullptr && fireStateChangeMID != nullptr
        && onWmMouseLeaveMID != nullptr && isMaximizedMID != nullptr) {
        initialized = 1;
    }
}

/**
 * Installs the Java environment and object associated with the NfxWinProc instance.
 * This method is typically called to establish the JNI environment and associate it with the NfxWinProc instance.
 *
 * @param env   The JNI environment pointer.
 * @param obj   The Java object associated with the NfxWinProc instance.
 * @param hWnd  The handle to the window.
 * @return      The handle to the window after installation.
 */
HWND NfxWinProc::install(JNIEnv *env, jobject obj, HWND hWnd) {
    initializeJavaCallBacks(env, obj);
    if (initialized < 0)
        return nullptr;

    //create HWND map
    if (hwndMap == nullptr) {
        hwndMap = new HwndMap();
        if (hwndMap == nullptr)
            return nullptr;
    }

    if (hWnd == nullptr || hwndMap->get(hWnd) != nullptr) {
        return nullptr;
    }

    auto *wp = new NfxWinProc();
    if (!hwndMap->put(hWnd, wp)) {
        delete wp;
        return nullptr;
    }

    env->GetJavaVM(&wp->jvm);
    wp->obj = env->NewGlobalRef(obj);
    wp->hwnd = hWnd;

    // replace window procedure
    wp->defaultWndProc = reinterpret_cast<WNDPROC>(::SetWindowLongPtr(hWnd, GWLP_WNDPROC, (LONG_PTR) StaticWindowProc));

    /**
     *Forces a window update to apply changes
     */
    SetWindowPos(hWnd, nullptr, 0, 0, 0, 0,
                 SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_NOACTIVATE | SWP_FRAMECHANGED);
    return hWnd;
}

/**
 * Updates the specified window with the given parameters.
 *
 * @param hwnd The handle to the window.
 * @param max  Indicates whether the window is maximized (true) or not (false).
 * @param full Indicates whether the window is in full-screen mode (true) or not (false).
 */
void NfxWinProc::update(HWND hwnd, bool max, bool full) {
    auto *wp = (NfxWinProc *) hwndMap->get(hwnd);
    if (wp != nullptr) {
        if (max || full) {
            wp->wmSizeWParam = SIZE_MAXIMIZED;
        } else {
            wp->wmSizeWParam = -1;
        }
    }

    // this sends WM_NCCALCSIZE and removes/shows the window title bar
    ::SetWindowPos(hwnd, hwnd, 0, 0, 0, 0,
                   SWP_FRAMECHANGED | SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_NOACTIVATE);

    if (wp != nullptr)
        wp->wmSizeWParam = -1;
}

/**
 * Sets the background color of the specified window.
 *
 * @param hwnd The handle to the window.
 * @param r    The red component of the background color (0-255).
 * @param g    The green component of the background color (0-255).
 * @param b    The blue component of the background color (0-255).
 */
void NfxWinProc::setWindowBackground(HWND hwnd, int r, int g, int b) {
    auto *wp = (NfxWinProc *) hwndMap->get(hwnd);
    if (wp == nullptr)
        return;

    // delete old background brush
    if (wp->background != nullptr)
        ::DeleteObject(wp->background);

    // create new background brush
    wp->background = ::CreateSolidBrush(RGB(r, g, b));
}

/**
 * Static method that serves as the window procedure for the NfxWinProc class.
 * This method handles window messages sent to the specified window.
 *
 * @param hwnd   The handle to the window receiving the message.
 * @param uMsg   The message identifier.
 * @param wParam Additional message-specific information.
 * @param lParam Additional message-specific information.
 * @return       The result of the message processing.
 */
LRESULT CALLBACK NfxWinProc::StaticWindowProc(HWND hwnd, int uMsg, WPARAM wParam, LPARAM lParam) {
    auto *wp = (NfxWinProc *) hwndMap->get(hwnd);
    if (wp == nullptr)
        return 0;
    return wp->WindowProc(hwnd, uMsg, wParam, lParam);
}

/**
 * Static method that serves as the window procedure for the NfxWinProc class.
 * This method handles window messages sent to the specified window.
 *
 * @param hWnd   The handle to the window receiving the message.
 * @param uMsg   The message identifier.
 * @param wParam Additional message-specific information.
 * @param lParam Additional message-specific information.
 * @return       The result of the message processing.
 */
LRESULT CALLBACK NfxWinProc::WindowProc(HWND hWnd, int uMsg, WPARAM wParam, LPARAM lParam) {
    onWmMouseLeave(hWnd);
    switch (uMsg) {
        case WM_NCCALCSIZE:
            return WmNcCalcSize(hWnd, uMsg, wParam, lParam);

        case WM_NCHITTEST:
            return WmNcHitTest(hWnd, uMsg, wParam, lParam);

        case WM_NCMOUSEMOVE:
            // if mouse is moved over some non-client areas,
            // send it also to the client area to allow JavaFx to process it
            // (required for Windows 11 maximize button)
            if (wParam == HTMINBUTTON || wParam == HTMAXBUTTON || wParam == HTCLOSE ||
                wParam == HTCAPTION || wParam == HTSYSMENU) {
                sendMessageToClientArea(hWnd, WM_MOUSEMOVE, lParam);
            }
            break;

        case WM_NCLBUTTONDOWN:
        case WM_NCLBUTTONUP:
            if (wParam == HTMINBUTTON || wParam == HTMAXBUTTON || wParam == HTCLOSE) {
                int uClientMsg = (uMsg == WM_NCLBUTTONDOWN) ? WM_LBUTTONDOWN : WM_LBUTTONUP;
                sendMessageToClientArea(hWnd, uClientMsg, lParam);
                return 0;
            }
            break;

        case WM_NCRBUTTONUP:
            if (wParam == HTCAPTION || wParam == HTSYSMENU)
                openSystemMenu(hWnd, GET_X_LPARAM(lParam), GET_Y_LPARAM(lParam));
            break;

        case WM_DWMCOLORIZATIONCOLORCHANGED:
            fireStateChangedLaterOnce();
            break;

        case WM_SIZE:
            if (wmSizeWParam >= 0)
                wParam = wmSizeWParam;
            break;

        case WM_ENTERSIZEMOVE:
            isMovingOrSizing = true;
            break;

        case WM_EXITSIZEMOVE:
            isMovingOrSizing = isMoving = false;
            break;

        case WM_MOVE:
        case WM_MOVING:
            if (isMovingOrSizing)
                isMoving = true;
            break;

        case WM_ERASEBKGND:
            if (isMoving)
                return FALSE;

            return WmEraseBackground(hWnd, uMsg, wParam, lParam);

        case WM_DESTROY:
            return WmDestroy(hWnd, uMsg, wParam, lParam);

        default:
            return ::CallWindowProc(defaultWndProc, hWnd, uMsg, wParam, lParam);
    }

    return ::CallWindowProc(defaultWndProc, hWnd, uMsg, wParam, lParam);
}

/**
 * Handles the WM_DESTROY message to notify the window that it is being destroyed.
 *
 * @param hWnd   The handle to the window receiving the message.
 * @param uMsg   The message identifier (WM_DESTROY).
 * @param wParam Additional message-specific information.
 * @param lParam Additional message-specific information.
 * @return       The result of the message processing.
 */
LRESULT NfxWinProc::WmDestroy(HWND hWnd, int uMsg, WPARAM wParam, LPARAM lParam) {
    // restore original window procedure
    ::SetWindowLongPtr(hWnd, GWLP_WNDPROC, (LONG_PTR) defaultWndProc);

    WNDPROC defaultWndProc2 = defaultWndProc;

    // cleanup
    getEnv()->DeleteGlobalRef(obj);
    if (background != nullptr)
        ::DeleteObject(background);
    hwndMap->remove(hWnd);
    delete this;

    return ::CallWindowProc(defaultWndProc2, hWnd, uMsg, wParam, lParam);
}

/**
 * Handles the WM_ERASEBKGND message to erase the background of the window.
 *
 * @param hWnd   The handle to the window receiving the message.
 * @param uMsg   The message identifier (WM_ERASEBKGND).
 * @param wParam Additional message-specific information.
 * @param lParam Additional message-specific information.
 * @return       The result of the message processing.
 */
LRESULT NfxWinProc::WmEraseBackground(HWND hWnd, int uMsg, WPARAM wParam, LPARAM lParam) {
    if (background == nullptr)
        return FALSE;

    // fill background
    auto hdc = (HDC) wParam;
    RECT rect;
    ::GetClientRect(hWnd, &rect);
    ::FillRect(hdc, &rect, background);
    return TRUE;
}

/**
 * Handles the WM_NCCALCSIZE message to calculate the size and position of the client area.
 *
 * @param hWnd   The handle to the window receiving the message.
 * @param uMsg   The message identifier (WM_NCCALCSIZE).
 * @param wParam Additional message-specific information.
 * @param lParam Additional message-specific information.
 * @return       The result of the message processing.
 */
LRESULT NfxWinProc::WmNcCalcSize(HWND hWnd, int uMsg, WPARAM wParam, LPARAM lParam) {
    if (wParam != TRUE)
        return ::CallWindowProc(defaultWndProc, hWnd, uMsg, wParam, lParam);

    auto *params = reinterpret_cast<NCCALCSIZE_PARAMS *>(lParam);

    // Temporarily disable drawing to avoid flicker during resizing
    SendMessage(hWnd, WM_SETREDRAW, FALSE, 0);

    int originalTop = params->rgrc[0].top;
    LRESULT lResult = ::CallWindowProc(defaultWndProc, hWnd, uMsg, wParam, lParam);
    if (lResult != 0)
        return lResult;
    params->rgrc[0].top = originalTop;

    bool isMaximizedW = ::IsZoomed(hWnd);

    if (isMaximizedW && !isFullscreen()) {
        params->rgrc[0].top += getResizeHandleHeight();
        APPBARDATA autohide{0};
        autohide.cbSize = sizeof(autohide);
        UINT state = (UINT) ::SHAppBarMessage(ABM_GETSTATE, &autohide);
        if ((state & ABS_AUTOHIDE) != 0) {
            HMONITOR hMonitor = ::MonitorFromWindow(hWnd, MONITOR_DEFAULTTONEAREST);
            MONITORINFO monitorInfo{0};
            ::GetMonitorInfo(hMonitor, &monitorInfo);
            if (hasAutoHideTaskbar(ABE_TOP, monitorInfo.rcMonitor))
                params->rgrc[0].top++;
            if (hasAutoHideTaskbar(ABE_BOTTOM, monitorInfo.rcMonitor))
                params->rgrc[0].bottom--;
            if (hasAutoHideTaskbar(ABE_LEFT, monitorInfo.rcMonitor))
                params->rgrc[0].left++;
            if (hasAutoHideTaskbar(ABE_RIGHT, monitorInfo.rcMonitor))
                params->rgrc[0].right--;
        }
    } else if (!isMaximizedW && isFullscreen()) {
        // Set the client area to the full screen size proposed by the system
        params->rgrc[0] = params->rgrc[1];
    }

    // Re-enable drawing and force a redraw to update the window's appearance
    SendMessage(hWnd, WM_SETREDRAW, TRUE, 0);
    InvalidateRect(hWnd, nullptr, TRUE); // Invalidate the window to force a redraw
    return lResult;
}

/**
 * Handles the WM_NCHITTEST message to determine hit test results.
 *
 * @param hWnd   The handle to the window receiving the message.
 * @param uMsg   The message identifier (WM_NCHITTEST).
 * @param wParam Additional message-specific information.
 * @param lParam Additional message-specific information.
 * @return       The hit test result indicating the area of the window that the cursor is over.
 */
LRESULT NfxWinProc::WmNcHitTest(HWND hWnd, int uMsg, WPARAM wParam, LPARAM lParam) {
    // this will handle the left, right and bottom parts of the frame because we didn't change them
    LRESULT lResult = ::CallWindowProc(defaultWndProc, hWnd, uMsg, wParam, lParam);
    if (lResult != HTCLIENT)
        return lResult;

    // 1) Get client coordinates in PHYSICAL PX
    auto [x, y] = lparamScreenToClient(hWnd, lParam);

    // 2) Compute native-only facts in PX (no need to involve Java)
    const int resizeBorderHeightPx = getResizeHandleHeight(); // uses GetSystemMetricsForDpi
    const bool isOnResizeBorder = (y < resizeBorderHeightPx) &&
                                  ((::GetWindowLong(hWnd, GWL_STYLE) & WS_THICKFRAME) != 0);

    // 3) Convert PX -> DIP for Java
    const UINT dpi = ::GetDpiForWindow(hWnd);
    const int xDip = px_to_dip(x, dpi);
    const int yDip = px_to_dip(y, dpi);

    return onNcHitTest(xDip, yDip, isOnResizeBorder);
}

/**
 * Retrieves the height of the resize handle used for resizing the window.
 *
 * @return The height of the resize handle.
 */
int NfxWinProc::getResizeHandleHeight() {
    UINT dpi = ::GetDpiForWindow(hwnd);

    return ::GetSystemMetricsForDpi(SM_CXPADDEDBORDER, dpi)
           + ::GetSystemMetricsForDpi(SM_CYSIZEFRAME, dpi);
}

/**
 * Checks if the taskbar is set to auto-hide on the specified edge of the monitor.
 *
 * @param edge       The edge of the monitor where the taskbar is auto-hidden.
 * @param rcMonitor  The bounding rectangle of the monitor.
 * @return           True if the taskbar is set to auto-hide on the specified edge, false otherwise.
 */
bool NfxWinProc::hasAutoHideTaskbar(int edge, RECT rcMonitor) {
    APPBARDATA data{0};
    data.cbSize = sizeof(data);
    data.uEdge = edge;
    data.rc = rcMonitor;
    HWND hTaskbar = (HWND) ::SHAppBarMessage(ABM_GETAUTOHIDEBAREX, &data);
    return hTaskbar != nullptr;
}

/**
 * Checks whether the window is in full-screen mode.
 *
 * @return True if the window is in full-screen mode, false otherwise.
 */
BOOL NfxWinProc::isFullscreen() {
    JniAttachGuard guard(jvm);
    JNIEnv *at_env = guard.env();
    if (!at_env)
        return FALSE;

    jboolean r = at_env->CallBooleanMethod(obj, isFullscreenMID);

    if (at_env->ExceptionCheck()) {
        at_env->ExceptionClear();
        r = JNI_FALSE;
    }
    return (r == JNI_TRUE) ? TRUE : FALSE;
}

/**
 * Checks whether the window is maximized.
 *
 * @return True if the window is maximized, false otherwise.
 */
BOOL NfxWinProc::isMaximized() {
    JniAttachGuard guard(jvm);
    JNIEnv *at_env = guard.env();
    if (!at_env) return FALSE;

    jboolean r = at_env->CallBooleanMethod(obj, isMaximizedMID);
    if (at_env->ExceptionCheck()) {
        at_env->ExceptionClear();
        return FALSE;
    }
    return (r == JNI_TRUE) ? TRUE : FALSE; // convert jboolean -> BOOL
}

/**
 * Handles the WM_NCHITTEST message to determine hit test results for the specified coordinates.
 *
 * @param x                The x-coordinate of the cursor.
 * @param y                The y-coordinate of the cursor.
 * @param isOnResizeBorder Whether the cursor is on a resize border.
 * @return                 The hit test result indicating the area of the window that the cursor is over.
 */
int NfxWinProc::onNcHitTest(int x, int y, boolean isOnResizeBorder) {
    JniAttachGuard guard(jvm); // jvm is your cached JavaVM*
    JNIEnv *at_env = guard.env();
    if (!at_env) {
        // Failed to get/attach env; fall back
        return isOnResizeBorder ? HTTOP : HTCLIENT;
    }
    jint result = at_env->CallIntMethod(obj, onNcHitTestMID,
                                        (jint) x, (jint) y, (jboolean) isOnResizeBorder);
    if (at_env->ExceptionCheck()) {
        at_env->ExceptionClear();
        return isOnResizeBorder ? HTTOP : HTCLIENT;
    }
    return result;
}

/**
 * Handles the WM_MOUSELEAVE message for the specified window.
 *
 * @param hWnd The handle to the window receiving the message.
 */
void NfxWinProc::onWmMouseLeave(HWND hWnd) {
    POINT point;
    GetCursorPos(&point);
    HWND under = WindowFromPoint(point);
    if (under != hWnd) {
        JniAttachGuard guard(jvm);
        JNIEnv *at_env = guard.env();
        if (!at_env) return;

        at_env->CallVoidMethod(obj, onWmMouseLeaveMID);

        if (at_env->ExceptionCheck()) {
            at_env->ExceptionClear();
        }
    }
}

/**
 * Fires a state changed event later, ensuring that it happens only once.
 * This method schedules the state changed event to be fired later, but ensures
 * that it will be fired only once even if called multiple times.
 */
void NfxWinProc::fireStateChangedLaterOnce() {
    JniAttachGuard guard(jvm);
    JNIEnv *at_env = guard.env();
    if (!at_env) return;

    at_env->CallVoidMethod(obj, fireStateChangeMID);

    if (at_env->ExceptionCheck()) {
        at_env->ExceptionClear();
    }
}

/**
 * Retrieves the Java environment pointer associated with the current thread.
 *
 * @return The Java environment pointer (JNIEnv) associated with the current thread.
 */
JNIEnv *NfxWinProc::getEnv() {
    if (env != nullptr)
        return env;

    jvm->GetEnv((void **) &env, JNI_VERSION_1_2);
    return env;
}

/**
 * Sends a message to the client area of the specified window.
 *
 * @param hwnd   The handle to the window whose client area will receive the message.
 * @param uMsg   The message to be sent.
 * @param lParam Additional message-specific information.
 */
void NfxWinProc::sendMessageToClientArea(HWND hwnd, int uMsg, LPARAM lParam) {
    // get mouse x/y in window coordinates
    LRESULT xy = screenToWindowCoordinates(hwnd, lParam);

    // send message
    ::SendMessage(hwnd, uMsg, 0, xy);
}

/**
 * Converts screen coordinates to window coordinates relative to the given window.
 *
 * @param hwnd   The handle to the window to which the coordinates will be relative.
 * @param lParam The screen coordinates to be converted.
 * @return       The converted window coordinates as an LRESULT.
 */
LRESULT NfxWinProc::screenToWindowCoordinates(HWND hwnd, LPARAM lParam) {
    RECT rcWindow;
    ::GetWindowRect(hwnd, &rcWindow);

    int x = GET_X_LPARAM(lParam) - rcWindow.left;
    int y = GET_Y_LPARAM(lParam) - rcWindow.top;

    return MAKELONG(x, y);
}

/**
 * Opens the system menu at the specified coordinates relative to the given window.
 *
 * @param hwnd The handle to the window where the system menu should be opened.
 * @param x    The x-coordinate of the position where the system menu should be opened.
 * @param y    The y-coordinate of the position where the system menu should be opened.
 */
void NfxWinProc::openSystemMenu(HWND hwnd, int x, int y) {
    HMENU systemMenu = ::GetSystemMenu(hwnd, false);

    LONG style = ::GetWindowLong(hwnd, GWL_STYLE);
    bool isMaximized = ::IsZoomed(hwnd);
    setMenuItemState(systemMenu, SC_RESTORE, isMaximized);
    setMenuItemState(systemMenu, SC_MOVE, !isMaximized);
    setMenuItemState(systemMenu, SC_SIZE, (style & WS_THICKFRAME) != 0 && !isMaximized);
    setMenuItemState(systemMenu, SC_MINIMIZE, (style & WS_MINIMIZEBOX) != 0);
    setMenuItemState(systemMenu, SC_MAXIMIZE, (style & WS_MAXIMIZEBOX) != 0 && !isMaximized);
    setMenuItemState(systemMenu, SC_CLOSE, true);

    ::SetMenuDefaultItem(systemMenu, SC_CLOSE, 0);

    int ret = ::TrackPopupMenu(systemMenu, TPM_RETURNCMD, x, y, 0, hwnd, nullptr);
    if (ret != 0)
        ::PostMessage(hwnd, WM_SYSCOMMAND, ret, 0);
}

/**
 * Sets the state of a menu item in the system menu.
 *
 * @param systemMenu A handle to the system menu.
 * @param item       The identifier of the menu item to set the state for.
 * @param enabled    True if the menu item should be enabled, false if it should be disabled.
 */
void NfxWinProc::setMenuItemState(HMENU systemMenu, int item, bool enabled) {
    MENUITEMINFO mii{0};
    mii.cbSize = sizeof(mii);
    mii.fMask = MIIM_STATE;
    mii.fType = MFT_STRING;
    mii.fState = enabled ? MF_ENABLED : MF_DISABLED;
    ::SetMenuItemInfo(systemMenu, item, FALSE, &mii);
}

/**
 *======================================================================================================================
 *
 *                                        JNI BELOW
 *
 *======================================================================================================================
 */

/**
 * Hides or shows the window in the taskbar.
 *
 * @param hWnd   The handle of the window to hide
 * @param hidden True to hide the window from the taskbar, false to show it
 */
extern "C"
JNIEXPORT void JNICALL Java_xss_it_nfx_AbstractNfxUndecoratedWindow_hideFromTaskBar
(JNIEnv *env, jobject obj, jlong hWnd, jboolean hidden) {
    HWND hwnd = to_hwnd(hWnd);

    LONG exStyle = GetWindowLong(hwnd, GWL_EXSTYLE);
    if (exStyle == 0) {
        return;
    }

    if (hidden) {
        exStyle |= WS_EX_TOOLWINDOW;
    } else {
        exStyle &= ~WS_EX_TOOLWINDOW;
    }

    SetWindowLong(hwnd, GWL_EXSTYLE, exStyle);
}

/**
 * Installs the window.
 *
 * @param hWnd The handle of the window to install
 */
extern "C"
JNIEXPORT void JNICALL Java_xss_it_nfx_AbstractNfxUndecoratedWindow_install
(JNIEnv *env, jobject obj, jlong hWnd) {
    NfxWinProc::install(env, obj, to_hwnd(hWnd));
}

/**
 * Updates the window state.
 *
 * @param hWnd      The handle of the window to update
 * @param max       True if the window is maximized, false otherwise
 * @param full      True if the window is in full-screen mode, false otherwise
 */
extern "C"
JNIEXPORT void JNICALL Java_xss_it_nfx_AbstractNfxUndecoratedWindow_update
(JNIEnv *env, jobject obj, jlong hWnd, jboolean max, jboolean full) {
    NfxWinProc::update(to_hwnd(hWnd), max, full);
}

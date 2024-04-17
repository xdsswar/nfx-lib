/**
 * Created by XDSSWAR on 6/11/2023.
 */

#ifndef NFX_HWND_MAP_H
#define NFX_HWND_MAP_H
#pragma once

#include <windows.h>

struct Entry
{
    HWND key;
    LPVOID value;
};

class HwndMap
{
public:
    HwndMap();
    LPVOID get(HWND key);
    bool put(HWND key, LPVOID value);
    void remove(HWND key);

private:
    int size;
    int capacity;
    Entry* table;
    CRITICAL_SECTION criticalSection{};

    int binarySearch(HWND key);
    void ensureCapacity();
};

#endif

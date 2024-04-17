/**
 * Created by XDSSWAR on 6/11/2023.
 */
#pragma once
#include "HwndMap.h"

#define DEFAULT_CAPACITY		20
#define INCREASE_CAPACITY		10


class LOCK
{
    LPCRITICAL_SECTION lpCriticalSection;

public:
    explicit LOCK(LPCRITICAL_SECTION lpCriticalSection) {
        this->lpCriticalSection = lpCriticalSection;
        ::EnterCriticalSection(lpCriticalSection);
    }
    ~LOCK() {
        ::LeaveCriticalSection(lpCriticalSection);
    }

};


HwndMap::HwndMap() {
    size = 0;
    capacity = 0;
    table = nullptr;

    ::InitializeCriticalSection(&criticalSection);
}

LPVOID HwndMap::get(HWND key) {
    LOCK lock(&criticalSection);

    int index = binarySearch(key);
    return (index >= 0) ? table[index].value : nullptr;
}


bool HwndMap::put(HWND key, LPVOID value) {
    LOCK lock(&criticalSection);

    int index = binarySearch(key);
    if (index >= 0) {
        // key already in map --> replace
        table[index].value = value;
    }
    else {
        ensureCapacity();
        // make room for new entry
        index = -(index + 1);
        for (int i = size - 1; i >= index; i--)
            table[i + 1] = table[i];
        size++;

        // insert entry
        table[index].key = key;
        table[index].value = value;
    }
    return true;
}

void HwndMap::remove(HWND key) {
    LOCK lock(&criticalSection);

    // search for key
    int index = binarySearch(key);
    if (index < 0)
        return;

    // remove entry
    for (int i = index + 1; i < size; i++)
        table[i - 1] = table[i];
    size--;
}

int HwndMap::binarySearch(HWND key) {
    if (table == nullptr)
        return -1;

    auto ikey = reinterpret_cast<__int64>(key);
    int low = 0;
    int high = size - 1;

    while (low <= high) {
        int mid = (low + high) >> 1;

        auto midKey = reinterpret_cast<__int64>(table[mid].key);
        if (midKey < ikey)
            low = mid + 1;
        else if (midKey > ikey)
            high = mid - 1;
        else
            return mid;
    }

    return -(low + 1);
}

void HwndMap::ensureCapacity() {
    if (table == nullptr) {
        table = new Entry[DEFAULT_CAPACITY];
        capacity = DEFAULT_CAPACITY;
        return;
    }

    // check capacity
    int minCapacity = size + 1;
    if (minCapacity <= capacity) {
        return;
    }

    // allocate new table
    int newCapacity = minCapacity + INCREASE_CAPACITY;
    auto* newTable = new Entry[newCapacity];

    // copy old table to new table
    for (int i = 0; i < capacity; i++) {
        newTable[i] = table[i];
    }
    // delete old table
    delete[] table;
    table = newTable;
    capacity = newCapacity;;
}

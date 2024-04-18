/**
 * Created by XDSSWAR on 6/11/2023.
 */
#pragma once
#include "HwndMap.h"

#define DEFAULT_CAPACITY		20
#define INCREASE_CAPACITY		10


class LOCK {
    /**
     * A pointer to a CRITICAL_SECTION object.
     * This pointer is used to reference a CRITICAL_SECTION object,
     * which is a synchronization primitive used for thread safety.
     */
    LPCRITICAL_SECTION lpCriticalSection;

public:
    /**
     * Constructs a new LOCK object and enters the critical section.
     *
     * @param lpCriticalSection A pointer to the CRITICAL_SECTION object to enter.
     */
    explicit LOCK(LPCRITICAL_SECTION lpCriticalSection) {
        this->lpCriticalSection = lpCriticalSection;
        ::EnterCriticalSection(lpCriticalSection);
    }


    /**
     * Destroys the LOCK object and leaves the critical section.
     */
    ~LOCK() {
        ::LeaveCriticalSection(lpCriticalSection);
    }
};


HwndMap::HwndMap() {
    /** Initialize the size member to 0 */
    size = 0;

    /** Initialize the capacity member to 0 */
    capacity = 0;

    /** Initialize the table member to nullptr */
    table = nullptr;

    /** Initialize the critical section for thread safety */
    ::InitializeCriticalSection(&criticalSection);
}

/**
 * Retrieves the value associated with the specified key in the map.
 *
 * @param key The HWND key to search for.
 * @return    The value associated with the key if found, or nullptr if the key is not found.
 */
LPVOID HwndMap::get(HWND key) {
    LOCK lock(&criticalSection);

    int index = binarySearch(key);
    return (index >= 0) ? table[index].value : nullptr;
}

/**
 * Associates the specified value with the specified key in the map.
 *
 * @param key    The HWND key to associate the value with.
 * @param value  The value to be associated with the key.
 * @return       true if the value was successfully associated with the key, false otherwise.
 */
bool HwndMap::put(HWND key, LPVOID value) {
    LOCK lock(&criticalSection);

    int index = binarySearch(key);
    if (index >= 0) {
        // key already in map --> replace
        table[index].value = value;
    } else {
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

/**
 * Removes the mapping for the specified key from the map if present.
 *
 * @param key The HWND key whose mapping is to be removed from the map.
 */
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

/**
 * Performs a binary search to find the index of the specified key in the map.
 *
 * @param key The HWND key to search for.
 * @return    The index of the key if found, or the index where the key should be inserted if not found (negative value).
 */
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

/**
 * Ensures that the capacity of the map is sufficient to accommodate additional entries.
 * If necessary, the capacity of the map is increased.
 */
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
    auto *newTable = new Entry[newCapacity];

    // copy old table to new table
    for (int i = 0; i < capacity; i++) {
        newTable[i] = table[i];
    }
    // delete old table
    delete[] table;
    table = newTable;
    capacity = newCapacity;;
}

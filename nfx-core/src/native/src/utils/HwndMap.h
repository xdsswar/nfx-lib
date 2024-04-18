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
    /**
     * Constructs a new HwndMap object.
     * This constructor initializes an empty map.
     */
    HwndMap();

    /**
     * Retrieves the value associated with the specified key in the map.
     *
     * @param key The HWND key.
     * @return    The value associated with the key, or NULL if the key is not found.
     */
    LPVOID get(HWND key);

    /**
     * Associates the specified value with the specified key in the map.
     *
     * @param key    The HWND key.
     * @param value  The value to be associated with the key.
     * @return       true if the value was successfully associated with the key, false otherwise.
     */
    bool put(HWND key, LPVOID value);

    /**
     * Removes the mapping for the specified key from the map if present.
     *
     * @param key The HWND key whose mapping is to be removed from the map.
     */
    void remove(HWND key);


private:
 /**
  * The size of the map (number of entries).
  */
 int size;

 /**
  * The capacity of the map (maximum number of entries before resizing).
  */
 int capacity;

 /**
  * An array of Entry objects representing the hash table.
  */
 Entry* table;

 /**
  * A CRITICAL_SECTION object used for thread safety.
  */
 CRITICAL_SECTION criticalSection{};

 /**
  * Performs a binary search to find the index of the specified key in the map.
  *
  * @param key The HWND key to search for.
  * @return    The index of the key if found, or the index where the key should be inserted if not found.
  */
 int binarySearch(HWND key);

 /**
  * Ensures that the map has enough capacity to accommodate new entries.
  * This method is called internally to resize the map if necessary.
  */
 void ensureCapacity();

};

#endif

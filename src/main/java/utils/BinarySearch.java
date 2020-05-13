package utils;

public class BinarySearch {
    /**
     * Binary search in sorted array(Long array)
     *
     * @param arr  - array to process
     * @param low  - 0 if we want to search in the full array
     * @param high - size of array - 1, if we want to search in the full array
     * @param key  - key to find in array
     * @return index where key is located, -1 otherwise
     */
    public static int binarySearchL(long[] arr, int low, int high, long key) {
        if (high < low)
            return -1;

        int mid = (low + high) / 2;
        if (key == arr[mid])
            return mid;
        if (key > arr[mid])
            return binarySearchL(arr, (mid + 1), high, key);
        return binarySearchL(arr, low, (mid - 1), key);
    }

    /**
     * Binary search in reverse sorted (Float array)
     *
     * @param arr  - array to process
     * @param low  - 0 if we want to search in the full array
     * @param high - size of array - 1, if we want to search in the full array
     * @param key  - key to find in array
     * @return index where key is located, -1 otherwise
     */
    public static int binarySearchF(float[] arr, int low, int high, float key) {
        if (high < low)
            return -1;

        int mid = (low + high) / 2;
        if (key == arr[mid])
            return mid;
        if (key < arr[mid])
            return binarySearchF(arr, (mid + 1), high, key);
        return binarySearchF(arr, low, (mid - 1), key);
    }

    /**
     * Returns the index on where and element should be inserted to maintain sorted array
     * @param arr - array to process
     * @param length - size of array
     * @param key - key to insert in array
     * @return
     */
    public static int getIndexInSortedArray(long arr[], int length, long key) {
        int i = length;
        while(i >= 0 && arr[i] > key){i--;};
        return (i + 1);
    }
}

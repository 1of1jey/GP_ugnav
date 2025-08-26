package algorithm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
public final class MergeSort {
    public static <T> void sort(List<T> list, Comparator<T> comparator) {
        if (list.size() < 2) return;
        List<T> aux = new ArrayList<>(list);
        mergeSort(list, aux, 0, list.size() - 1, comparator);
    }

    private static <T> void mergeSort(List<T> a, List<T> aux, int lo, int hi, Comparator<T> cmp) {
        if (lo >= hi) return;
        int mid = (lo + hi) >>> 1;
        mergeSort(a, aux, lo, mid, cmp);
        mergeSort(a, aux, mid + 1, hi, cmp);
        merge(a, aux, lo, mid, hi, cmp);
    }

    private static <T> void merge(List<T> a, List<T> aux, int lo, int mid, int hi, Comparator<T> cmp) {
        for (int i = lo; i <= hi; i++) aux.set(i, a.get(i));
        int i = lo, j = mid + 1, k = lo;
        while (i <= mid && j <= hi) {
            if (cmp.compare(aux.get(i), aux.get(j)) <= 0) {
                a.set(k++, aux.get(i++));
            } else {
                a.set(k++, aux.get(j++));
            }
        }
        while (i <= mid) a.set(k++, aux.get(i++));
        while (j <= hi) a.set(k++, aux.get(j++));
    }
}



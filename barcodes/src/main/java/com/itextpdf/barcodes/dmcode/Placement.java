package com.itextpdf.barcodes.dmcode;

import java.util.Arrays;
import java.util.Hashtable;

public class Placement {
    private int nrow;
    private int ncol;
    private short[] array;
    private static final Hashtable<Integer, short[]> cache = new Hashtable<Integer, short[]>();

    private Placement() {
    }

    static public short[] doPlacement(int nrow, int ncol) {
        Integer key = Integer.valueOf(nrow * 1000 + ncol);
        short[] pc = cache.get(key);
        if (pc != null)
            return pc;
        Placement p = new Placement();
        p.nrow = nrow;
        p.ncol = ncol;
        p.array = new short[nrow * ncol];
        p.ecc200();
        cache.put(key, p.array);
        return p.array;
    }

    /* "module" places "chr+bit" with appropriate wrapping within array[] */
    private void module(int row, int col, int chr, int bit) {
        if (row < 0) {
            row += nrow;
            col += 4 - (nrow + 4) % 8;
        }
        if (col < 0) {
            col += ncol;
            row += 4 - (ncol + 4) % 8;
        }
        array[row * ncol + col] = (short) (8 * chr + bit);
    }

    /* "utah" places the 8 bits of a utah-shaped symbol character in ECC200 */
    private void utah(int row, int col, int chr) {
        module(row - 2, col - 2, chr, 0);
        module(row - 2, col - 1, chr, 1);
        module(row - 1, col - 2, chr, 2);
        module(row - 1, col - 1, chr, 3);
        module(row - 1, col, chr, 4);
        module(row, col - 2, chr, 5);
        module(row, col - 1, chr, 6);
        module(row, col, chr, 7);
    }

    /* "cornerN" places 8 bits of the four special corner cases in ECC200 */
    private void corner1(int chr) {
        module(nrow - 1, 0, chr, 0);
        module(nrow - 1, 1, chr, 1);
        module(nrow - 1, 2, chr, 2);
        module(0, ncol - 2, chr, 3);
        module(0, ncol - 1, chr, 4);
        module(1, ncol - 1, chr, 5);
        module(2, ncol - 1, chr, 6);
        module(3, ncol - 1, chr, 7);
    }

    private void corner2(int chr) {
        module(nrow - 3, 0, chr, 0);
        module(nrow - 2, 0, chr, 1);
        module(nrow - 1, 0, chr, 2);
        module(0, ncol - 4, chr, 3);
        module(0, ncol - 3, chr, 4);
        module(0, ncol - 2, chr, 5);
        module(0, ncol - 1, chr, 6);
        module(1, ncol - 1, chr, 7);
    }

    private void corner3(int chr) {
        module(nrow - 3, 0, chr, 0);
        module(nrow - 2, 0, chr, 1);
        module(nrow - 1, 0, chr, 2);
        module(0, ncol - 2, chr, 3);
        module(0, ncol - 1, chr, 4);
        module(1, ncol - 1, chr, 5);
        module(2, ncol - 1, chr, 6);
        module(3, ncol - 1, chr, 7);
    }

    private void corner4(int chr) {
        module(nrow - 1, 0, chr, 0);
        module(nrow - 1, ncol - 1, chr, 1);
        module(0, ncol - 3, chr, 2);
        module(0, ncol - 2, chr, 3);
        module(0, ncol - 1, chr, 4);
        module(1, ncol - 3, chr, 5);
        module(1, ncol - 2, chr, 6);
        module(1, ncol - 1, chr, 7);
    }

    /* "ECC200" fills an nrow x ncol array with appropriate values for ECC200 */
    private void ecc200() {
        int row, col, chr;
            /* First, fill the array[] with invalid entries */
        Arrays.fill(array, (short) 0);
            /* Starting in the correct location for character #1, bit 8,... */
        chr = 1;
        row = 4;
        col = 0;
        do {
                /* repeatedly first check for one of the special corner cases, then... */
            if (row == nrow && col == 0) corner1(chr++);
            if (row == nrow - 2 && col == 0 && ncol % 4 != 0) corner2(chr++);
            if (row == nrow - 2 && col == 0 && ncol % 8 == 4) corner3(chr++);
            if (row == nrow + 4 && col == 2 && ncol % 8 == 0) corner4(chr++);
                /* sweep upward diagonally, inserting successive characters,... */
            do {
                if (row < nrow && col >= 0 && array[row * ncol + col] == 0)
                    utah(row, col, chr++);
                row -= 2;
                col += 2;
            } while (row >= 0 && col < ncol);
            row += 1;
            col += 3;
                /* & then sweep downward diagonally, inserting successive characters,... */

            do {
                if (row >= 0 && col < ncol && array[row * ncol + col] == 0)
                    utah(row, col, chr++);
                row += 2;
                col -= 2;
            } while (row < nrow && col >= 0);
            row += 3;
            col += 1;
                /* ... until the entire array is scanned */
        } while (row < nrow || col < ncol);
            /* Lastly, if the lower righthand corner is untouched, fill in fixed pattern */
        if (array[nrow * ncol - 1] == 0) {
            array[nrow * ncol - 1] = array[nrow * ncol - ncol - 2] = 1;
        }
    }
}

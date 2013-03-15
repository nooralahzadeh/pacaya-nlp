package edu.jhu.hltcoe.util.vector;

import java.util.Arrays;
import java.util.Iterator;

import edu.jhu.hltcoe.util.Sort;
import edu.jhu.hltcoe.util.Utilities;

public class SortedIntLongMap implements Iterable<IntLongEntry> {

	protected int[] indices;
	protected long[] values;
	protected int used; // TODO: size
	
	public SortedIntLongMap() {
		this.used = 0;
		this.indices= new int[0];
		this.values = new long[0];	
	}

	public SortedIntLongMap(int[] index, long[] data) {
		if (!Sort.isSortedAscAndUnique(index)) {
			throw new IllegalStateException("Indices are not sorted ascending");
		}
		if (!Sort.isSortedAsc(data)) {
			throw new IllegalStateException("Values are not sorted ascending");
		}
		
		this.used = index.length;
		this.indices = index;
		this.values = data;
	}

	public SortedIntLongMap(SortedIntLongMap other) {
		this.used = other.used;
		this.indices = Utilities.copyOf(other.indices);
		this.values = Utilities.copyOf(other.values);
	}

	public void clear() {
		this.used = 0;
	}
	
	public boolean contains(int idx) {
		return Arrays.binarySearch(indices, 0, used, idx) >= 0;
	}
	
	public long get(int idx) {
		int i = Arrays.binarySearch(indices, 0, used, idx);
		if (i < 0) {
			throw new IllegalArgumentException("This map does not contain the key: " + idx);
		}
		return values[i];
	}
	
	public long getWithDefault(int idx, long defaultVal) {
		int i = Arrays.binarySearch(indices, 0, used, idx);
		if (i < 0) {
			return defaultVal;
		}
		return values[i];
	}
	
	public void remove(int idx) {
		int i = Arrays.binarySearch(indices, 0, used, idx);
		if (i < 0) {
			throw new IllegalArgumentException("This map does not contain the key: " + idx);
		}		
		// Shift the values over.
		System.arraycopy(indices, i+1, indices, i, used - i - 1);
		System.arraycopy(values, i+1, values, i, used - i - 1);
		used--;
	}
	
	public void put(int idx, long val) {
		int i = Arrays.binarySearch(indices, 0, used, idx);
		if (i >= 0) {
			// Just update the value.
			values[i] = val;
			return;
		} 
		int insertionPoint = -(i + 1);
		indices = insert(indices, insertionPoint, idx);
		values = insert(values, insertionPoint, val);
		used++;
	}
	
	private final int[] insert(int[] array, int i, int val) {
		if (used >= array.length) {
			// Increase the capacity of the array.
			array = cern.colt.Arrays.ensureCapacity(array, used+1);
		}
		if (i < used) {
			// Shift the values over.
			System.arraycopy(array, i, array, i+1, used - i);
		}
		// Insert the new index into the array.
		array[i] = val;
		return array;
	}
	
	private final long[] insert(long[] array, int i, long val) {
		if (used >= array.length) {
			// Increase the capacity of the array.
			array = cern.colt.Arrays.ensureCapacity(array, used+1);
		}
		if (i < used) {
			// Shift the values over.
			System.arraycopy(array, i, array, i+1, used - i);
		}
		// Insert the new index into the array.
		array[i] = val;
		return array;
	}
	
	public class IntLongEntryImpl implements IntLongEntry {
		private int i;
		public IntLongEntryImpl(int i) {
			this.i = i;
		}
		public int index() {
			return indices[i];
		}
		public long get() {
			return values[i];
		}
	}

	public class IntLongIterator implements Iterator<IntLongEntry> {

		private int i = 0;
		
		@Override
		public boolean hasNext() {
			return i < used;
		}

		@Override
		public IntLongEntry next() {
			return new IntLongEntryImpl(i);
		}

		@Override
		public void remove() {
			throw new RuntimeException("operation not supported");
		}
		
	}

	@Override
	public Iterator<IntLongEntry> iterator() {
		return new IntLongIterator();
	}


	public int size() {
		return used;
	}

	public int getUsed() {
		return used;
	}	

    /**
     * Returns the indices.
     */
    public int[] getIndices() {
        if (used == indices.length)
            return indices;

        int[] tmpIndices = new int[used];
        for (int i = 0; i < used; i++) {
        	tmpIndices[i] = indices[i];
        }
        return tmpIndices;
    }
    
    /**
     * Returns the values.
     */
    public long[] getValues() {
        if (used == values.length)
            return values;

        long[] tmpValues = new long[used];
        for (int i = 0; i < used; i++) {
        	tmpValues[i] = values[i];
        }
        return tmpValues;
    }
		
}
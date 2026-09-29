package io.github.rohrl.interstellar.scene;

import java.util.TreeMap;

/** Contiguous GPU row allocation; adjacent freed ranges coalesce. */
public final class RowArena {
    private final TreeMap<Integer,Integer> free=new TreeMap<>();
    private final int first;
    private int end;
    public RowArena(int first,int rows) {if(first<0 || rows<1 || (long)first+rows>Integer.MAX_VALUE)throw new IllegalArgumentException();this.first=first;end=first+rows;free.put(first,rows);}
    public int allocate(int rows) {
        if(rows<1)throw new IllegalArgumentException();
        for(var entry:free.entrySet())if(entry.getValue()>=rows) {
            int start=entry.getKey(),size=entry.getValue();free.remove(start);
            if(size>rows)free.put(start+rows,size-rows);return start;
        }
        throw new IllegalStateException("Native mesh GPU arena full: requested="+rows+", free="+freeRows()+", largest="+largestFree()+", capacity="+(end-first));
    }
    public int freeRows(){return free.values().stream().mapToInt(Integer::intValue).sum();}
    public int largestFree(){return free.values().stream().mapToInt(Integer::intValue).max().orElse(0);}
    public int capacity(){return end-first;}
    /** Extend storage without moving any live allocation. */
    public void grow(int rows) {
        if(rows<capacity() || (long)first+rows>Integer.MAX_VALUE)throw new IllegalArgumentException();
        int oldEnd=end;end=first+rows;if(end>oldEnd)release(oldEnd,end-oldEnd);
    }
    /** Keep an edited chunk at the same address when its adjacent space is free. */
    public boolean extend(int start,int oldRows,int rows) {
        if(oldRows<1 || rows<oldRows || start<first || (long)start+oldRows>end)throw new IllegalArgumentException();
        int extra=rows-oldRows;if(extra==0)return true;
        int at=start+oldRows,available=free.getOrDefault(at,0);if(available<extra)return false;
        free.remove(at);if(available>extra)free.put(at+extra,available-extra);return true;
    }
    public void release(int start,int rows) {
        if(rows<1 || start<first || (long)start+rows>end)throw new IllegalArgumentException();
        var before=free.floorEntry(start);
        if(before!=null && before.getKey()+before.getValue()>start)throw new IllegalArgumentException("Overlapping release");
        var after=free.ceilingEntry(start);
        if(after!=null && start+rows>after.getKey())throw new IllegalArgumentException("Overlapping release");
        if(before!=null && before.getKey()+before.getValue()==start) {free.remove(before.getKey());rows+=before.getValue();start=before.getKey();}
        after=free.ceilingEntry(start);
        if(after!=null && start+rows==after.getKey()) {rows+=after.getValue();free.remove(after.getKey());}
        free.put(start,rows);
    }
}

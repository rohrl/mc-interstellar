package io.github.rohrl.interstellar.scene;

import java.util.TreeMap;

/** Contiguous GPU row allocation; adjacent freed ranges coalesce. */
public final class RowArena {
    private final TreeMap<Integer,Integer> free=new TreeMap<>();
    private final int first,end;
    public RowArena(int first,int rows) {if(first<0 || rows<1 || (long)first+rows>Integer.MAX_VALUE)throw new IllegalArgumentException();this.first=first;end=first+rows;free.put(first,rows);}
    public int allocate(int rows) {
        if(rows<1)throw new IllegalArgumentException();
        for(var entry:free.entrySet())if(entry.getValue()>=rows) {
            int start=entry.getKey(),size=entry.getValue();free.remove(start);
            if(size>rows)free.put(start+rows,size-rows);return start;
        }
        throw new IllegalStateException("Native mesh GPU arena full; lower render distance");
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

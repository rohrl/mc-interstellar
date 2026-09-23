package io.github.rohrl.interstellar.source;

import java.util.*;
import java.util.function.Function;
import static io.github.rohrl.interstellar.source.ClusterProbe.*;

/** Shared, loaded-world component index. Every world read is charged to advance's budget. */
public final class ClusterTracker {
    public static final class Entry {
        public final long id;
        public final Result result;
        public final List<Cell> members;
        private final Set<Long> chunks;
        private boolean dirty;
        private long dirtySince;
        private Entry(long id,Result result,List<Cell> members,Set<Long> chunks) {
            this.id=id;this.result=result;this.members=members;this.chunks=chunks;
        }
        public boolean ready() {return !dirty&&result.status()==Status.COMPLETE;}
        public boolean dirty() {return dirty;}
        public long dirtySince() {return dirtySince;}
    }
    private final Function<Cell,CellState> view;
    private final double radiusPerBlock;
    private final Map<Cell,Entry> owners=new HashMap<>();
    private final Map<Long,Entry> entries=new LinkedHashMap<>();
    private final Map<Long,Set<Entry>> dependencies=new HashMap<>();
    private final LinkedHashMap<Cell,Long> pending=new LinkedHashMap<>();
    private ClusterProbe active;
    private Cell seed;
    private final Set<Long> activeChunks=new HashSet<>();
    private long nextId=1,tick;
    private long revision;
    private boolean invalid;
    public ClusterTracker(Function<Cell,CellState> view,double radiusPerBlock) {
        this.view=view;this.radiusPerBlock=radiusPerBlock;
    }
    public Collection<Entry> entries() {return Collections.unmodifiableCollection(entries.values());}
    public Entry at(Cell cell) {return owners.get(cell);}
    public Entry byId(long id) {return entries.get(id);}
    public long revision() {return revision;}
    public boolean pending() {return active!=null||!pending.isEmpty();}
    public static long chunk(int x,int z) {return ((long)x<<32)^(z&0xffffffffL);}
    /** A chunk scan discovered a block. Existing complete components need no new probe. */
    public void discover(Cell cell) {
        if(!owners.containsKey(cell))pending.putIfAbsent(cell,tick);
    }
    /** Block mutation; inspect the old owner and all six adjoining components. */
    public void changed(Cell cell,long now) {
        tick=now;
        touch(cell);
        touch(new Cell(cell.x()+1,cell.y(),cell.z()));touch(new Cell(cell.x()-1,cell.y(),cell.z()));
        touch(new Cell(cell.x(),cell.y()+1,cell.z()));touch(new Cell(cell.x(),cell.y()-1,cell.z()));
        touch(new Cell(cell.x(),cell.y(),cell.z()+1));touch(new Cell(cell.x(),cell.y(),cell.z()-1));
        pending.put(cell,now+3);
    }
    private void touch(Cell cell) {
        if(active!=null&&active.touches(cell))invalid=true;
        Entry entry=owners.get(cell);
        if(entry!=null)dirty(entry);
    }
    private void dirty(Entry entry) {
        if(entry.dirty)return;
        revision++;
        entry.dirty=true;entry.dirtySince=tick;
        for(Cell cell:entry.members)pending.putIfAbsent(cell,tick+3);
    }
    public void chunkChanged(int x,int z,long now) {
        tick=now;long key=chunk(x,z);
        if(activeChunks.contains(key))invalid=true;
        var affected=dependencies.get(key);
        if(affected!=null)for(Entry entry:affected)dirty(entry);
    }
    /** Cell reads and queue examination both have bounded work, even after a large split/removal. */
    public int advance(long now,int budget) {
        tick=now;int reads=0,examined=0;
        while(reads<budget&&examined++<budget) {
            if(active!=null&&invalid) {
                pending.putIfAbsent(seed,now+3);active=null;activeChunks.clear();invalid=false;
            }
            if(active==null) {
                if(pending.isEmpty())break;
                var iterator=pending.entrySet().iterator();var item=iterator.next();
                Cell candidate=item.getKey();long due=item.getValue();iterator.remove();
                if(due>now) {pending.put(candidate,due);continue;}
                Entry owner=owners.get(candidate);
                if(owner!=null&&!owner.dirty)continue;
                seed=candidate;activeChunks.clear();
                active=new ClusterProbe(seed,cell->{
                    activeChunks.add(chunk(cell.x()>>4,cell.z()>>4));return view.apply(cell);
                },4096,radiusPerBlock);
            }
            reads+=active.advance(Math.min(64,budget-reads));
            if(active.finished()) {publish();active=null;activeChunks.clear();}
        }
        return reads;
    }
    private void retire(Entry entry) {
        entries.remove(entry.id);
        for(Cell cell:entry.members)owners.remove(cell,entry);
        for(long chunk:entry.chunks) {
            var set=dependencies.get(chunk);
            if(set!=null) {set.remove(entry);if(set.isEmpty())dependencies.remove(chunk);}
        }
    }
    private void publish() {
        revision++;
        var members=active.members();var old=new HashSet<Entry>();
        Entry seedOwner=owners.get(seed);if(seedOwner!=null)old.add(seedOwner);
        for(Cell cell:members) {Entry owner=owners.get(cell);if(owner!=null)old.add(owner);}
        long id=old.stream().mapToLong(entry->entry.id).min().orElse(nextId++);
        for(Entry entry:old) {
            // New discoveries can merge existing components without a block callback (chunk load).
            for(Cell cell:entry.members)pending.putIfAbsent(cell,tick);
            retire(entry);
        }
        if(members.isEmpty())return;
        var entry=new Entry(id,active.result(),members,Set.copyOf(activeChunks));entries.put(id,entry);
        for(Cell cell:members) {owners.put(cell,entry);pending.remove(cell);}
        for(long chunk:entry.chunks)dependencies.computeIfAbsent(chunk,key->new HashSet<>()).add(entry);
    }
}

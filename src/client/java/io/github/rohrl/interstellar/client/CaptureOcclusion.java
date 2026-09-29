package io.github.rohrl.interstellar.client;

import io.github.rohrl.interstellar.mixin.client.WeightedModelAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.EmptyBlockView;
import net.minecraft.world.chunk.ChunkSection;
import java.util.IdentityHashMap;

/** Capture-local proof that a block emits no native geometry. No surface simplification. */
final class CaptureOcclusion {
    private final ClientWorld world;
    private final IdentityHashMap<ChunkSection,byte[]> sections=new IdentityHashMap<>();
    private final IdentityHashMap<BlockState,Byte> states=new IdentityHashMap<>();
    private final IdentityHashMap<BakedModel,Boolean> models=new IdentityHashMap<>();
    private final BlockPos.Mutable neighbour=new BlockPos.Mutable();
    private final Random random=Random.create(0);
    CaptureOcclusion(ClientWorld world){this.world=world;}
    private byte classify(BlockState state) {
        var cached=states.get(state);if(cached!=null)return cached;
        byte flags=0;
        // Position-dependent culling shapes cannot be classified once per state.
        if(!state.getBlock().hasDynamicBounds() && state.isOpaqueFullCube(EmptyBlockView.INSTANCE,BlockPos.ORIGIN)) {
            flags=1;
            if(state.getFluidState().isEmpty() && state.getRenderType()==net.minecraft.block.BlockRenderType.MODEL
                    && directionalOnly(MinecraftClient.getInstance().getBlockRenderManager().getModel(state)))flags=3;
        }
        states.put(state,flags);return flags;
    }
    private boolean directionalOnly(BakedModel model) {
        var cached=models.get(model);if(cached!=null)return cached;
        models.put(model,false); // Conservative for unsupported or cyclic custom models.
        boolean result=false;
        if(model.getClass()==BasicBakedModel.class)result=model.getQuads(null,null,random).isEmpty();
        else if(model.getClass()==WeightedBakedModel.class)
            result=((WeightedModelAccessor)model).interstellar$models().stream().allMatch(entry->directionalOnly(entry.data()));
        models.put(model,result);return result;
    }
    private byte[] mask(ChunkSection section) {
        var found=sections.get(section);if(found!=null)return found;
        var result=new byte[4096];
        for(int i=0;i<4096;i++)result[i]=classify(section.getBlockState(i&15,i>>8,(i>>4)&15));
        sections.put(section,result);return result;
    }
    boolean enclosed(ChunkSection section,int i,BlockPos pos) {
        var cells=mask(section);if((cells[i]&2)==0)return false;
        int x=i&15,z=(i>>4)&15,y=i>>8;
        return (x>0?(cells[i-1]&1)!=0:opaque(pos,-1,0,0)) && (x<15?(cells[i+1]&1)!=0:opaque(pos,1,0,0))
            && (z>0?(cells[i-16]&1)!=0:opaque(pos,0,0,-1)) && (z<15?(cells[i+16]&1)!=0:opaque(pos,0,0,1))
            && (y>0?(cells[i-256]&1)!=0:opaque(pos,0,-1,0)) && (y<15?(cells[i+256]&1)!=0:opaque(pos,0,1,0));
    }
    private boolean opaque(BlockPos pos,int x,int y,int z) {
        neighbour.set(pos.getX()+x,pos.getY()+y,pos.getZ()+z);return (classify(world.getBlockState(neighbour))&1)!=0;
    }
}

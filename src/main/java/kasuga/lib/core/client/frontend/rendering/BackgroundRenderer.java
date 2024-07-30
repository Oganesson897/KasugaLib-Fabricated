package kasuga.lib.core.client.frontend.rendering;

import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import kasuga.lib.core.client.render.texture.*;
import kasuga.lib.core.util.LazyRecomputable;
import net.minecraft.resources.ResourceLocation;

public class BackgroundRenderer {

    public static enum RenderMode {
        COMMON,
        NINE_SLICED
    }

    public ResourceLocation location;

    public int color = 0xffffff;
    public float opacity = 1.0f;
    public float borderSize = 0;
    public float borderScale = 0;
    private ImageProvider image;
    int left = 0;
    int top = 0;
    int width = 0;
    int height = 0;
    public LazyRecomputable<SimpleTexture> simple = LazyRecomputable.of(()->{
        if(this.image == null)
            return null;
        SimpleTexture texture = this.image.getSimpleTexture();
        if(texture == null)
            return null;
        return texture.cutSize(left,top,width == 0 ? texture.width() : width,width == 0 ? texture.height() : height).withColor(color,opacity);
    });

    public LazyRecomputable<WorldTexture> world = LazyRecomputable.of(()->{
        if(this.image == null)
            return null;
        WorldTexture texture = this.image.getWorldTexture();
        if(texture == null)
            return null;
        return texture.cutSize(left,top,width == 0 ? texture.width() : width,width == 0 ? texture.height() : height).withColor(color,opacity);
    });

    public LazyRecomputable<ImageMask> mask = LazyRecomputable.of(() -> {
        if (this.image == null) return null;
        StaticImage sim = this.image.getImage();
        if (sim == null) return null;
        ImageMask mask = sim.getMask();
        return mask.rectangle(new Vector3f(left, top, 0), ImageMask.Axis.X, ImageMask.Axis.Y,
                true, true, width == 0 ? sim.width() : width, height == 0 ? sim.height() : height);
    });

    public LazyRecomputable<NineSlicedImageMask> nineSlicedMask = LazyRecomputable.of(() ->{
        if (this.image == null) return null;
        StaticImage sim = this.image.getImage();
        if (sim == null) return null;
        NineSlicedImageMask mask = sim.getNineSlicedMask();
        mask.rectangle(new Vector3f(left, top, 0), ImageMask.Axis.X, ImageMask.Axis.Y,
                true, true, width == 0 ? sim.width() : width, height == 0 ? sim.height() : height);
        mask.setBorders(borderSize, borderSize, borderSize, borderSize);
        mask.setScalingFactor(borderScale);
        return mask;
    });

    RenderMode mode = RenderMode.COMMON;

    public void setRenderMode(RenderMode mode) {
        this.mode = mode;
    }

    public void render(RenderContext context,int x,int y,int width,int height){
        if( mode == RenderMode.COMMON ) {
            // this.renderCommon(context, x, y, width, height);
            neoRenderCommon(context);
        } else {
            // this.renderNineSliced(context, x, y, width, height);
            neoRenderNineSliced(context);
        }
    }

    public void setNineSlicedParam(float borderSize,float borderScale){
        this.borderSize = borderSize;
        this.borderScale = borderScale;
    }

    private void renderNineSliced(RenderContext context, int x, int y, int width, int height) {
        if(context.getContextType() == RenderContext.RenderContextType.SCREEN){
            if(this.simple.get() == null) return;
            this.simple.get().renderNineSliceScaled(borderSize,x,y,width,height,borderScale);
        } else {
            if (this.world.get() == null) return;
            context.pose().pushPose();
            context.pose().mulPose(Quaternion.fromXYZ(0f,3.14159267f,0f));
            context.pose().translate(x,-y,0f);
            this.world.get().renderType(context.getRenderType());
            this.world.get().render(context.pose(),context.getBufferSource(),width,height,context.getLight(),true);
            context.pose().popPose();
        }
    }

    private void neoRenderNineSliced(RenderContext context) {
        if (mask == null) return;
        ImageMask imageMask = nineSlicedMask.get();
        if (context.getContextType() == RenderContext.RenderContextType.SCREEN)
            imageMask.renderToGui();
        imageMask.renderToWorld(context.pose(), context.getBufferSource(),
                context.getRenderType().build(imageMask.image.id), true, context.packedLight);
    }

    public void renderCommon(RenderContext context,int x,int y,int width,int height){
        if(context.getContextType() == RenderContext.RenderContextType.SCREEN){
            if(this.simple.get() == null)
                return;
            this.simple.get().render(x,y,width,height);
        }else{
            if(this.world.get() == null)
                return;
            context.pose().pushPose();
            context.pose().mulPose(Quaternion.fromXYZ(0f,3.14159267f,0f));
            context.pose().translate(x,-y,0f);
            this.world.get().renderType(context.getRenderType());
            this.world.get().render(context.pose(),context.getBufferSource(),width,height,context.getLight(),true);
            context.pose().popPose();
        }
    }

    public void neoRenderCommon(RenderContext context) {
        if (mask == null) return;
        ImageMask imageMask = mask.get();
        if (imageMask == null) return;
        if (context.getContextType() == RenderContext.RenderContextType.SCREEN)
            imageMask.renderToGui();
        imageMask.renderToWorld(context.pose(), context.getBufferSource(),
                context.getRenderType().build(imageMask.image.id), true, context.packedLight);
    }

    public void markDirty(){
        this.simple.clear();
        this.world.clear();
    }

    public void setImage(ImageProvider provider){
        this.image = provider;
        markDirty();
    }

    public void setUV(int left,int top,int width,int height){
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        markDirty();
    }
}
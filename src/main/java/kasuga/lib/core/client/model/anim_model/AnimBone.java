package kasuga.lib.core.client.model.anim_model;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import kasuga.lib.core.client.model.BedrockRenderable;
import kasuga.lib.core.client.model.Rotationable;
import kasuga.lib.core.client.model.model_json.Bone;
import kasuga.lib.core.client.model.model_json.Cube;
import kasuga.lib.core.client.model.model_json.Locator;
import kasuga.lib.core.client.render.SimpleColor;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnimBone implements BedrockRenderable, Animable {

    public final Bone bone;
    private final Vector3f pivot, rotation;
    private final Map<String, BedrockRenderable> children;
    private final List<BedrockRenderable> cubes;
    private final HashMap<String, Locator> locators;

    public final AnimModel model;
    private AnimBone parent;
    private Vector3f offset, animRot, scale;

    public AnimBone(Bone bone, AnimModel model) {
        this.bone = bone;
        this.pivot = new Vector3f(bone.pivot);
        this.rotation = new Vector3f(bone.rotation);
        pivot.mul(1 / 16f);
        children = Maps.newHashMap();
        this.model = model;
        this.cubes = Lists.newArrayList();
        this.locators = new HashMap<>();
        collectCubes();
        initAnim();
    }

    protected AnimBone(Bone bone, AnimModel model, List<BedrockRenderable> cubes, HashMap<String, Locator> locators) {
        this.bone = bone;
        this.children = new HashMap<>();
        this.model = model;
        this.cubes = new ArrayList<>(cubes.size());
        cubes.forEach(c -> {
            this.cubes.add(((AnimCube) c).copy(model, this));
        });

        this.locators = new HashMap<>();
        locators.forEach((name, locator) -> {this.locators.put(name, locator.copy());});
        this.pivot = new Vector3f(bone.pivot);
        this.rotation = new Vector3f(bone.rotation);
        initAnim();
    }

    private void collectLocators() {
        HashMap<String, Locator> locators = bone.getLocators();
        Vector3f parentPivot = parent == null ? new Vector3f() : new Vector3f(parent.getPivot());
        locators.forEach((name, loc) -> {
            this.locators.put(name, new Locator(vonvertPivot(loc.position, parentPivot), loc.rotation));
        });
    }

    private void collectCubes() {
        List<Cube> cubes = bone.getCubes();
        for (Cube cube : cubes) {
            AnimCube ac = new AnimCube(cube, this.model, this);
            this.cubes.add(ac);
        }
    }

    public void addThisToParent() {
        BedrockRenderable parent = model.getChild(this.bone.parent);
        if (!(parent instanceof AnimBone parentBone)) return;
        Map<String, BedrockRenderable> map = parentBone.getChildrens();
        map.put(this.bone.name, this);
        this.parent = (AnimBone) model.getChild(this.bone.parent);
        collectLocators();
    }


    @Override
    public Map<String, BedrockRenderable> getChildrens() {
        return children;
    }

    @Override
    public BedrockRenderable getChild(String name) {
        return children.getOrDefault(name, null);
    }

    public BedrockRenderable getChild(int index) {
        return children.getOrDefault(String.valueOf(index), null);
    }

    public Vector3f getPivot() {
        return new Vector3f(this.pivot);
    }

    public void resetAnimation() {
        this.initAnim();
    }

    private void initAnim() {
        this.offset = new Vector3f();
        this.animRot = new Vector3f();
        this.scale = new Vector3f(1, 1, 1);
    }

    public Vector3f getRealPosition() {
        Vector3f result = new Vector3f(this.pivot);
        result.add(this.offset);
        return result;
    }

    public void setOffset(Vector3f offset) {
        this.offset = offset;
    }

    public Vector3f getRealRotation() {
        Vector3f result = new Vector3f(this.rotation);
        result.add(this.animRot);
        return result;
    }

    public void setAnimRot(Vector3f animRot) {
        this.animRot = animRot;
    }

    public void setScale(Vector3f scale) {
        this.scale = scale;
    }

    @Override
    public void applyTranslationAndRotation(PoseStack pose) {
        Vector3f translation = getRealPosition();
        Vector3f parentTrans = parent != null ? new Vector3f(parent.getPivot()) : new Vector3f();
        Vector3f t = vonvertPivot(translation, parentTrans);
        pose.translate(t.x(), t.y(), t.z());

        Vector3f rotation = getRealRotation();
        if (!rotation.equals(Rotationable.ZERO)) {
            pose.mulPose(Axis.ZP.rotationDegrees(rotation.z()));
            pose.mulPose(Axis.YN.rotationDegrees(rotation.y()));
            pose.mulPose(Axis.XN.rotationDegrees(rotation.x()));
        }

        if (scale.equals(Cube.BASE_SCALE)) return;
        pose.scale(scale.x(), scale.y(), scale.z());
    }

    @Override
    public void render(PoseStack pose, VertexConsumer consumer, SimpleColor color, int light, int overlay) {
        pose.pushPose();
        applyTranslationAndRotation(pose);
        cubes.forEach(c -> c.render(pose, consumer, color, light, overlay));
        children.forEach((c, d) -> d.render(pose, consumer, color, light, overlay));
        pose.popPose();
    }

    public AnimBone copy(Bone bone, AnimModel model) {
        return new AnimBone(bone, model, this.cubes, this.locators);
    }
}

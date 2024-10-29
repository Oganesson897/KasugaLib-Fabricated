package kasuga.lib.core.client.model.model_json;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import kasuga.lib.KasugaLib;
import kasuga.lib.core.util.Resources;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.IModelBuilder;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.SimpleUnbakedGeometry;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;

@OnlyIn(Dist.CLIENT)
public class UnbakedBedrockModel extends SimpleUnbakedGeometry<UnbakedBedrockModel> {
    public final ResourceLocation modelLocation, textureLocation;
    private Material material;
    private ArrayList<Geometry> geometries;
    private final boolean flipV;
    private String formatVersion;
    private boolean legacy;


    public UnbakedBedrockModel(ResourceLocation modelLocation, ResourceLocation textureLocation, boolean flipV) {
        this.flipV = flipV;
        this.modelLocation = modelLocation;
        this.textureLocation = textureLocation;
        geometries = Lists.newArrayList();
        legacy = false;
        parse(null);
    }

    public UnbakedBedrockModel(ResourceLocation modelLocation, Material material, boolean flipV) {
        this.flipV = flipV;
        this.modelLocation = modelLocation;
        this.textureLocation = material.texture();
        this.geometries = new ArrayList<>();
        legacy = false;
        parse(material);
    }

    public void parse(@Nullable Material material) {
        JsonObject model = readModel();
        if (model == null) {
            KasugaLib.MAIN_LOGGER.warn("Unable to open animated model: " + this.modelLocation.toString());
            return;
        }
        formatVersion = model.get("format_version").getAsString();
        this.material = material == null ? new Material(TextureAtlas.LOCATION_BLOCKS, textureLocation) : material;

        JsonArray geos;
        if (model.has("minecraft:geometry")) {
            geos = model.getAsJsonArray("minecraft:geometry");
            legacy = false;
        } else {
            geos = new JsonArray();
            if (model.has("geometry.model")) geos.add(model.get("geometry.model"));
            legacy = true;
        }
        for (JsonElement element : geos) {
            JsonObject geometryJson = element.getAsJsonObject();
            Geometry geometry = new Geometry(geometryJson, this);
            geometries.add(geometry);
        }
    }

    @Override
    public Set<String> getConfigurableComponentNames() {
        Set<String> result = new HashSet<>();
        geometries.forEach(bone -> result.add(bone.getDescription().getIdentifier()));
        return result;
    }

    public JsonObject readModel() {
        JsonObject model;
        try {
            Resource resource = Resources.getResource(modelLocation);
            model = JsonParser.parseReader(resource.openAsReader()).getAsJsonObject();
        } catch (IOException e) {
            KasugaLib.MAIN_LOGGER.error("Failed to load animated model: " + this.modelLocation.toString(), e);
            return null;
        }
        return model;
    }

    public String getFormatVersion() {
        return formatVersion;
    }

    public Material getMaterial() {
        return material;
    }

    public boolean isFlipV() {
        return flipV;
    }

    public List<Geometry> getGeometries() {
        return geometries;
    }

    public boolean isLegacy() {
        return legacy;
    }

    @Override
    protected void addQuads(IGeometryBakingContext owner, IModelBuilder<?> modelBuilder, ModelBakery bakery,
                            Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelTransform, ResourceLocation modelLocation) {
        geometries.forEach(geometry -> geometry.addQuads(
                owner, modelBuilder, bakery,
                spriteGetter, modelTransform, modelLocation
        ));
    }

    @Override
    public Collection<Material> getMaterials(IGeometryBakingContext context, Function<ResourceLocation, UnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors) {
        Set<Material> materials = new HashSet<>();
        materials.add(this.material);
        return materials;
    }
}

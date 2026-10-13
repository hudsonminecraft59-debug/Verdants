package dev.verdant.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.verdant.modules.EspModule;
import dev.verdant.modules.EspSettings;
import dev.verdant.module.ModuleManager;
import dev.verdant.util.ColorUtil;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

/**
 * Draws translucent boxes and box outlines (as thin cuboids) using the 1.21.11 render pipeline API.
 * Two pipelines are used: one without depth testing (through walls) and one with.
 */
public final class EspRenderer {
    private static final RenderPipeline WALLS_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("verdant", "pipeline/esp_through_walls"))
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .build());

    private static final RenderPipeline DEPTH_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("verdant", "pipeline/esp_depth_tested"))
                    .build());

    private static final Layer WALLS = new Layer(WALLS_PIPELINE, "verdant esp walls");
    private static final Layer DEPTH = new Layer(DEPTH_PIPELINE, "verdant esp depth");

    private EspRenderer() {}

    // ------------------------------------------------------------------ frame entry point

    public static void render(WorldRenderContext context) {
        EspModule[] modules = {ModuleManager.STORAGE_ESP, ModuleManager.SPAWN_FINDER};
        boolean any = false;
        for (EspModule m : modules) {
            if (m.isEnabled() && !m.targets().isEmpty()) { any = true; break; }
        }
        if (!any) return;

        Vec3 cam = context.worldState().cameraRenderState.pos;
        PoseStack matrices = context.matrices();
        Matrix4fc pose = matrices.last().pose();
        float time = (System.currentTimeMillis() % 100_000_000L) / 1000f;

        for (EspModule m : modules) {
            if (!m.isEnabled()) continue;
            EspSettings s = m.esp;
            Layer layer = s.throughWalls.get() ? WALLS : DEPTH;
            for (EspTarget t : m.targets()) {
                emit(layer, pose, s, t, cam, time);
            }
        }

        WALLS.flush();
        DEPTH.flush();
    }

    public static void close() {
        WALLS.close();
        DEPTH.close();
    }

    // ------------------------------------------------------------------ target -> geometry

    private static void emit(Layer layer, Matrix4fc pose, EspSettings s, EspTarget t, Vec3 cam, float time) {
        // Coordinates are made camera-relative in double precision before narrowing to float.
        AABB b = t.box().inflate(s.expand.get());
        float x0 = (float) (b.minX - cam.x), y0 = (float) (b.minY - cam.y), z0 = (float) (b.minZ - cam.z);
        float x1 = (float) (b.maxX - cam.x), y1 = (float) (b.maxY - cam.y), z1 = (float) (b.maxZ - cam.z);
        if (x1 <= x0 || y1 <= y0 || z1 <= z0) return;

        double cx = (x0 + x1) * 0.5, cy = (y0 + y1) * 0.5, cz = (z0 + z1) * 0.5;
        double dist = Math.sqrt(cx * cx + cy * cy + cz * cz);
        double range = s.range.get();
        if (dist > range) return;

        float visibility = 1f;
        if (s.distanceFade.get()) {
            float f = ColorUtil.clamp01((float) ((dist - range * 0.4) / (range * 0.6)));
            visibility = 1f - f * s.fadeStrength.getF();
        }
        if (s.pulse.get()) {
            float wave = 0.5f + 0.5f * (float) Math.sin(time * s.pulseSpeed.getF() * Math.PI * 2.0);
            visibility *= 1f - s.pulseDepth.getF() * wave;
        }
        if (visibility <= 0.01f) return;

        boolean drawFill = s.fillOn() && !t.outlineOnly();
        boolean drawOutline = s.outlineOn() || t.outlineOnly();

        if (drawFill) {
            float a = s.fillOpacity.getF() * s.sourceAlpha(t, false) * visibility;
            int rgb = s.fillRgb(t, time);
            int bottom = ColorUtil.argb(rgb, a);
            int top = s.fillGradient.get() ? ColorUtil.argb(rgb, a * (1f - s.gradientStrength.getF())) : bottom;
            if ((bottom >>> 24) > 0 || (top >>> 24) > 0) {
                cuboid(layer.builder(), pose, x0, y0, z0, x1, y1, z1, top, bottom);
            }
        }

        if (drawOutline) {
            float a = s.outlineOpacity.getF() * s.sourceAlpha(t, true) * visibility;
            int color = ColorUtil.argb(s.outlineRgb(t, time), a);
            if ((color >>> 24) == 0) return;
            float width = s.outlineWidth.getF();
            float th = s.screenConstant.get() ? (float) (width * Math.max(dist, 1.0) * 0.0013) : width * 0.01f;
            th = Math.max(th, 0.002f);
            float frac = s.outlineStyle.is("Corners") && !t.outlineOnly() ? s.cornerLength.getF() : 0.5f;
            outline(layer.builder(), pose, x0, y0, z0, x1, y1, z1, th, frac, color);
        }
    }

    private static void outline(BufferBuilder b, Matrix4fc m, float x0, float y0, float z0,
                                float x1, float y1, float z1, float th, float frac, int color) {
        float[] xs = {x0, x1};
        float[] ys = {y0, y1};
        float[] zs = {z0, z1};
        for (int j = 0; j < 2; j++) {
            for (int k = 0; k < 2; k++) {
                edge(b, m, x0, ys[j], zs[k], x1, ys[j], zs[k], th, frac, color); // along X
                edge(b, m, xs[j], y0, zs[k], xs[j], y1, zs[k], th, frac, color); // along Y
                edge(b, m, xs[j], ys[k], z0, xs[j], ys[k], z1, th, frac, color); // along Z
            }
        }
    }

    /** An edge is either one full segment, or two corner segments each {@code frac} of its length. */
    private static void edge(BufferBuilder b, Matrix4fc m, float ax, float ay, float az,
                             float bx, float by, float bz, float th, float frac, int color) {
        if (frac >= 0.5f) {
            segment(b, m, ax, ay, az, bx, by, bz, th, color);
            return;
        }
        float dx = (bx - ax) * frac, dy = (by - ay) * frac, dz = (bz - az) * frac;
        segment(b, m, ax, ay, az, ax + dx, ay + dy, az + dz, th, color);
        segment(b, m, bx - dx, by - dy, bz - dz, bx, by, bz, th, color);
    }

    private static void segment(BufferBuilder b, Matrix4fc m, float ax, float ay, float az,
                                float bx, float by, float bz, float th, int color) {
        float h = th * 0.5f;
        cuboid(b, m,
                Math.min(ax, bx) - h, Math.min(ay, by) - h, Math.min(az, bz) - h,
                Math.max(ax, bx) + h, Math.max(ay, by) + h, Math.max(az, bz) + h,
                color, color);
    }

    private static void cuboid(BufferBuilder b, Matrix4fc m, float x0, float y0, float z0,
                               float x1, float y1, float z1, int top, int bot) {
        // front (+z)
        v(b, m, x0, y0, z1, bot); v(b, m, x1, y0, z1, bot); v(b, m, x1, y1, z1, top); v(b, m, x0, y1, z1, top);
        // back (-z)
        v(b, m, x1, y0, z0, bot); v(b, m, x0, y0, z0, bot); v(b, m, x0, y1, z0, top); v(b, m, x1, y1, z0, top);
        // left (-x)
        v(b, m, x0, y0, z0, bot); v(b, m, x0, y0, z1, bot); v(b, m, x0, y1, z1, top); v(b, m, x0, y1, z0, top);
        // right (+x)
        v(b, m, x1, y0, z1, bot); v(b, m, x1, y0, z0, bot); v(b, m, x1, y1, z0, top); v(b, m, x1, y1, z1, top);
        // top (+y)
        v(b, m, x0, y1, z1, top); v(b, m, x1, y1, z1, top); v(b, m, x1, y1, z0, top); v(b, m, x0, y1, z0, top);
        // bottom (-y)
        v(b, m, x0, y0, z0, bot); v(b, m, x1, y0, z0, bot); v(b, m, x1, y0, z1, bot); v(b, m, x0, y0, z1, bot);
    }

    private static void v(BufferBuilder b, Matrix4fc m, float x, float y, float z, int argb) {
        b.addVertex(m, x, y, z).setColor(argb);
    }

    // ------------------------------------------------------------------ GPU upload / draw

    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();

    /** A pipeline plus the buffers needed to build and draw one batch per frame. */
    private static final class Layer {
        private final RenderPipeline pipeline;
        private final String label;
        private final ByteBufferBuilder allocator = new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);
        private BufferBuilder buffer;
        private MappableRingBuffer vertexBuffer;

        Layer(RenderPipeline pipeline, String label) {
            this.pipeline = pipeline;
            this.label = label;
        }

        BufferBuilder builder() {
            if (buffer == null) {
                buffer = new BufferBuilder(allocator, pipeline.getVertexFormatMode(), pipeline.getVertexFormat());
            }
            return buffer;
        }

        void flush() {
            if (buffer == null) return;
            MeshData built = buffer.buildOrThrow();
            buffer = null;
            MeshData.DrawState state = built.drawState();
            VertexFormat format = state.format();
            GpuBuffer vertices = upload(state, format, built);
            draw(Minecraft.getInstance(), built, state, vertices, format);
            vertexBuffer.rotate();
        }

        private GpuBuffer upload(MeshData.DrawState state, VertexFormat format, MeshData built) {
            int size = state.vertexCount() * format.getVertexSize();
            if (vertexBuffer == null || vertexBuffer.size() < size) {
                if (vertexBuffer != null) vertexBuffer.close();
                vertexBuffer = new MappableRingBuffer(() -> label, GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE, size);
            }
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            try (GpuBuffer.MappedView view = encoder.mapBuffer(
                    vertexBuffer.currentBuffer().slice(0, built.vertexBuffer().remaining()), false, true)) {
                MemoryUtil.memCopy(built.vertexBuffer(), view.data());
            }
            return vertexBuffer.currentBuffer();
        }

        private void draw(Minecraft client, MeshData built, MeshData.DrawState state, GpuBuffer vertices, VertexFormat format) {
            GpuBuffer indices;
            VertexFormat.IndexType indexType;
            if (pipeline.getVertexFormatMode() == VertexFormat.Mode.QUADS) {
                built.sortQuads(allocator, RenderSystem.getProjectionType().vertexSorting());
                indices = pipeline.getVertexFormat().uploadImmediateIndexBuffer(built.indexBuffer());
                indexType = built.drawState().indexType();
            } else {
                RenderSystem.AutoStorageIndexBuffer shape = RenderSystem.getSequentialBuffer(pipeline.getVertexFormatMode());
                indices = shape.getBuffer(state.indexCount());
                indexType = shape.type();
            }

            GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                    .writeTransform(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);

            try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                    () -> label + " pass",
                    client.getMainRenderTarget().getColorTextureView(), OptionalInt.empty(),
                    client.getMainRenderTarget().getDepthTextureView(), OptionalDouble.empty())) {
                pass.setPipeline(pipeline);
                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms", dynamicTransforms);
                pass.setVertexBuffer(0, vertices);
                pass.setIndexBuffer(indices, indexType);
                pass.drawIndexed(0, 0, state.indexCount(), 1);
            }
            built.close();
        }

        void close() {
            allocator.close();
            if (vertexBuffer != null) {
                vertexBuffer.close();
                vertexBuffer = null;
            }
        }
    }
}

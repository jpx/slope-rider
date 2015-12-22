package com.sloperider.component;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.sloperider.ComponentFactory;
import com.sloperider.SlopeRider;

/**
 * Created by jpx on 21/12/15.
 */
public class Draggable extends Component {
    private Vector2 _anchorPosition;

    private RenderContext _context;
    private Environment _environment;
    private Renderable _renderable;
    private Shader _shader;
    private String _vertexShaderSource;
    private String _fragmentShaderSource;

    @Override
    public void requireAssets(AssetManager assetManager) {
        _vertexShaderSource = Gdx.files.internal("shader/draggable.vertex.glsl").readString();
        _fragmentShaderSource = Gdx.files.internal("shader/draggable.fragment.glsl").readString();
    }

    @Override
    public void manageAssets(AssetManager assetManager) {

    }

    @Override
    public void doReleaseAssets(AssetManager assetManager) {

    }

    @Override
    protected void doReady(ComponentFactory componentFactory) {
        setSize(2.f, 6.f);
        setOrigin(getWidth() / 2.f, getHeight() / 2.f);

        final ModelBuilder builder = new ModelBuilder();

        final float width = getWidth();
        final float height = getHeight();

        final Mesh mesh = new Mesh(true, 4, 6,
            new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position"),
            new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_uv")
        );

        mesh.setIndices(new short[]{
            0, 1, 2,
            0, 2, 3
        });

        mesh.setVertices(new float[]{
            0.f, 0.f, 0.f, 0.f,
            0.f, height, 0.f, 1.f,
            width, height, 1.f, 1.f,
            width, 0.f, 1.f, 0.f
        });

        builder.begin();
        builder.part("draggable", mesh, Gdx.gl.GL_TRIANGLES, 0, mesh.getNumIndices(), new Material());

        final Model model = builder.end();

        _environment = new Environment();
        _renderable = new Renderable();
        _renderable.worldTransform
            .idt()
            .translate(-getOriginX() * SlopeRider.PIXEL_PER_UNIT, -getOriginY() * SlopeRider.PIXEL_PER_UNIT, 0.f)
            .scale(getScaleX() * SlopeRider.PIXEL_PER_UNIT, getScaleY() * SlopeRider.PIXEL_PER_UNIT, 1.f)
            .translate(getX(), getY(), 0.f);
        _renderable.environment = _environment;
        _renderable.meshPart.set(model.meshParts.first());
        _renderable.material = model.materials.first();

        _context = new RenderContext(new DefaultTextureBinder(DefaultTextureBinder.WEIGHTED, 1));

        _shader = new Shader() {
            ShaderProgram program;
            Camera camera;
            RenderContext context;

            @Override
            public void init() {
                program = new ShaderProgram(_vertexShaderSource, _fragmentShaderSource);
                if (!program.isCompiled())
                    throw new GdxRuntimeException(program.getLog());
            }

            @Override
            public int compareTo(Shader other) {
                return 0;
            }

            @Override
            public boolean canRender(Renderable instance) {
                return true;
            }

            @Override
            public void begin(Camera camera, RenderContext context) {
                this.camera = camera;
                this.context = context;

                program.begin();

                program.setUniformMatrix("u_worldToScreenMatrix", camera.combined);

                program.setUniformf("u_size", getWidth(), getHeight());
                program.setUniformf("u_anchorPosition", 0.5f, 0.5f);
                program.setUniformf("u_limit", 0.25f);
                program.setUniformf("u_limitMask", 0.f, 1.f);
            }

            @Override
            public void render(Renderable renderable) {
                program.setUniformMatrix("u_modelToWorldMatrix", renderable.worldTransform);

                renderable.meshPart.render(program);
            }

            @Override
            public void end() {
                program.end();
            }

            @Override
            public void dispose() {
                program.dispose();
            }
        };

        _shader.init();
    }

    @Override
    protected void doAct(float delta) {

    }

    @Override
    protected void doDraw(Batch batch) {
        batch.end();

        _context.begin();

        _context.setBlending(true, Gdx.gl.GL_BLEND_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);

        _shader.begin(getStage().getCamera(), _context);

        _shader.render(_renderable);

        _shader.end();

        _context.end();

        batch.begin();
    }

    @Override
    protected void doDestroy(ComponentFactory componentFactory) {

    }

    @Override
    public void initializeBody(World world) {

    }

    @Override
    public void updateBody(World world) {

    }

    @Override
    public void destroyBody(World world) {

    }
}

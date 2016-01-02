package com.sloperider.component;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.sloperider.ComponentFactory;
import com.sloperider.SlopeRider;

/**
 * Created by jpx on 14/12/15.
 */
public class Bumper  extends Component {
    private static final float _spriteRatio = 960.f / 720.f;

    private RenderContext _context;
    private Environment _environment;
    private Renderable _renderable;
    private Shader _shader;
    private String _vertexShaderSource;
    private String _fragmentShaderSource;

    private Texture _diffuseMask;

    @Override
    public void setPosition(float x, float y) {
        super.setPosition(x, y);

        if (_renderable != null) {
            _renderable.worldTransform
                .idt()
                .scale(getScaleX() * SlopeRider.PIXEL_PER_UNIT, getScaleY() * SlopeRider.PIXEL_PER_UNIT, 1.f)
                .translate(getX(), getY(), 0.f)
                .rotate(Vector3.Z, getRotation())
                .translate(-getOriginX(), -getOriginY(), 0.f);
        }
    }

    @Override
    public void requireAssets(AssetManager assetManager) {
        assetManager.load("texture/bumper.png", Texture.class);

        _vertexShaderSource = Gdx.files.internal("shader/bumper.vertex.glsl").readString();
        _fragmentShaderSource = Gdx.files.internal("shader/bumper.fragment.glsl").readString();
    }

    @Override
    public void manageAssets(AssetManager assetManager) {
        _diffuseMask = assetManager.get("texture/bumper.png", Texture.class);
    }

    @Override
    public void doReleaseAssets(AssetManager assetManager) {
        if (assetManager.isLoaded("texture/bumpber.png", Texture.class))
            assetManager.unload("texture/bumper.png");
    }

    @Override
    protected void doReady(ComponentFactory componentFactory) {
        setSize(getWidth(), getHeight() * 1.f / _spriteRatio);
        setOrigin(getWidth() / 2.f, getHeight() * 0.92f);

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
            0.f, 0.f, 0.f, 1.f,
            0.f, height, 0.f, 0.f,
            width, height, 1.f, 0.f,
            width, 0.f, 1.f, 1.f
        });

        builder.begin();
        builder.part("bumper", mesh, Gdx.gl.GL_TRIANGLES, 0, mesh.getNumIndices(), new Material());

        final Model model = builder.end();

        _environment = new Environment();

        _renderable = new Renderable();
        _renderable.environment = _environment;
        _renderable.meshPart.set(model.meshParts.first());
        _renderable.material = model.materials.first();

        _context = new RenderContext(new DefaultTextureBinder(DefaultTextureBinder.WEIGHTED, 1));

        _shader = new Shader() {
            ShaderProgram program;
            Camera camera;
            RenderContext context;
            float time;

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
            }

            @Override
            public void render(Renderable renderable) {
                program.setUniformMatrix("u_modelToWorldMatrix", renderable.worldTransform);

                time += Gdx.graphics.getDeltaTime();
                if (time > 15.f)
                    time = 0.f;

                if (program.hasUniform("u_time"))
                    program.setUniformf("u_time", time);

                _diffuseMask.bind(0);
                program.setUniformi("u_diffuseMask", 0);

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
        _context.setBlending(true, Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);

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
    public void updateBody(World world, float deltaTime) {

    }

    @Override
    public void destroyBody(World world) {

    }
}

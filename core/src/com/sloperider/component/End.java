package com.sloperider.component;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.particles.ParticleSorter;
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Joint;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.DistanceJointDef;
import com.badlogic.gdx.physics.box2d.joints.RopeJointDef;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.sloperider.ComponentFactory;
import com.sloperider.SlopeRider;
import com.sloperider.physics.CollisionGroup;
import com.sloperider.physics.PhysicsActor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Created by jpx on 29/11/15.
 */
public class End extends Component {

    static class ContactData implements PhysicsActor.ContactData {
        End end;

        ContactData(End end) {
            this.end = end;
        }

        @Override
        public boolean contactBegin(PhysicsActor.ContactData data) {
            if (data instanceof Sleigh.ContactData) {
                Sleigh.ContactData sleighContactData = (Sleigh.ContactData) data;
                Sleigh sleigh = sleighContactData.sleigh;

                end._sleightsToAdd.add(sleigh);

                return true;
            }

            return false;
        }

        @Override
        public boolean contactEnd(PhysicsActor.ContactData data) {
            if (data instanceof Sleigh.ContactData) {
                Sleigh.ContactData sleighContactData = (Sleigh.ContactData) data;
                Sleigh sleigh = sleighContactData.sleigh;

                end._sleightsToRemove.add(sleigh);

                return true;
            }

            return false;
        }
    }

    static class SleighEntry {
        Sleigh sleigh;
    }

    private Mesh _mesh;
    private RenderContext _context;
    private Environment _environment;
    private Renderable _renderable;
    private Shader _shader;
    private String _vertexShaderSource;
    private String _fragmentShaderSource;

    private TextureRegion _textureRegion;

    private Body _body;
    private Fixture _fixture;
    private boolean _bodyNeedsUpdate;

    private final List<Sleigh> _sleightsToAdd = new ArrayList<Sleigh>();
    private final List<Sleigh> _sleightsToRemove = new ArrayList<Sleigh>();
    private final List<SleighEntry> _activeSleighs = new ArrayList<SleighEntry>();

    public final void sleighDestroyed(final Sleigh sleigh) {
        removeSleigh(sleigh);
    }

    private void addSleigh(Sleigh sleigh) {
        SleighEntry sleighEntry = null;

        for (SleighEntry activeSleighEntry : _activeSleighs) {
            if (activeSleighEntry.sleigh == sleigh) {
                sleighEntry = activeSleighEntry;

                break;
            }
        }

        if (sleighEntry == null) {
            sleighEntry = new SleighEntry();
            sleighEntry.sleigh = sleigh;

            _activeSleighs.add(sleighEntry);
        }
    }

    private void removeSleigh(final Sleigh sleigh) {
        for (int i = 0; i < _activeSleighs.size(); ++i) {
            final SleighEntry sleighEntry = _activeSleighs.get(i);

            if (sleighEntry.sleigh == sleigh) {
                _activeSleighs.remove(i);

                return;
            }
        }
    }

    @Override
    public void setPosition(float x, float y) {
        final float previousY = this.getY();

        super.setPosition(x, y);

        if (previousY != y) {
            _bodyNeedsUpdate = true;
        }

        if (_renderable != null) {
            _renderable.worldTransform
                .idt()
                .translate(-getOriginX() * SlopeRider.PIXEL_PER_UNIT, -getOriginY() * SlopeRider.PIXEL_PER_UNIT, 0.f)
                .scale(getScaleX() * SlopeRider.PIXEL_PER_UNIT, getScaleY() * SlopeRider.PIXEL_PER_UNIT, 1.f)
                .translate(getX(), getY(), 0.f);
        }
    }

    @Override
    public void requireAssets(AssetManager assetManager) {
        _vertexShaderSource = Gdx.files.internal("shader/end.vertex.glsl").readString();
        _fragmentShaderSource = Gdx.files.internal("shader/end.fragment.glsl").readString();
    }

    @Override
    public void manageAssets(AssetManager assetManager) {

    }

    @Override
    public void doReleaseAssets(AssetManager assetManager) {

    }

    @Override
    protected void doReady(ComponentFactory componentFactory) {
        _bodyNeedsUpdate = false;

        setSize(8.f, 8.f);
        setOrigin(getWidth() / 2.f, getHeight() / 2.f);

        final Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1.f, 0.f, 0.f, 0.5f);
        pixmap.fill();

        _textureRegion = new TextureRegion(new Texture(pixmap));

        final ModelBuilder builder = new ModelBuilder();

        final float width = getWidth();
        final float height = getHeight();

        final Mesh mesh = new Mesh(true, 4, 6,
                new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position"),
                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_uv"));

        mesh.setIndices(new short[] {
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
        builder.part("end", mesh, Gdx.gl.GL_TRIANGLES, 0, mesh.getNumIndices(), new Material());

        final Model model = builder.end();

        _environment = new Environment();
        _environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        _environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        _renderable = new Renderable();
        _renderable.worldTransform
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
    protected void doDestroy(ComponentFactory componentFactory) {
        while (!_activeSleighs.isEmpty()) {
            removeSleigh(_activeSleighs.get(0).sleigh);
        }
    }

    @Override
    protected void doAct(float delta) {

    }

    @Override
    protected void doDraw(Batch batch) {
        batch.end();

        _context.begin();

        _shader.begin(getStage().getCamera(), _context);

        _shader.render(_renderable);

        _shader.end();

        _context.end();

        batch.begin();

//        batch.draw(
//                _textureRegion,
//                getX() * SlopeRider.PIXEL_PER_UNIT, getY() * SlopeRider.PIXEL_PER_UNIT,
//                getOriginX(), getOriginY(),
//                getWidth(), getHeight(),
//                getScaleX() * SlopeRider.PIXEL_PER_UNIT, getScaleY() * SlopeRider.PIXEL_PER_UNIT,
//                getRotation()
//        );
    }

    @Override
    public void initializeBody(World world) {
        resetBody(world);
    }

    private void resetBody(World world) {
        if (_body != null) {
            for (final SleighEntry sleighEntry : _activeSleighs) {
                _sleightsToRemove.add(sleighEntry.sleigh);
            }

            while (!_sleightsToRemove.isEmpty()) {
                removeSleigh(_sleightsToRemove.remove(0));
            }

            _body.destroyFixture(_fixture);
            world.destroyBody(_body);

            _body = null;
            _fixture = null;
        }

        final BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        bodyDef.position.set(new Vector2(getX(), getY()));

        _body = world.createBody(bodyDef);

        final FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.isSensor = true;

        fixtureDef.filter.categoryBits = group();
        fixtureDef.filter.maskBits = collidesWith();

        final CircleShape shape = new CircleShape();
        shape.setRadius(Math.max(getWidth(), getHeight()) / 2.f);

        fixtureDef.shape = shape;

        _fixture = _body.createFixture(fixtureDef);
        _fixture.setUserData(new ContactData(this));
    }

    @Override
    public void updateBody(World world) {
        if (_bodyNeedsUpdate) {
            _bodyNeedsUpdate = false;
            Gdx.app.log(SlopeRider.TAG, "resetting body");
            resetBody(world);
        }

        while (!_sleightsToRemove.isEmpty()) {
            final Sleigh sleigh = _sleightsToRemove.remove(0);
            removeSleigh(sleigh);
        }

        while (!_sleightsToAdd.isEmpty()) {
            final Sleigh sleigh = _sleightsToAdd.remove(0);
            addSleigh(sleigh);
        }
    }

    @Override
    public void destroyBody(World world) {
        world.destroyBody(_body);
    }

    @Override
    public short group() {
        return CollisionGroup.END.value();
    }

    @Override
    public short collidesWith() {
        return CollisionGroup.SLEIGH.value();
    }

    public final boolean hasSleigh(final Sleigh sleigh) {
        for (final SleighEntry entry : _activeSleighs) {
            if (entry.sleigh == sleigh) {
                return true;
            }
        }

        return false;
    }
}

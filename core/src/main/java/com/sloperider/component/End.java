package com.sloperider.component;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Mesh;
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
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.sloperider.ComponentFactory;
import com.sloperider.SlopeRider;
import com.sloperider.physics.CollisionGroup;
import com.sloperider.physics.PhysicsActor;

import java.util.ArrayList;
import java.util.List;

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
        public boolean contactBegin(PhysicsActor.ContactData data, Contact contact) {
            if (data instanceof MainCharacter.ContactData) {
                MainCharacter.ContactData mainCharacterContactData = (MainCharacter.ContactData) data;
                MainCharacter mainCharacter = mainCharacterContactData.mainCharacter;

                end._mainCharactertsToAdd.add(mainCharacter);

                return true;
            }

            return false;
        }

        @Override
        public boolean contactEnd(PhysicsActor.ContactData data, Contact contact) {
//            if (data instanceof MainCharacter.ContactData) {
//                MainCharacter.ContactData mainCharacterContactData = (MainCharacter.ContactData) data;
//                MainCharacter mainCharacter = mainCharacterContactData.mainCharacter;
//
//                end._mainCharactertsToRemove.add(mainCharacter);
//
//                return true;
//            }

            return false;
        }
    }

    static class MainCharacterEntry {
        MainCharacter mainCharacter;
    }

    private RenderContext _context;
    private Environment _environment;
    private Renderable _renderable;
    private Shader _shader;
    private ShaderProgram _shaderProgram;
    private String _vertexShaderSource;
    private String _fragmentShaderSource;
    private float _time;

    private final Color _color0 = new Color(0.1f, 0.2f, 0.9f, 1.f);
    private final Color _color1 = new Color(0.6f, 0.3f, 0.1f, 1.f);

    private boolean _animationActive;
    private float _animationStartTime;
    private final float _animationDuration = 1.0f;

    private TextureRegion _textureRegion;

    private Body _body;
    private Fixture _fixture;
    private boolean _bodyNeedsUpdate;

    private final List<MainCharacter> _mainCharactertsToAdd = new ArrayList<MainCharacter>();
    private final List<MainCharacter> _mainCharactertsToRemove = new ArrayList<MainCharacter>();
    private final List<MainCharacterEntry> _activeMainCharacters = new ArrayList<MainCharacterEntry>();

    public End color0(final Color value) {
        _color0.set(value);

        return this;
    }

    public End color1(final Color value) {
        _color1.set(value);

        return this;
    }

    public final void mainCharacterDestroyed(final MainCharacter mainCharacter) {
        removeMainCharacter(mainCharacter);
    }

    private void addMainCharacter(MainCharacter mainCharacter) {
        MainCharacterEntry mainCharacterEntry = null;

        for (MainCharacterEntry activeMainCharacterEntry : _activeMainCharacters) {
            if (activeMainCharacterEntry.mainCharacter == mainCharacter) {
                mainCharacterEntry = activeMainCharacterEntry;

                break;
            }
        }

        if (mainCharacterEntry == null) {
            mainCharacterEntry = new MainCharacterEntry();
            mainCharacterEntry.mainCharacter = mainCharacter;

            _activeMainCharacters.add(mainCharacterEntry);
        }

        startAnimation(_time);
        mainCharacter.disablePhysics();
    }

    private void removeMainCharacter(final MainCharacter mainCharacter) {
        for (int i = 0; i < _activeMainCharacters.size(); ++i) {
            final MainCharacterEntry mainCharacterEntry = _activeMainCharacters.get(i);

            if (mainCharacterEntry.mainCharacter == mainCharacter) {
                _activeMainCharacters.remove(i);

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

        updateRenderablePosition();
    }

    private void updateRenderablePosition() {
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
        _time = 0.f;

        _bodyNeedsUpdate = false;
        _animationActive = false;

        setOrigin(getWidth() / 2.f, getHeight() / 2.f);

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

        _context = new RenderContext(new DefaultTextureBinder(DefaultTextureBinder.LRU, 1));

        _shader = new Shader() {
            ShaderProgram program;
            Camera camera;
            RenderContext context;

            @Override
            public void init() {
                program = new ShaderProgram(_vertexShaderSource, _fragmentShaderSource);
                if (!program.isCompiled())
                    throw new GdxRuntimeException(program.getLog());

                program.begin();

                program.setUniformf("u_animationMask", 0.0f);
                program.setUniformf("u_animationStartTime", 0.0f);
                program.setUniformf("u_animationDuration", 0.0f);

                _shaderProgram = program;
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

                if (program.hasUniform("u_time"))
                    program.setUniformf("u_time", _time);

                program.setUniformf("u_diffuseColor0", _color0);
                program.setUniformf("u_diffuseColor1", _color1);

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

        updateRenderablePosition();
    }

    @Override
    protected void doDestroy(ComponentFactory componentFactory) {
        while (!_activeMainCharacters.isEmpty()) {
            removeMainCharacter(_activeMainCharacters.get(0).mainCharacter);
        }
    }

    @Override
    protected void doAct(float delta) {
        _time += delta;

        if (_animationActive) {
            for (final MainCharacterEntry mainCharacterEntry : _activeMainCharacters) {
                final MainCharacter mainCharacter = mainCharacterEntry.mainCharacter;

                final Vector2 targetPosition = new Vector2(getX(), getY());
                final Vector2 position = new Vector2(mainCharacter.getX(), mainCharacter.getY());

                final Vector2 offset = targetPosition.cpy()
                    .sub(position)
                    .scl(3.f * delta);

                mainCharacter.moveBy(offset.x, offset.y);
                mainCharacter.rotateBy(360.f * delta);
                mainCharacter.scaleBy(-0.8f * delta);
            }

            if (_time - _animationStartTime >= _animationDuration) {
                animationComplete();
            }
        }
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
    public void initializeBody(World world) {
        resetBody(world);
    }

    private void resetBody(World world) {
        if (_body != null) {
            for (final MainCharacterEntry mainCharacterEntry : _activeMainCharacters) {
                _mainCharactertsToRemove.add(mainCharacterEntry.mainCharacter);
            }

            while (!_mainCharactertsToRemove.isEmpty()) {
                removeMainCharacter(_mainCharactertsToRemove.remove(0));
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
        shape.setRadius(Math.max(getWidth(), getHeight()) / 2.f * 0.9f);

        fixtureDef.shape = shape;

        _fixture = _body.createFixture(fixtureDef);
        _fixture.setUserData(new ContactData(this));
    }

    @Override
    public void updateBody(World world, float deltaTime) {
        if (_bodyNeedsUpdate) {
            _bodyNeedsUpdate = false;
            resetBody(world);
        }

        while (!_mainCharactertsToRemove.isEmpty()) {
            final MainCharacter mainCharacter = _mainCharactertsToRemove.remove(0);
            removeMainCharacter(mainCharacter);
        }

        while (!_mainCharactertsToAdd.isEmpty()) {
            final MainCharacter mainCharacter = _mainCharactertsToAdd.remove(0);
            addMainCharacter(mainCharacter);
        }
    }

    @Override
    public void destroyBody(World world) {
        if (_body == null)
            return;

        world.destroyBody(_body);
        _body = null;
    }

    @Override
    public short group() {
        return CollisionGroup.END.value();
    }

    @Override
    public short collidesWith() {
        return CollisionGroup.MAIN_CHARACTER.value();
    }

    public final boolean hasMainCharacter(final MainCharacter mainCharacter) {
        if (_animationActive)
            return false;

        for (final MainCharacterEntry entry : _activeMainCharacters) {
            if (entry.mainCharacter == mainCharacter) {
                return true;
            }
        }

        return false;
    }

    private void startAnimation(final float time) {
        if (_animationActive)
            return;

        _animationActive = true;

        _animationStartTime = time;

        _shaderProgram.begin();
        _shaderProgram.setUniformf("u_animationMask", 1.0f);
        _shaderProgram.setUniformf("u_animationStartTime", _animationStartTime);
        _shaderProgram.setUniformf("u_animationDuration", _animationDuration);
    }

    private void animationComplete() {
        _animationActive = false;

        _shaderProgram.begin();
        _shaderProgram.setUniformf("u_animationMask", 0.0f);

        setVisible(false);
    }
}

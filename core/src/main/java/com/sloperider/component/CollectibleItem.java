package com.sloperider.component;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
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
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.sloperider.ComponentFactory;
import com.sloperider.SlopeRider;
import com.sloperider.physics.CollisionGroup;
import com.sloperider.physics.PhysicsActor;

/**
 * Created by jpx on 09/01/16.
 */
public class CollectibleItem extends Component {
    interface Listener {
        void collected(final CollectibleItem self);
        void complete(final CollectibleItem self);
    }

    private Listener _listener;
    private float _duration;

    private boolean _collected;

    private String _textureFilename;
    private Texture _texture;
    private Texture _maskTexture;
    private Color _diffuseColor;

    private Body _body;
    private boolean _bodyNeedsUpdate;
    private boolean _bodyNeedsDestruction;

    private RenderContext _context;
    private Environment _environment;
    private Renderable _renderable;
    private Shader _shader;
    private ShaderProgram _shaderProgram;
    private String _vertexShaderSource;
    private String _fragmentShaderSource;
    private float _time;

    private boolean _animationActive;
    private Vector2 _basePosition;
    private Vector2 _baseScale;
    private Vector2 _animationStartPosition;
    private Vector2 _animationTargetPosition;
    private Vector2 _animationStartScale;
    private Vector2 _animationTargetScale;
    private float _animationStartZoom;
    private float _animationTargetZoom;
    private float _animationCurrentZoom;
    private float _animationStartTime;
    private float _animationDuration;

    private boolean _cooldownAnimationActive;
    private float _cooldownAnimationStartTime;
    private float _cooldownAnimationDuration;

    public final CollectibleItem listener(final Listener listener) {
        _listener = listener;

        return this;
    }

    public final CollectibleItem duration(final float value) {
        _duration = value;

        return this;
    }

    public final CollectibleItem diffuseColor(final Color value) {
        _diffuseColor = value;

        return this;
    }

    public final CollectibleItem textureFilename(final String value) {
        _textureFilename = value;

        return this;
    }

    @Override
    public void setPosition(float x, float y) {
        super.setPosition(x, y);

        updateRenderablePosition();
    }

    @Override
    public void setScale(float scaleX, float scaleY) {
        super.setScale(scaleX, scaleY);

        setOrigin(getWidth() * 0.5f * getScaleX(), getHeight() * 0.5f * getScaleY());

        updateRenderablePosition();
    }

    private void updateRenderablePosition() {
        if (_renderable != null) {
            _renderable.worldTransform
                .idt()
                .translate(-getOriginX() * SlopeRider.PIXEL_PER_UNIT, -getOriginY() * SlopeRider.PIXEL_PER_UNIT, 0.f)
                .translate(getX() * SlopeRider.PIXEL_PER_UNIT, getY() * SlopeRider.PIXEL_PER_UNIT, 0.f)
                .rotate(Vector3.Z, getRotation())
                .scale(getScaleX() * SlopeRider.PIXEL_PER_UNIT, getScaleY() * SlopeRider.PIXEL_PER_UNIT, 1.f);
        }
    }

    @Override
    public void requireAssets(AssetManager assetManager) {
        final TextureLoader.TextureParameter params = new TextureLoader.TextureParameter();
        params.minFilter = Texture.TextureFilter.Linear;
        params.magFilter = Texture.TextureFilter.Linear;
        params.wrapU = Texture.TextureWrap.ClampToEdge;
        params.wrapV = Texture.TextureWrap.ClampToEdge;

        assetManager.load(_textureFilename, Texture.class);
        assetManager.load("texture/collectible_mask.png", Texture.class);

        _vertexShaderSource = Gdx.files.internal("shader/collectible.vertex.glsl").readString();
        _fragmentShaderSource = Gdx.files.internal("shader/collectible.fragment.glsl").readString();
    }

    @Override
    public void manageAssets(AssetManager assetManager) {
        _texture = assetManager.get(_textureFilename, Texture.class);
        _maskTexture = assetManager.get("texture/collectible_mask.png", Texture.class);
    }

    @Override
    public void doReleaseAssets(AssetManager assetManager) {
        if (assetManager.isLoaded(_textureFilename))
            assetManager.unload(_textureFilename);
    }

    @Override
    protected void doReady(ComponentFactory componentFactory) {
        _time = 0.f;
        _collected = false;

        _bodyNeedsUpdate = false;
        _bodyNeedsDestruction = false;

        setTouchable(Touchable.disabled);
        setVisible(false);

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
        builder.part("collectible", mesh, Gdx.gl.GL_TRIANGLES, 0, mesh.getNumIndices(), new Material());

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

            @Override
            public void init() {
                program = new ShaderProgram(_vertexShaderSource, _fragmentShaderSource);
                if (!program.isCompiled())
                    throw new GdxRuntimeException(program.getLog());

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

                program.setUniformMatrix("u_projectionMatrix", camera.projection);

                program.setUniformMatrix("u_worldToScreenMatrix", camera.combined);

                if (program.hasUniform("u_time"))
                    program.setUniformf("u_time", _time);

                if (program.hasUniform("u_diffuseMap")) {
                    _texture.bind(0);
                    program.setUniformi("u_diffuseMap", 0);
                }

                if (program.hasUniform("u_maskMap")) {
                    _maskTexture.bind(1);
                    program.setUniformi("u_maskMap", 1);
                }

                program.setUniformf("u_diffuseColor", _diffuseColor);
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

        updateRenderablePosition();
    }

    @Override
    protected void doAct(float delta) {

    }

    @Override
    protected void doDraw(Batch batch) {
        batch.end();

        _context.begin();

        _context.setBlending(true, Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);

        final float baseZoom = getCamera().zoom;

        if (_animationActive) {
            getCamera().zoom = _animationCurrentZoom;
            getCamera().update();
        }

        _shader.begin(getStage().getCamera(), _context);

        _shader.render(_renderable);

        _shader.end();

        if (_animationActive) {
            getCamera().zoom = baseZoom;
            getCamera().update();
        }

        _context.end();

        batch.begin();
    }

    @Override
    protected void doDestroy(ComponentFactory componentFactory) {

    }

    @Override
    public void initializeBody(World world) {
    }

    private void resetBody(final World world) {
        destroyBody(world);

        final BodyDef body = new BodyDef();
        body.type = BodyDef.BodyType.StaticBody;
        body.position.set(getX(), getY());

        final FixtureDef fixture = new FixtureDef();
        fixture.isSensor = true;

        final CircleShape shape = new CircleShape();
        shape.setRadius(getWidth() * 0.4f);
        fixture.shape = shape;

        fixture.filter.categoryBits = group();
        fixture.filter.maskBits = collidesWith();

        _body = world.createBody(body);
        _body.createFixture(fixture).setUserData(new ContactData() {
            @Override
            public boolean contactBegin(ContactData data, Contact contact) {
                if (data instanceof Sleigh.ContactData) {

                    _bodyNeedsDestruction = true;

                    _collected = true;

                    return true;
                }

                return false;
            }

            @Override
            public boolean contactEnd(ContactData data, Contact contact) {
                return false;
            }
        });
    }

    @Override
    public void updateBody(World world, float deltaTime) {
        if (_bodyNeedsDestruction) {
            _bodyNeedsDestruction = false;

            destroyBody(world);
        }

        if (_bodyNeedsUpdate) {
            _bodyNeedsUpdate = false;

            resetBody(world);
        }

        _time += deltaTime;

        if (_collected) {
            _collected = false;

            final float targetSize = 1.f;
            final Vector2 padding = new Vector2(-0.5f, -0.5f).sub(targetSize * 0.5f, targetSize * 0.5f);

            startAnimation(
                new Vector2(getCamera().viewportWidth * 0.5f / SlopeRider.PIXEL_PER_UNIT, getCamera().viewportHeight * 0.5f / SlopeRider.PIXEL_PER_UNIT).add(padding),
                targetSize,
                0.5f,
                _time
            );

            startCooldownAnimation(_duration, _time);

            if (_listener != null)
                _listener.collected(CollectibleItem.this);
        }

        if (_animationActive) {
            final float elapsedTime = _time - _animationStartTime;

            if (elapsedTime > _animationDuration - 1e-2f)
                animationComplete();
            else {
                final Vector2 position = _animationStartPosition.cpy()
                    .lerp(_animationTargetPosition, elapsedTime / _animationDuration);

                final Vector2 scale = _animationStartScale.cpy()
                    .lerp(_animationTargetScale, elapsedTime / _animationDuration);

                _animationCurrentZoom = MathUtils.lerp(
                    _animationStartZoom,
                    _animationTargetZoom,
                    elapsedTime / _animationDuration
                );

                setPosition(position.x, position.y);
                setScale(scale.x, scale.y);
            }
        }

        if (_cooldownAnimationActive) {
            final float elapsedTime = _time - _cooldownAnimationStartTime;


            if (elapsedTime > _cooldownAnimationDuration - 1e-2f)
                cooldownAnimationComplete();
        }
    }

    @Override
    public void destroyBody(World world) {
        if (_body != null) {
            world.destroyBody(_body);
            _body = null;
        }
    }

    @Override
    public short collidesWith() {
        return CollisionGroup.SLEIGH.value();
    }

    @Override
    public short group() {
        return CollisionGroup.TRACK.value();
    }

    @Override
    protected void doLevelPlayed(Level level) {
        super.doLevelPlayed(level);

        setVisible(true);

        _bodyNeedsUpdate = true;
    }

    @Override
    protected void doLevelStopped(Level level) {
        super.doLevelStopped(level);

        stopAnimation();

        setVisible(false);

        _bodyNeedsDestruction = true;
    }

    @Override
    protected void doLevelComplete(Level level) {
        super.doLevelComplete(level);

        stopAnimation();

        setVisible(false);

        _bodyNeedsDestruction = true;
    }

    private void startAnimation(final Vector2 targetPosition, final float targetSize, final float duration, final float time) {
        if (_animationActive)
            return;

        _basePosition = new Vector2(getX(), getY());
        _baseScale = new Vector2(getScaleX(), getScaleY());

        _animationActive = true;
        _animationStartPosition = new Vector2(getX(), getY())
            .sub(new Vector2(getCamera().position.x, getCamera().position.y).scl(1.f / SlopeRider.PIXEL_PER_UNIT));
        _animationTargetPosition = targetPosition;
        _animationStartScale = _baseScale;
        _animationTargetScale = new Vector2(targetSize / getWidth(), targetSize / getHeight());
        _animationStartZoom = getCamera().zoom;
        _animationTargetZoom = 1.f;
        _animationDuration = duration;
        _animationStartTime = time;

        _shaderProgram.begin();
        _shaderProgram.setUniformf("u_animationMask", 1.f);
        _shaderProgram.end();
    }

    private void animationComplete() {
        if (!_animationActive)
            return;

        _animationCurrentZoom = _animationTargetZoom;
        setPosition(_animationTargetPosition.x, _animationTargetPosition.y);
        setScale(_animationTargetScale.x, _animationTargetScale.y);

    }

    private void startCooldownAnimation(final float duration, final float time) {
        if (_cooldownAnimationActive)
            return;

        _cooldownAnimationActive = true;
        _cooldownAnimationDuration = duration;
        _cooldownAnimationStartTime = time;

        _shaderProgram.begin();
        _shaderProgram.setUniformf("u_cooldownAnimationMask", 1.f);
        _shaderProgram.setUniformf("u_cooldownAnimationDuration", _cooldownAnimationDuration);
        _shaderProgram.setUniformf("u_cooldownAnimationStartTime", _cooldownAnimationStartTime);
        _shaderProgram.end();
    }

    private void cooldownAnimationComplete() {
        if (!_cooldownAnimationActive)
            return;

        _animationActive = false;
        _cooldownAnimationActive = false;

        _shaderProgram.begin();
        _shaderProgram.setUniformf("u_animationMask", 0.f);
        _shaderProgram.setUniformf("u_cooldownAnimationMask", 0.f);
        _shaderProgram.end();

        setPosition(_basePosition.x, _basePosition.y);
        setScale(_baseScale.x, _baseScale.y);

        if (_listener != null)
            _listener.complete(this);
    }

    private void stopAnimation() {
        if (_animationActive)
            animationComplete();

        if (_cooldownAnimationActive)
            cooldownAnimationComplete();
    }
}

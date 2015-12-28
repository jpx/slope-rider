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
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.sloperider.ComponentFactory;
import com.sloperider.SlopeRider;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jpx on 21/12/15.
 */
public class Draggable extends Component {
    public interface Listener {
        void dragged(final Draggable self,
                     final Vector2 move,
                     final Vector2 position,
                     final float deltaDistance);
    }

    private Component _draggedComponent;
    private final List<Listener> _listeners = new ArrayList<Listener>();

    private Vector2 _anchorPosition;
    private Vector2 _draggingMinBound;
    private Vector2 _draggingMaxBound;

    private float _limit;

    private RenderContext _context;
    private Environment _environment;
    private Renderable _renderable;
    private Shader _shader;
    private String _vertexShaderSource;
    private String _fragmentShaderSource;

    private Vector2 _graphicsSize;
    private Vector2 _graphicsOrigin;

    private Vector2 _draggingMask;

    private boolean _draggingActive;
    private Vector2 _draggingLastTouchPosition;
    private Vector2 _draggingTouchPosition;

    public final Draggable draggedComponent(final Component component) {
        _draggedComponent = component;

        return this;
    }

    public final Draggable registerListener(final Listener listener) {
        _listeners.add(listener);

        return this;
    }

    public final Draggable unregisterListener(final Listener listener) {
        _listeners.remove(listener);

        return this;
    }

    public final Draggable draggingMask(final Vector2 value) {
        _draggingMask = value.cpy().nor();

        return this;
    }

    public final Draggable draggingBounds(final Vector2 anchorPosition,
                                          final Vector2 draggingMinBound,
                                          final Vector2 draggingMaxBound) {
        _anchorPosition = anchorPosition;
        _draggingMinBound = draggingMinBound;
        _draggingMaxBound = draggingMaxBound;

        final Vector2 graphicsScale = new Vector2(2.f, 1.f);
        _graphicsSize = _draggingMaxBound.cpy().sub(_draggingMinBound).scl(graphicsScale);
        _graphicsOrigin = _draggingMinBound.cpy().scl(-1.f).scl(graphicsScale);

        return this;
    }

    public final Draggable limitChanged(final float value) {
        final float distanceFromAnchor = new Vector2(getX(), getY()).dst(_anchorPosition);

        _limit = Math.abs(value + distanceFromAnchor);

        return this;
    }

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
        setTouchable(Touchable.enabled);

        final ModelBuilder builder = new ModelBuilder();

        final float width = _graphicsSize.x;
        final float height = _graphicsSize.y;

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
            .translate(-_graphicsOrigin.x * SlopeRider.PIXEL_PER_UNIT, -_graphicsOrigin.y * SlopeRider.PIXEL_PER_UNIT, 0.f)
            .scale(getScaleX() * SlopeRider.PIXEL_PER_UNIT, getScaleY() * SlopeRider.PIXEL_PER_UNIT, 1.f)
            .translate(_anchorPosition.x, _anchorPosition.y, 0.f);
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

                program.setUniformf("u_size", _graphicsSize);
                if (program.hasUniform("u_position"))
                    program.setUniformf("u_position", new Vector2(getX(), getY()).sub(_anchorPosition).scl(1.f / _graphicsSize.x, 1.f / _graphicsSize.y));
                if (program.hasUniform("u_anchorPosition"))
                    program.setUniformf("u_anchorPosition", _draggingMinBound.cpy().scl(-1.f).scl(1.f / _graphicsSize.x, 1.f / _graphicsSize.y));
                program.setUniformf("u_limit", _limit / _graphicsSize.len());
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

        addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (!_draggingActive)
                    startDragging(new Vector2(x, y));

                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                if (_draggingActive)
                    stopDragging(new Vector2(x, y));

                super.touchUp(event, x, y, pointer, button);
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                if (_draggingActive)
                    updateDragging(new Vector2(x, y));

                super.touchDragged(event, x, y, pointer);
            }
        });
    }

    @Override
    protected void doAct(float delta) {
        if (_draggingActive) {
            final Vector2 move = _draggingTouchPosition.cpy()
                .sub(_draggingLastTouchPosition)
                .scl(1.f / SlopeRider.PIXEL_PER_UNIT)
                .scl(_draggingMask);

            _draggingLastTouchPosition = _draggingTouchPosition;

            if (move.len() < 1e-2f)
                return;

            final Vector2 position = new Vector2(getX(), getY()).add(move);

            position.x = MathUtils.clamp(
                position.x,
                _anchorPosition.x + _draggingMinBound.x,
                _anchorPosition.x + _draggingMaxBound.x
            );

            position.y = MathUtils.clamp(
                position.y,
                _anchorPosition.y + _draggingMinBound.y,
                _anchorPosition.y + _draggingMaxBound.y
            );

            position.x = MathUtils.clamp(
                position.x,
                _anchorPosition.x - _limit,
                _anchorPosition.x + _limit
            );

            position.y = MathUtils.clamp(
                position.y,
                _anchorPosition.y - _limit,
                _anchorPosition.y + _limit
            );

            final Vector2 actualMove = position.cpy().sub(getX(), getY());

            if (actualMove.len() < 1e-2f)
                return;

            final float deltaDistance = position.dst(_anchorPosition) - new Vector2(getX(), getY()).dst(_anchorPosition);

            for (final Listener listener : _listeners)
                listener.dragged(this, actualMove, position, deltaDistance);

            _draggedComponent.setPosition(position.x, position.y);
        }
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

    @Override
    public Actor doHit(float x, float y, boolean touchable) {
        final float additionalHitScale = 1.5f;

        final Vector2 minBound = new Vector2(
            -getOriginX() - getWidth() * additionalHitScale / 2.f,
            -getOriginY() - getHeight() * additionalHitScale / 2.f
        );

        final Vector2 maxBound =  new Vector2(
            -getOriginX() + getWidth() * (1.f + additionalHitScale / 2.f),
            -getOriginY() + getHeight() * (1.f + additionalHitScale / 2.f)
        );

        if (x >= minBound.x && x < maxBound.x &&
            y >= minBound.y && y < maxBound.y)
            return this;

        return null;
    }

    private void startDragging(final Vector2 position) {
        _draggingActive = true;

        _draggingTouchPosition = position;
        _draggingLastTouchPosition = _draggingTouchPosition;
    }

    private void stopDragging(final Vector2 position) {
        _draggingActive = false;
    }

    private void updateDragging(final Vector2 position) {
        _draggingTouchPosition = position;
    }

    @Override
    public float getX() {
        return _draggedComponent.getX();
    }

    @Override
    public float getY() {
        return _draggedComponent.getY();
    }

    @Override
    public float getWidth() {
        return _draggedComponent.getWidth();
    }

    @Override
    public float getHeight() {
        return _draggedComponent.getHeight();
    }

    @Override
    public float getOriginX() {
        return _draggedComponent.getOriginX();
    }

    @Override
    public float getOriginY() {
        return _draggedComponent.getOriginY();
    }

    @Override
    public float getScaleX() {
        return _draggedComponent.getScaleX();
    }

    @Override
    public float getScaleY() {
        return _draggedComponent.getScaleY();
    }

    @Override
    protected void doLevelPlayed(Level level) {
        super.doLevelPlayed(level);

        setTouchable(Touchable.disabled);
        setVisible(false);
    }

    @Override
    protected void doLevelStopped(Level level) {
        super.doLevelStopped(level);

        setTouchable(Touchable.enabled);
        setVisible(true);
    }

    @Override
    protected void doLevelComplete(Level level) {
        super.doLevelComplete(level);

        setTouchable(Touchable.disabled);
        setVisible(false);
    }
}

package com.sloperider.component;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.sloperider.ComponentFactory;
import com.sloperider.SlopeRider;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jpx on 23/12/15.
 */
public class DraggableNetwork extends Component {
    public static interface Listener {
        void valueChanged(final DraggableNetwork self, final float value, final float quota);
    }

    private Listener _listener;

    private final List<Draggable> _draggables = new ArrayList<Draggable>();

    private float _quota;
    private float _currentValue;

    private void addDraggable(final Draggable draggable) {
        _draggables.add(draggable);

        draggable.registerListener(new Draggable.Listener() {
            @Override
            public void dragged(Draggable self, Vector2 move, Vector2 position, float deltaDistance) {
                _currentValue += deltaDistance;

                currentValueChanged(self, _currentValue);
            }
        });

        updateDraggable(draggable, _quota - _currentValue);
    }

    private void removeDraggable(final Draggable draggable) {
        _draggables.remove(draggable);
    }

    public final DraggableNetwork quota(final float value) {
        _quota = value;

        return this;
    }

    public final DraggableNetwork listener(final Listener listener) {
        _listener = listener;

        _listener.valueChanged(this, _quota, _quota);

        return this;
    }

    @Override
    public void requireAssets(AssetManager assetManager) {

    }

    @Override
    public void manageAssets(AssetManager assetManager) {

    }

    @Override
    public void doReleaseAssets(AssetManager assetManager) {

    }

    @Override
    protected void doReady(ComponentFactory componentFactory) {
        _currentValue = 0.f;

        componentFactory.registerListener(new ComponentFactory.Listener() {
            @Override
            public void componentCreated(Component component) {
                if (component instanceof Draggable)
                    addDraggable((Draggable) component);
            }

            @Override
            public void componentDestroyed(Component component) {
                if (component instanceof  Draggable)
                    removeDraggable((Draggable) component);
            }
        });
    }

    @Override
    protected void doAct(float delta) {
    }

    @Override
    protected void doDraw(Batch batch) {

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

    private void updateDraggable(final Draggable draggable, final float value) {
        draggable.limitChanged(value);
    }

    private void currentValueChanged(final Draggable changedDraggable, final float value) {
        final float left = _quota - value;

        if (_listener != null)
            _listener.valueChanged(this, left, _quota);

        for (final Draggable draggable : _draggables)
            if (draggable != changedDraggable)
                updateDraggable(draggable, left);
    }
}

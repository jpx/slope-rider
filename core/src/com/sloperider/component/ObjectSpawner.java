package com.sloperider.component;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Timer;
import com.sloperider.ComponentFactory;

/**
 * Created by jpx on 21/12/15.
 */
public class ObjectSpawner extends Component {
    private Class<? extends Component> _spawnedType;

    private float _cardinality;
    private float _delay;

    private Timer _timer;

    public final ObjectSpawner setParameters(final Class<? extends Component> spawnedType, final float cardinality, final float delay) {
        _spawnedType = spawnedType;

        _cardinality = cardinality;
        _delay = delay;

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
    protected void doReady(final ComponentFactory componentFactory) {
        _timer = new Timer();
        _timer.start();

        final Timer.Task task = new Timer.Task() {
            @Override
            public void run() {
                addComponent(componentFactory.createComponent(
                    _layer,
                    new Vector2(getX(), getY()),
                    _spawnedType
                ));
            }
        };

        if (_cardinality > 1.f)
            _timer.scheduleTask(task, _delay, _delay, (int) _cardinality - 1);
        else if (_cardinality > 0.f)
            _timer.scheduleTask(task, _delay);
        else
            _timer.scheduleTask(task, _delay, _delay);
    }

    @Override
    protected void doAct(float delta) {

    }

    @Override
    protected void doDraw(Batch batch) {

    }

    @Override
    protected void doDestroy(ComponentFactory componentFactory) {
        _timer.stop();
        _timer.clear();
        _timer = null;
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

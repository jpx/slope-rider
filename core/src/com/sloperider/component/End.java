package com.sloperider.component;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.particles.ParticleSorter;
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
        Joint joint;
    }

    private TextureRegion _textureRegion;

    private Body _body;
    private Fixture _fixture;
    private boolean _bodyNeedsUpdate;

    private final List<Sleigh> _sleightsToAdd = new ArrayList<Sleigh>();
    private final List<Sleigh> _sleightsToRemove = new ArrayList<Sleigh>();
    private final List<SleighEntry> _activeSleighs = new ArrayList<SleighEntry>();

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

            final RopeJointDef ropeJoint = new RopeJointDef();
            ropeJoint.maxLength = getWidth() / 2.f;
            ropeJoint.bodyA = _body;
            ropeJoint.bodyB = sleigh.body();
            ropeJoint.localAnchorA.set(-getWidth() / 2.f, ropeJoint.bodyA.getLocalCenter().y);
            ropeJoint.localAnchorB.set(ropeJoint.bodyB.getLocalCenter());
            ropeJoint.collideConnected = true;

            sleighEntry.joint = _body.getWorld().createJoint(ropeJoint);
        }
    }

    private void removeSleigh(final Sleigh sleigh) {
        for (int i = 0; i < _activeSleighs.size(); ++i) {
            final SleighEntry sleighEntry = _activeSleighs.get(i);

            if (sleighEntry.sleigh == sleigh) {
                _activeSleighs.remove(i);

                sleighEntry.sleigh.body().getWorld().destroyJoint(sleighEntry.joint);

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
    }

    @Override
    public void requireAssets(AssetManager assetManager) {

    }

    @Override
    public void manageAssets(AssetManager assetManager) {

    }

    @Override
    protected void doReady(ComponentFactory componentFactory) {
        _bodyNeedsUpdate = false;

        setSize(6.f, 8.f);
        setOrigin(getWidth() / 2.f, getHeight() / 2.f);

        final Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1.f, 0.f, 0.f, 0.5f);
        pixmap.fill();

        _textureRegion = new TextureRegion(new Texture(pixmap));
    }

    @Override
    protected void doAct(float delta) {

    }

    @Override
    protected void doDraw(Batch batch) {
        batch.draw(
            _textureRegion,
            getX() * SlopeRider.PIXEL_PER_UNIT, getY() * SlopeRider.PIXEL_PER_UNIT,
            getOriginX(), getOriginY(),
            getWidth(), getHeight(),
            getScaleX() * SlopeRider.PIXEL_PER_UNIT, getScaleY() * SlopeRider.PIXEL_PER_UNIT,
            getRotation()
        );
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

        for (final SleighEntry sleighEntry : _activeSleighs) {
            final float reactionForceValue = sleighEntry.joint.getReactionForce(1.f / Gdx.graphics.getDeltaTime()).len();

            if (reactionForceValue > 100.f) {
                _sleightsToRemove.add(sleighEntry.sleigh);
            }
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
    public short group() {
        return CollisionGroup.END.value();
    }

    @Override
    public short collidesWith() {
        return CollisionGroup.SLEIGH.value();
    }
}

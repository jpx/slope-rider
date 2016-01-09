package com.sloperider.component;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Joint;
import com.badlogic.gdx.physics.box2d.JointDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef;
import com.badlogic.gdx.physics.box2d.joints.WheelJointDef;
import com.sloperider.ComponentFactory;
import com.sloperider.physics.PhysicsActor;

/**
 * Created by jpx on 03/01/16.
 */
public class SleighWheel extends Component {
    static class ContactData implements PhysicsActor.ContactData {
        @Override
        public boolean contactBegin(PhysicsActor.ContactData data, Contact contact) {
            if (data instanceof Track.EdgeContactData) {

                contact.setFriction(1.f);

                return true;
            }

            return false;
        }

        @Override
        public boolean contactEnd(PhysicsActor.ContactData data, Contact contact) {
            if (data instanceof Track.EdgeContactData) {

                return true;
            }

            return false;
        }
    }
    private final Sleigh _sleigh;

    private Body _leftWheelBody;
    private Body _rightWheelBody;

    SleighWheel(final Sleigh sleigh) {
        _sleigh = sleigh;
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
        final Body body = _sleigh.body();

        final BodyDef leftWheelBodyDef = new BodyDef();
        leftWheelBodyDef.type = BodyDef.BodyType.DynamicBody;
        leftWheelBodyDef.position.set(_sleigh.getX() - 0.35f, _sleigh.getY() - 0.35f);
        final BodyDef rightWheelBodyDef = new BodyDef();
        rightWheelBodyDef.type = BodyDef.BodyType.DynamicBody;
        rightWheelBodyDef.position.set(_sleigh.getX() + 0.35f, _sleigh.getY() - 0.35f);

        final CircleShape leftWheelShape = new CircleShape();
        leftWheelShape.setRadius(0.2f);

        final CircleShape rightWheelShape = new CircleShape();
        rightWheelShape.setRadius(0.2f);

        final FixtureDef leftWheel = new FixtureDef();
        final FixtureDef rightWheel = new FixtureDef();

        leftWheel.shape = leftWheelShape;
        leftWheel.density = 0.2f;
        leftWheel.friction = 0.5f;
        leftWheel.filter.categoryBits = _sleigh.group();
        leftWheel.filter.maskBits = _sleigh.collidesWith();
        rightWheel.shape = rightWheelShape;
        rightWheel.density = 0.2f;
        rightWheel.friction = 0.5f;
        rightWheel.filter.categoryBits = _sleigh.group();
        rightWheel.filter.maskBits = _sleigh.collidesWith();

        _leftWheelBody = world.createBody(leftWheelBodyDef);
        _rightWheelBody = world.createBody(rightWheelBodyDef);

        final Fixture leftWheelFixture = _leftWheelBody.createFixture(leftWheel);
        final Fixture rightWheelFixture = _rightWheelBody.createFixture(rightWheel);

        leftWheelFixture.setUserData(new ContactData());
        rightWheelFixture.setUserData(new ContactData());

        final WheelJointDef leftWheelJoint = new WheelJointDef();
        leftWheelJoint.initialize(body, _leftWheelBody, _leftWheelBody.getPosition(), new Vector2(0.f, 1.f));
        leftWheelJoint.enableMotor = false;
        leftWheelJoint.frequencyHz = 20.f;
        leftWheelJoint.dampingRatio = 20.f;
        world.createJoint(leftWheelJoint);

        final WheelJointDef rightWheelJoint = new WheelJointDef();
        rightWheelJoint.initialize(body, _rightWheelBody, _rightWheelBody.getPosition(), new Vector2(0.f, 1.f));
        rightWheelJoint.enableMotor = false;
        rightWheelJoint.frequencyHz = 20.f;
        rightWheelJoint.dampingRatio = 20.f;
        world.createJoint(rightWheelJoint);
    }

    @Override
    public void updateBody(World world, float deltaTime) {

    }

    @Override
    public void destroyBody(World world) {
        if (_leftWheelBody != null) {
            world.destroyBody(_leftWheelBody);
            _leftWheelBody = null;
        }

        if (_rightWheelBody != null) {
            world.destroyBody(_rightWheelBody);
            _rightWheelBody = null;
        }
    }
}

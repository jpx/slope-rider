package com.sloperider.component;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
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
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.sloperider.ComponentFactory;
import com.sloperider.SlopeRider;
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

    private Texture _texture;
    private TextureRegion _textureRegion;

    SleighWheel(final Sleigh sleigh) {
        _sleigh = sleigh;
    }

    @Override
    public void requireAssets(AssetManager assetManager) {
        assetManager.load("texture/wheel_diffuse_map.png", Texture.class);
    }

    @Override
    public void manageAssets(AssetManager assetManager) {
        _texture = assetManager.get("texture/wheel_diffuse_map.png", Texture.class);
    }

    @Override
    public void doReleaseAssets(AssetManager assetManager) {
        if (assetManager.isLoaded("texture/wheel_diffuse_map.png"))
            assetManager.unload("texture/wheel_diffuse_map.png");
    }

    @Override
    protected void doReady(ComponentFactory componentFactory) {
        setTouchable(Touchable.disabled);
        setOrigin(getWidth() / 2.f, getHeight() / 2.f);

        _textureRegion = new TextureRegion(_texture);
    }

    @Override
    protected void doAct(float delta) {

    }

    @Override
    protected void doDraw(Batch batch) {
        batch.draw(
            _textureRegion,
            _leftWheelBody.getPosition().x * SlopeRider.PIXEL_PER_UNIT,
            _leftWheelBody.getPosition().y * SlopeRider.PIXEL_PER_UNIT,
            wheelSize().x / 2.f,
            wheelSize().y / 2.f,
            wheelSize().x,
            wheelSize().y,
            getScaleX() * SlopeRider.PIXEL_PER_UNIT,
            getScaleY() * SlopeRider.PIXEL_PER_UNIT,
            _leftWheelBody.getAngle() * MathUtils.degreesToRadians
        );

        batch.draw(
            _textureRegion,
            _rightWheelBody.getPosition().x * SlopeRider.PIXEL_PER_UNIT,
            _rightWheelBody.getPosition().y * SlopeRider.PIXEL_PER_UNIT,
            wheelSize().x / 2.f,
            wheelSize().y / 2.f,
            wheelSize().x,
            wheelSize().y,
            getScaleX() * SlopeRider.PIXEL_PER_UNIT,
            getScaleY() * SlopeRider.PIXEL_PER_UNIT,
            _rightWheelBody.getAngle() * MathUtils.degreesToRadians
        );
    }

    @Override
    protected void doDestroy(ComponentFactory componentFactory) {

    }

    @Override
    public void initializeBody(World world) {
        final Body body = _sleigh.body();

        final BodyDef leftWheelBodyDef = new BodyDef();
        leftWheelBodyDef.type = BodyDef.BodyType.DynamicBody;
        leftWheelBodyDef.position.set(leftWheelPosition().x, leftWheelPosition().y);
        final BodyDef rightWheelBodyDef = new BodyDef();
        rightWheelBodyDef.type = BodyDef.BodyType.DynamicBody;
        rightWheelBodyDef.position.set(rightWheelPosition().x, rightWheelPosition().y);

        final CircleShape leftWheelShape = new CircleShape();
        leftWheelShape.setRadius(wheelSize().x / 2.f);

        final CircleShape rightWheelShape = new CircleShape();
        rightWheelShape.setRadius(wheelSize().x / 2.f);

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

    @Override
    protected void doLevelPhysicsUpdate(World world, float deltaTime, int stepId, int frameId) {
        super.doLevelPhysicsUpdate(world, deltaTime, stepId, frameId);
    }

    private Vector2 wheelSize() {
        return new Vector2(0.6f, 0.6f);
    }

    private Matrix4 sleighModelToWorldMatrix() {
        return new Matrix4()
            .translate(_sleigh.getX(), _sleigh.getY(), 0.f)
            .rotate(Vector3.Z, _sleigh.getRotation())
            .scale(_sleigh.getScaleX(), _sleigh.getScaleY(), 1.f);
    }

    private Vector2 leftWheelPosition() {
        final Vector3 worldPosition = new Vector3(-0.4f, -0.4f, 0.f).mul(sleighModelToWorldMatrix());
        return new Vector2(worldPosition.x, worldPosition.y);
    }

    private Vector2 rightWheelPosition() {
        final Vector3 worldPosition = new Vector3(0.4f, -0.4f, 0.f).mul(sleighModelToWorldMatrix());
        return new Vector2(worldPosition.x, worldPosition.y);
    }
}

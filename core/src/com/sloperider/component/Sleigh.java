package com.sloperider.component;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.sloperider.ComponentFactory;
import com.sloperider.SlopeRider;
import com.sloperider.physics.CollisionGroup;
import com.sloperider.physics.PhysicsActor;

/**
 * Created by jpx on 08/11/15.
 */
public class Sleigh extends Component {
    private static final float _spriteRatio = 516.f / 298.f;

    private Texture _texture;
    private TextureRegion _textureRegion;

    private Body _body;
    private boolean _physicsEnabled;

    private Vector2 _persistentForceVector;

    public Sleigh() {
    }

    static class ContactData implements PhysicsActor.ContactData {
        Sleigh sleigh;

        ContactData(Sleigh sleigh) {
            this.sleigh = sleigh;
        }

        @Override
        public boolean contactBegin(PhysicsActor.ContactData data, Contact contact) {
            if (data instanceof Track.EdgeContactData) {
                Track.EdgeContactData edgeContactData = (Track.EdgeContactData) data;

                if (edgeContactData.material.type == Track.GroundMaterialType.BOOSTER)
                    sleigh.persistentForceVector(new Vector2(edgeContactData.normal.y, -edgeContactData.normal.x));

                return true;
            }

            return false;
        }

        @Override
        public boolean contactEnd(PhysicsActor.ContactData data, Contact contact) {
            if (data instanceof Track.EdgeContactData) {
                Track.EdgeContactData edgeContactData = (Track.EdgeContactData) data;

                if (edgeContactData.material.type == Track.GroundMaterialType.BOOSTER)
                    sleigh.persistentForceVector(null);

                return true;
            }

            return false;
        }
    }

    Body body() {
        return _body;
    }

    void persistentForceVector(final Vector2 persistentForceVector) {
        _persistentForceVector = persistentForceVector;
    }

    @Override
    public short group() {
        return CollisionGroup.SLEIGH.value();
    }

    @Override
    public short collidesWith() {
        return (short) (CollisionGroup.TRACK.value() | CollisionGroup.END.value());
    }

    @Override
    protected void setParent(Group parent) {
        super.setParent(parent);

        final float baseSize = 1.f;

        setSize(baseSize, baseSize / _spriteRatio);
        setOrigin(getWidth() / 2.f, getHeight() / 2.f);
    }

    @Override
    protected void doReady(ComponentFactory componentFactory) {
        _physicsEnabled = true;

        setTouchable(Touchable.disabled);
        _textureRegion = new TextureRegion(_texture);
    }

    @Override
    protected void doDestroy(ComponentFactory componentFactory) {

    }

    @Override
    protected void doAct(float delta) {

    }

    @Override
    protected void doDraw(Batch batch) {
        batch.draw(
            _textureRegion,
            getX() * SlopeRider.PIXEL_PER_UNIT,
            getY() * SlopeRider.PIXEL_PER_UNIT,
            getOriginX(),
            getOriginY(),
            getWidth(),
            getHeight(),
            getScaleX() * SlopeRider.PIXEL_PER_UNIT,
            getScaleY() * SlopeRider.PIXEL_PER_UNIT,
            getRotation()
        );
    }

    @Override
    public void requireAssets(AssetManager assetManager) {
        assetManager.load("texture/sleigh.png", Texture.class);
    }

    @Override
    public void manageAssets(AssetManager assetManager) {
        _texture = assetManager.get("texture/sleigh.png", Texture.class);
    }

    @Override
    public void doReleaseAssets(AssetManager assetManager) {

    }

    @Override
    public void initializeBody(World world) {

        BodyDef bodyDef = new BodyDef();

        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(getX(), getY());

        _body = world.createBody(bodyDef);

        PolygonShape shape = new PolygonShape();

        final float width = getWidth();
        final float height = getHeight();

        shape.set(new float[] {
            -width * 0.45f, -height * 0.35f,
            width * 0.38f, -height * 0.35f,
            width * 0.38f, height * 0.15f,
            -width * 0.45f, height * 0.15f
        });

        FixtureDef fixtureDef = new FixtureDef();

        fixtureDef.shape = shape;
        fixtureDef.density = 2.f;
        fixtureDef.friction = 0.1f;
        fixtureDef.restitution = 0.0f;

        fixtureDef.filter.categoryBits = group();
        fixtureDef.filter.maskBits = collidesWith();

        Fixture fixture = _body.createFixture(fixtureDef);

        fixture.setUserData(new ContactData(this));
    }

    @Override
    public void updateBody(World world) {
        if (!_physicsEnabled)
            return;

        if (_persistentForceVector != null) {
            _body.applyForceToCenter(_persistentForceVector.cpy().scl(30.f), true);
        }

        final Vector2 position = _body.getPosition().cpy();
        final float rotation = MathUtils.radiansToDegrees * _body.getAngle();

        setPosition(position.x, position.y);
        setRotation(rotation);
    }

    @Override
    public void destroyBody(World world) {
        world.destroyBody(_body);
    }

    public final boolean isMoving() {
        return _body.getLinearVelocity().len() > 1e-2f;
    }

    public final Sleigh disablePhysics() {
        _physicsEnabled = false;

        return this;
    }
}

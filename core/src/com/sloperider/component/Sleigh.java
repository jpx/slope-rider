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

    private Vector2 _boostVector;

    public Sleigh() {
    }

    static class ContactData implements PhysicsActor.ContactData {
        Sleigh sleigh;

        ContactData(Sleigh sleigh) {
            this.sleigh = sleigh;
        }

        @Override
        public boolean contactBegin(PhysicsActor.ContactData data) {
            if (data instanceof Track.EdgeContactData) {
                Track.EdgeContactData edgeContactData = (Track.EdgeContactData) data;

                if (edgeContactData.material.type == Track.GroundMaterialType.BOOSTER)
                    sleigh.boostVector(new Vector2(edgeContactData.normal.y, -edgeContactData.normal.x));

                return true;
            }

            return false;
        }

        @Override
        public boolean contactEnd(PhysicsActor.ContactData data) {
            if (data instanceof Track.EdgeContactData) {
                Track.EdgeContactData edgeContactData = (Track.EdgeContactData) data;

                if (edgeContactData.material.type == Track.GroundMaterialType.BOOSTER)
                    sleigh.boostVector(null);

                return true;
            }

            return false;
        }
    }

    void boostVector(final Vector2 boostVector) {
        _boostVector = boostVector;
    }

    @Override
    public CollisionGroup group() {
        return CollisionGroup.SLEIGH;
    }

    @Override
    public CollisionGroup collidesWith() {
        return CollisionGroup.TRACK;
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
        setTouchable(Touchable.disabled);
        _textureRegion = new TextureRegion(_texture);
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

        fixtureDef.filter.categoryBits = group().value();
        fixtureDef.filter.maskBits = collidesWith().value();

        Fixture fixture = _body.createFixture(fixtureDef);

        fixture.setUserData(new ContactData(this));
    }

    @Override
    public void updateBody(World world) {
        if (_boostVector != null) {
            Gdx.app.log(SlopeRider.TAG, "impulse: " + _boostVector.toString());
            _body.applyForceToCenter(_boostVector.cpy().scl(30.f), true);
        }

        final Vector2 position = _body.getPosition().cpy();;
        final float rotation = MathUtils.radiansToDegrees * _body.getAngle();

        setPosition(position.x, position.y);
        setRotation(rotation);
    }
}

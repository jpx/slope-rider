package com.sloperider.component;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.physics.box2d.World;
import com.sloperider.ComponentFactory;
import com.sloperider.SlopeRider;
import com.sloperider.physics.CollisionGroup;
import com.sloperider.physics.PhysicsActor;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by jpx on 21/12/15.
 */
public class RollingObject extends Component {
    private final String[] _filenames = {
        "texture/snowball.png",
        "texture/pokeball.png"
    };
    private final float[] _weights = {
        99.f / 100.f,
        1.f / 100.f
    };

    private final List<Texture> _textures = new ArrayList<Texture>();
    private TextureRegion _textureRegion;

    private Body _body;
    private Fixture _fixture;

    private static final Random _random = new RandomXS128();

    static class ContactData implements PhysicsActor.ContactData {
        @Override
        public boolean contactBegin(PhysicsActor.ContactData data, Contact contact) {
            if (data instanceof Track.EdgeContactData) {

                contact.setFriction(5.f);

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

    @Override
    public void requireAssets(AssetManager assetManager) {
        for (final String filename : _filenames)
            assetManager.load(filename, Texture.class);
    }

    @Override
    public void manageAssets(AssetManager assetManager) {
        for (final String filename : _filenames)
            _textures.add(assetManager.get(filename, Texture.class));
    }

    @Override
    public void doReleaseAssets(AssetManager assetManager) {
    }

    @Override
    protected void doReady(ComponentFactory componentFactory) {
        setSize(4.f, 4.f);
        setOrigin(getWidth() / 2.f, getHeight() / 2.f);

        int textureIndex = 0;
        while (textureIndex < _textures.size() - 1) {
            if (_random.nextFloat() < _weights[textureIndex]) {
                break;
            }

            ++textureIndex;
        }

        _textureRegion = new TextureRegion(_textures.get(textureIndex));
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
    protected void doDestroy(ComponentFactory componentFactory) {

    }

    @Override
    public void initializeBody(World world) {
        final BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(getX(), getY());

        final FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.density = 3.f;

        final Shape shape = new CircleShape();
        shape.setRadius(Math.max(getWidth(), getHeight()) * 0.9f / 2.f);

        fixtureDef.shape = shape;
        fixtureDef.friction = 5.f;
        fixtureDef.filter.categoryBits = group();
        fixtureDef.filter.maskBits = collidesWith();

        _body = world.createBody(bodyDef);
        _fixture = _body.createFixture(fixtureDef);

        _fixture.setUserData(new ContactData());
    }

    @Override
    public void updateBody(World world, float deltaTime) {
        final Vector2 position = _body.getPosition();

        setPosition(position.x, position.y);
        setRotation(MathUtils.radiansToDegrees * _body.getAngle());
    }

    @Override
    public void destroyBody(World world) {
        world.destroyBody(_body);
    }

    @Override
    public short group() {
        return CollisionGroup.TRACK.value();
    }

    @Override
    public short collidesWith() {
        return (short) (CollisionGroup.TRACK.value() | CollisionGroup.MAIN_CHARACTER.value());
    }
}

package com.sloperider.component;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.World;
import com.sloperider.ComponentFactory;
import com.sloperider.EventLogger;
import com.sloperider.physics.CollisionGroup;
import com.sloperider.physics.PhysicsActor;
import com.sloperider.physics.SmoothingState;

public abstract class MainCharacter extends Component {
    static class ContactData implements PhysicsActor.ContactData {
        MainCharacter mainCharacter;

        ContactData(MainCharacter mainCharacter) {
            this.mainCharacter = mainCharacter;
        }

        @Override
        public boolean contactBegin(PhysicsActor.ContactData data, Contact contact) {
            if (data instanceof Track.EdgeContactData) {
                Track.EdgeContactData edgeContactData = (Track.EdgeContactData) data;

                if (edgeContactData.material.type == Track.GroundMaterialType.BOOSTER)
                    mainCharacter.persistentForceVector(new Vector2(edgeContactData.normal.y, -edgeContactData.normal.x));

                return true;
            }

            return false;
        }

        @Override
        public boolean contactEnd(PhysicsActor.ContactData data, Contact contact) {
            if (data instanceof Track.EdgeContactData) {
                Track.EdgeContactData edgeContactData = (Track.EdgeContactData) data;

                if (edgeContactData.material.type == Track.GroundMaterialType.BOOSTER)
                    mainCharacter.persistentForceVector(null);

                return true;
            }

            return false;
        }
    }

    protected Body _body;
    protected Vector2 _persistentForceVector;
    protected boolean _physicsEnabled;

    private final SmoothingState _smoothingState = new SmoothingState();

    @Override
    public short group() {
        return CollisionGroup.MAIN_CHARACTER.value();
    }

    @Override
    public short collidesWith() {
        return (short) (CollisionGroup.TRACK.value() | CollisionGroup.END.value());
    }

    @Override
    protected void doReady(ComponentFactory componentFactory) {
        _physicsEnabled = true;
    }

    public final Body body() {
        return _body;
    }

    public final void persistentForceVector(final Vector2 persistentForceVector) {
        _persistentForceVector = persistentForceVector;
    }

    public final boolean isMoving() {
        return _body == null ? true : _body.getLinearVelocity().len() > 1e-2f;
    }

    public final MainCharacter disablePhysics() {
        _physicsEnabled = false;

        return this;
    }

    public final float getBodyX() {
        return _body.getPosition().x;
    }

    public final float getBodyY() {
        return _body.getPosition().y;
    }

    public final float getBodyAngle() {
        return _body.getAngle();
    }

    @Override
    public void updateBody(World world, float deltaTime) {
        if (!_physicsEnabled) {
            if (_body != null) {
                destroyBody(world);
            }

            return;
        }

        if (_persistentForceVector != null) {
            _body.applyForceToCenter(_persistentForceVector.cpy().scl(30.f), true);
        }

        EventLogger.instance().log("mainCharacter.position", _body.getPosition().toString());
    }

    @Override
    public void resetSmoothingState(World world, float deltaTime) {
        super.resetSmoothingState(world, deltaTime);

        if (!_physicsEnabled)
            return;

        _smoothingState.smoothedPosition.set(_body.getPosition());
        _smoothingState.previousPosition.set(_body.getPosition());
        _smoothingState.smoothedRotation = _smoothingState.previousRotation = _body.getAngle() * MathUtils.radiansToDegrees;
    }

    @Override
    public void applySmoothingState(World world, float deltaTime, float alpha) {
        super.applySmoothingState(world, deltaTime, alpha);

        if (!_physicsEnabled)
            return;

        _smoothingState.smoothedPosition.set(_body.getPosition().cpy()
            .scl(alpha)
            .add(_smoothingState.previousPosition.cpy().scl(1.f - alpha)));

        _smoothingState.smoothedRotation = _body.getAngle() * MathUtils.radiansToDegrees *
            alpha + _smoothingState.previousRotation * (1.f - alpha);

        setPosition(_smoothingState.smoothedPosition.x, _smoothingState.smoothedPosition.y);
        setRotation(_smoothingState.smoothedRotation);
    }

    @Override
    public void destroyBody(World world) {
        if (_body == null)
            return;

        world.destroyBody(_body);
        _body = null;
    }
}

/******************************************************************************
 * Spine Runtimes License Agreement
 * Last updated July 28, 2023. Replaces all prior versions.
 *
 * Copyright (c) 2013-2023, Esoteric Software LLC
 *
 * Integration of the Spine Runtimes into software or otherwise creating
 * derivative works of the Spine Runtimes is permitted under the terms and
 * conditions of Section 2 of the Spine Editor License Agreement:
 * http://esotericsoftware.com/spine-editor-license
 *
 * Otherwise, it is permitted to integrate the Spine Runtimes into software or
 * otherwise create derivative works of the Spine Runtimes (collectively,
 * "Products"), provided that each user of the Products must obtain their own
 * Spine Editor license and redistribution of the Products in any form must
 * include this license and copyright notice.
 *
 * THE SPINE RUNTIMES ARE PROVIDED BY ESOTERIC SOFTWARE LLC "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL ESOTERIC SOFTWARE LLC BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES,
 * BUSINESS INTERRUPTION, OR LOSS OF USE, DATA, OR PROFITS) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THE
 * SPINE RUNTIMES, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *****************************************************************************/

package com.sloperider;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Null;

import com.esotericsoftware.spine.Animation.MixBlend;
import com.esotericsoftware.spine.Animation.MixDirection;
import com.esotericsoftware.spine.Skeleton.Physics;
import com.esotericsoftware.spine.SkeletonRenderer;
import com.esotericsoftware.spine.Animation;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.*;
import com.esotericsoftware.spine.attachments.AtlasAttachmentLoader;
import com.esotericsoftware.spine.attachments.RegionAttachment;
import com.esotericsoftware.spine.attachments.Sequence;
import com.sloperider.component.MainCharacter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import kotlin.Pair;

/** Demonstrates positioning physics bodies for a skeleton. */
public class Box2DCharacter extends MainCharacter {
//    SpriteBatch batch;
    ShapeRenderer renderer;
    SkeletonRenderer skeletonRenderer;

    TextureAtlas atlas;
    Skeleton skeleton;
    Animation animation;
    float time;
    Array<Event> events = new Array();

//    OrthographicCamera camera;
//    Box2DDebugRenderer box2dRenderer;
//    World world;
//    Body groundBody;
//    Matrix4 transform = new Matrix4();
    Vector2 vector = new Vector2();

    public void create () {

    }

    public void render () {
//        float delta = Gdx.graphics.getDeltaTime();
//        float remaining = delta;
//        while (remaining > 0) {
//            float d = Math.min(0.016f, remaining);
//            world.step(d, 8, 3);
//            time += d;
//            remaining -= d;
//        }
//
//        camera.update();
//
//        ScreenUtils.clear(0, 0, 0, 0);
//        batch.setProjectionMatrix(camera.projection);
//        batch.setTransformMatrix(camera.view);
//        batch.begin();

//        batch.end();

//        box2dRenderer.render(world, camera.combined);
    }

//    public void resize (int width, int height) {
//        batch.setProjectionMatrix(camera.projection);
//        renderer.setProjectionMatrix(camera.projection);
//    }

    @Override
    public void requireAssets(AssetManager assetManager) {

    }

    @Override
    public void manageAssets(AssetManager assetManager) {

    }

    @Override
    public void doReleaseAssets(AssetManager assetManager) {
        atlas.dispose();
    }

    @Override
    protected void doReady(ComponentFactory componentFactory) {
//        batch = new SpriteBatch();
        renderer = new ShapeRenderer();
        skeletonRenderer = new SkeletonRenderer();
        skeletonRenderer.setPremultipliedAlpha(true);

        atlas = new TextureAtlas(Gdx.files.internal("spineboy/spineboy-pma.atlas"));

        // This loader creates Box2dAttachments instead of RegionAttachments for an easy way to keep track of the Box2D body for
        // each attachment.
        AtlasAttachmentLoader atlasLoader = new AtlasAttachmentLoader(atlas) {
            public RegionAttachment newRegionAttachment (Skin skin, String name, String path, @Null Sequence sequence) {
                Box2dAttachment attachment = new Box2dAttachment(name);
                Gdx.app.log(SlopeRider.TAG, "skeleton part=" + name);
                AtlasRegion region = atlas.findRegion(attachment.getName());
                if (region == null) throw new RuntimeException("Region not found in atlas: " + attachment);
                attachment.setRegion(region);
                return attachment;
            }
        };
        SkeletonJson json = new SkeletonJson(atlasLoader);
        json.setScale(0.6f * 0.05f * 0.1f);
        SkeletonData skeletonData = json.readSkeletonData(Gdx.files.internal("spineboy/spineboy-ess.json"));
        animation = skeletonData.findAnimation("walk");

        skeleton = new Skeleton(skeletonData);
        skeleton.setPosition(getX() + 5f, getY());
        skeleton.updateWorldTransform(Physics.update);

        // See Box2DTest in libgdx for more detailed information about Box2D setup.
//        camera = new OrthographicCamera(48, 32);
//        camera.position.set(0, 16, 0);
//        box2dRenderer = new Box2DDebugRenderer();
//        createWorld();
    }

    @Override
    protected void doAct(float delta) {
        time += delta;
        animation.apply(skeleton, time, time, true, events, 1, MixBlend.first, MixDirection.in);
//        skeleton.setX(skeleton.getX() + 8 * delta);
        skeleton.update(delta);
        skeleton.updateWorldTransform(Physics.update);
    }

    @Override
    protected void doDraw(Batch batch) {
        skeletonRenderer.draw(batch, skeleton);
    }

    @Override
    protected void doDestroy(ComponentFactory componentFactory) {

    }

    @Override
    public void initializeBody(World world) {
        List<Pair<Slot, Slot>> childAndParentSlots = new ArrayList<>();

        // Create a body for each attachment. Note it is probably better to create just a few bodies rather than one for each
        // region attachment, but this is just an example.
        for (Slot slot : skeleton.getSlots()) {
            if (!(slot.getAttachment() instanceof Box2dAttachment)) continue;
            Box2dAttachment attachment = (Box2dAttachment)slot.getAttachment();

            PolygonShape boxPoly = new PolygonShape();
            boxPoly.setAsBox(attachment.getWidth() / 2 * attachment.getScaleX(), attachment.getHeight() / 2 * attachment.getScaleY(),
                vector.set(attachment.getX(), attachment.getY()), attachment.getRotation() * MathUtils.degRad);

            BodyDef boxBodyDef = new BodyDef();
            boxBodyDef.type = BodyType.DynamicBody;
            attachment.body = world.createBody(boxBodyDef);

            FixtureDef fixture = new FixtureDef();
            fixture.shape = boxPoly;
            fixture.isSensor = false;
            fixture.filter.categoryBits = group();
            fixture.filter.maskBits = collidesWith();
            attachment.body.createFixture(boxPoly, 1);

            float x = slot.getBone().getWorldX();
            float y = slot.getBone().getWorldY();
            float rotation = slot.getBone().getWorldRotationX();
            attachment.body.setTransform(x, y, rotation * MathUtils.degRad);

//            Gdx.app.log(SlopeRider.TAG, "[1] slot=" + slot.getData().getName() +
//                ", parent=" + slot.getBone().getParent().getData().getName());

            Slot parentSlot = Arrays.stream(skeleton.getSlots().toArray(Slot.class))
                .filter(s -> slot.getBone().getParent().equals(s.getBone()))
                .findFirst()
                .orElse(null);

            if (parentSlot == null) {
                Gdx.app.log(SlopeRider.TAG, "found root=" + slot.getData().getName());

                if (_body == null) {
                    _body = attachment.body;
                }
            }

            boxPoly.dispose();
        }

        for (Slot slot : skeleton.getSlots()) {
            if (!(slot.getAttachment() instanceof Box2dAttachment)) continue;
            Box2dAttachment attachment = (Box2dAttachment) slot.getAttachment();

            Slot parentSlot = Arrays.stream(skeleton.getSlots().toArray(Slot.class))
                .filter(s -> slot.getBone().getParent().equals(s.getBone()))
                .findFirst()
                .orElse(null);

            if (parentSlot == null) {
                continue;
            }

//            Gdx.app.log(SlopeRider.TAG, "[2] slot=" + slot.getData().getName() + ", parent=" + parentSlot.getData().getName());

            if (parentSlot.getAttachment() instanceof Box2dAttachment) {
                Box2dAttachment parentAttachment = (Box2dAttachment) parentSlot.getAttachment();

                RevoluteJointDef jointDef = new RevoluteJointDef();
                jointDef.initialize(parentAttachment.body, attachment.body, new Vector2(slot.getBone().getWorldX(), slot.getBone().getWorldY()));
                world.createJoint(jointDef);
            }
        }
    }

    @Override
    public void updateBody(World world, float deltaTime) {
        // Position the physics body for each attachment.
//        for (Slot slot : skeleton.getSlots()) {
//            if (!(slot.getAttachment() instanceof Box2dAttachment)) continue;
//            Box2dAttachment attachment = (Box2dAttachment)slot.getAttachment();
//            if (attachment.body == null) continue;
//            float x = slot.getBone().getWorldX();
//            float y = slot.getBone().getWorldY();
//            float rotation = slot.getBone().getWorldRotationX();
//            attachment.body.setTransform(x, y, rotation * MathUtils.degRad);
//        }
    }

    @Override
    public void destroyBody(World world) {

    }

    static class Box2dAttachment extends RegionAttachment {
        Body body;

        public Box2dAttachment (String name) {
            super(name);
        }
    }
}

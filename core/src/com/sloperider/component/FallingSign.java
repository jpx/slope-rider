package com.sloperider.component;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.sloperider.ComponentFactory;
import com.sloperider.SlopeRider;

/**
 * Created by jpx on 26/12/15.
 */
public class FallingSign extends Component {
    private static final float _spriteRatio = 357.f / 566.f;

    private Texture _texture;
    private TextureRegion _textureRegion;

    @Override
    public void requireAssets(AssetManager assetManager) {
        assetManager.load("texture/falling_sign.png", Texture.class);
    }

    @Override
    public void manageAssets(AssetManager assetManager) {
        _texture = assetManager.get("texture/falling_sign.png", Texture.class);
        _texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    @Override
    public void doReleaseAssets(AssetManager assetManager) {

    }

    @Override
    protected void doReady(ComponentFactory componentFactory) {
        _textureRegion = new TextureRegion(_texture);

        final float baseSize = 2.f;

        setTouchable(Touchable.disabled);

        setSize(baseSize, baseSize / _spriteRatio);
        setOrigin(baseSize / 2.f, 0.f);
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
    protected void doDestroy(ComponentFactory componentFactory) {

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

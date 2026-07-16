package br.com.diginteligente.diagnostico;

import android.os.Bundle;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import java.util.ArrayList;
import java.util.List;

public class FourDArenaActivity extends AndroidApplication {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useImmersiveMode = true;
        config.useAccelerometer = false;
        config.useCompass = false;
        initialize(new ArenaGame(), config);
    }

    private static class ArenaGame extends ApplicationAdapter {
        private ModelBatch batch;
        private PerspectiveCamera camera;
        private Environment environment;
        private final List<Model> models = new ArrayList<>();
        private final List<ModelInstance> cars = new ArrayList<>();
        private ModelInstance track;
        private float time;
        private float cameraAngle = 25f;
        private float lastX;

        @Override public void create() {
            batch = new ModelBatch();
            camera = new PerspectiveCamera(62, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            camera.near = .1f; camera.far = 160f;
            environment = new Environment();
            environment.set(new ColorAttribute(ColorAttribute.AmbientLight, .34f, .38f, .44f, 1f));
            environment.add(new DirectionalLight().set(1f, .92f, .78f, -1f, -1.4f, -.7f));
            ModelBuilder builder = new ModelBuilder();
            long attrs = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;
            Model road = builder.createBox(25f, .25f, 44f, new Material(ColorAttribute.createDiffuse(new Color(.12f,.15f,.18f,1))), attrs);
            models.add(road); track = new ModelInstance(road);
            Color[] palette = {Color.RED,Color.BLUE,Color.GREEN,Color.GOLD,Color.PURPLE,Color.CYAN,Color.ORANGE,Color.PINK,Color.LIME,Color.WHITE};
            for (int i=0;i<10;i++) {
                Model car = builder.createBox(1.35f,.65f,2.45f,new Material(ColorAttribute.createDiffuse(palette[i])),attrs);
                models.add(car); cars.add(new ModelInstance(car));
            }
            Gdx.input.vibrate(35);
        }

        @Override public void render() {
            float dt = Math.min(.04f, Gdx.graphics.getDeltaTime()); time += dt;
            if (Gdx.input.isTouched()) {
                float x=Gdx.input.getX(); if(lastX!=0)cameraAngle+=(x-lastX)*.08f; lastX=x;
            } else lastX=0;
            float radius=36f;
            camera.position.set(MathUtils.cosDeg(cameraAngle)*radius,20f,MathUtils.sinDeg(cameraAngle)*radius);
            camera.lookAt(Vector3.Zero); camera.up.set(Vector3.Y); camera.update();
            for(int i=0;i<cars.size();i++){
                float phase=time*(22f+i*.32f)+i*4.1f;
                float z=((phase%42f)-21f);
                float x=-9f+(i%5)*4.5f+MathUtils.sin(time*.7f+i)*.35f;
                cars.get(i).transform.setToTranslation(x,.55f,z);
            }
            Gdx.gl.glViewport(0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
            Gdx.gl.glClearColor(.025f,.045f,.065f,1); Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT|GL20.GL_DEPTH_BUFFER_BIT);
            batch.begin(camera); batch.render(track,environment); for(ModelInstance car:cars)batch.render(car,environment); batch.end();
        }

        @Override public void resize(int width,int height){camera.viewportWidth=width;camera.viewportHeight=height;camera.update();}
        @Override public void dispose(){batch.dispose();for(Model model:models)model.dispose();}
    }
}

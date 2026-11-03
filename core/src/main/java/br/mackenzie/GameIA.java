package br.mackenzie;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

public class GameIA extends ApplicationAdapter {
    private ShapeRenderer formas;
    private OrthographicCamera camera;
    private Agente npc;
    private Agente jogador;
    private Vector2 alvoClique;
    private Estado estado = Estado.WANDER;

    private enum Estado { WANDER, ARRIVE, PURSUIT }
    private static final float RAIO_VISAO = 220f;
    private static final float RAIO_CHEGADA = 55f;

    @Override
    public void create() {
        formas = new ShapeRenderer();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        npc = new Agente(Gdx.graphics.getWidth() * 0.25f, Gdx.graphics.getHeight() * 0.5f);
        jogador = new Agente(Gdx.graphics.getWidth() * 0.75f, Gdx.graphics.getHeight() * 0.5f);
        jogador.maxVelocidade = 180f;
        alvoClique = null;

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (button == Input.Buttons.LEFT) {
                    alvoClique = new Vector2(screenX, Gdx.graphics.getHeight() - screenY);
                }
                return true;
            }
        });
    }

    @Override
    public void render() {
        float delta = Math.min(Gdx.graphics.getDeltaTime(), 1f / 30f);
        atualizarJogador(delta);

        if (npc.posicao.dst(jogador.posicao) < RAIO_VISAO) {
            estado = Estado.PURSUIT;
        } else if (alvoClique != null) {
            estado = Estado.ARRIVE;
        } else {
            estado = Estado.WANDER;
        }

        Vector2 steering;
        switch (estado) {
            case PURSUIT:
                steering = npc.pursuit(jogador);
                break;
            case ARRIVE:
                steering = npc.arrive(alvoClique, RAIO_CHEGADA);
                if (npc.posicao.dst(alvoClique) < 5f && npc.velocidade.len() < 12f) {
                    alvoClique = null;
                }
                break;
            default:
                steering = npc.wander();
                break;
        }
        npc.mover(steering, delta);
        npc.limitarTela(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        Gdx.gl.glClearColor(0.12f, 0.12f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camera.update();
        formas.setProjectionMatrix(camera.combined);
        formas.begin(ShapeRenderer.ShapeType.Line);
        formas.setColor(0.3f, 0.3f, 0.3f, 1);
        formas.circle(jogador.posicao.x, jogador.posicao.y, RAIO_VISAO);
        if (alvoClique != null) {
            formas.setColor(1, 1, 1, 1);
            formas.circle(alvoClique.x, alvoClique.y, RAIO_CHEGADA);
            formas.line(npc.posicao, alvoClique);
        }
        formas.setColor(0, 1, 0, 1);
        formas.line(npc.posicao, new Vector2(npc.posicao).mulAdd(npc.velocidade, 0.35f));
        formas.setColor(0, 0.5f, 1, 1);
        formas.line(npc.posicao, new Vector2(npc.posicao).mulAdd(steering, 0.12f));
        formas.end();

        formas.begin(ShapeRenderer.ShapeType.Filled);
        formas.setColor(1, 0.85f, 0.1f, 1);
        formas.circle(npc.posicao.x, npc.posicao.y, 12);
        formas.setColor(0.2f, 0.8f, 1, 1);
        formas.circle(jogador.posicao.x, jogador.posicao.y, 10);
        formas.end();
    }

    private void atualizarJogador(float delta) {
        Vector2 direcao = new Vector2();
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) direcao.x--;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) direcao.x++;
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) direcao.y--;
        if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) direcao.y++;
        if (!direcao.isZero()) {
            direcao.nor().scl(jogador.maxVelocidade);
            jogador.velocidade.set(direcao);
        } else {
            jogador.velocidade.setZero();
        }
        jogador.posicao.mulAdd(jogador.velocidade, delta);
        jogador.limitarTela(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    @Override
    public void dispose() {
        formas.dispose();
    }
}

package br.mackenzie;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.MathUtils;
import java.util.ArrayList;
import java.util.List;

public class GameIA extends ApplicationAdapter {
    private ShapeRenderer formas;
    private OrthographicCamera camera;
    private Agente npc;
    private Agente jogador;
    private Vector2 alvoClique;
    private Estado estado = Estado.WANDER;
    private List<Agente> bando = new ArrayList<>();
    private static final int QUANTIDADE_AGENTES = 30;
    private static final float RAIO_VIZINHOS = 95f;
    private static final float PESO_PERSEGUICAO = 1.2f;
    private static final float PESO_SEPARACAO = 1.8f;
    private static final float PESO_ALINHAMENTO = 0.7f;
    private static final float PESO_COESAO = 1.0f;
    private static final float DISTANCIA_NPC = 48f;

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
        // Os 30 agentes começam agrupados ao redor do NPC.
        for (int i = 0; i < QUANTIDADE_AGENTES; i++) {
            float angulo = i * 360f / QUANTIDADE_AGENTES;
            float distancia = MathUtils.random(90f, 145f);
            float x = npc.posicao.x + MathUtils.cosDeg(angulo) * distancia;
            float y = npc.posicao.y + MathUtils.sinDeg(angulo) * distancia;
            x = MathUtils.clamp(x, 20f, Gdx.graphics.getWidth() - 20f);
            y = MathUtils.clamp(y, 20f, Gdx.graphics.getHeight() - 20f);
            Agente agente = new Agente(x, y);
            agente.maxVelocidade = 105f;
            agente.maxForca = 190f;
            bando.add(agente);
        }

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

        // Bônus: os 30 agentes perseguem o NPC e usam Flocking
        // para permanecer organizados como um bando.
        for (Agente agente : bando) {
            Vector2 perseguirNpc = agente.pursuit(npc);
            Vector2 manterDistancia = new Vector2();
            if (agente.posicao.dst(npc.posicao) < DISTANCIA_NPC) {
                manterDistancia = agente.flee(npc.posicao);
            }

            Vector2 forcaBando = new Vector2()
                    .mulAdd(perseguirNpc, PESO_PERSEGUICAO)
                    .mulAdd(separacao(agente), PESO_SEPARACAO)
                    .mulAdd(alinhamento(agente), PESO_ALINHAMENTO)
                    .mulAdd(coesao(agente), PESO_COESAO)
                    .mulAdd(manterDistancia, 1.0f)
                    .limit(agente.maxForca);
            agente.mover(forcaBando, delta);
            agente.limitarTela(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        }

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
        // Linhas azuis mostram a direção atual dos agentes do bando.
        formas.setColor(0.25f, 0.55f, 0.9f, 1);
        for (Agente agente : bando) {
            formas.line(agente.posicao,
                    new Vector2(agente.posicao).mulAdd(agente.velocidade, 0.25f));
        }
        formas.end();

        formas.begin(ShapeRenderer.ShapeType.Filled);
        formas.setColor(1, 0.85f, 0.1f, 1);
        formas.circle(npc.posicao.x, npc.posicao.y, 12);
        formas.setColor(0.2f, 0.8f, 1, 1);
        formas.circle(jogador.posicao.x, jogador.posicao.y, 10);
        // Linhas azuis mostram a direção atual dos agentes do bando.
        formas.setColor(0.25f, 0.55f, 0.9f, 1);
        for (Agente agente : bando) {
            formas.circle(agente.posicao.x, agente.posicao.y, 4);
        }
        formas.end();
    }

    private Vector2 separacao(Agente agente) {
        Vector2 soma = new Vector2();
        int quantidade = 0;
        for (Agente outro : bando) {
            if (outro == agente) continue;
            float distancia = agente.posicao.dst(outro.posicao);
            if (distancia > 0 && distancia < RAIO_VIZINHOS) {
                soma.add(new Vector2(agente.posicao).sub(outro.posicao).nor().scl(1f / distancia));
                quantidade++;
            }
        }
        if (quantidade == 0) return new Vector2();
        soma.scl(1f / quantidade);
        if (soma.isZero()) return soma;
        return soma.nor().scl(agente.maxVelocidade).sub(agente.velocidade);
    }

    private Vector2 alinhamento(Agente agente) {
        Vector2 media = new Vector2();
        int quantidade = 0;
        for (Agente outro : bando) {
            if (outro == agente || agente.posicao.dst(outro.posicao) >= RAIO_VIZINHOS) continue;
            media.add(outro.velocidade);
            quantidade++;
        }
        if (quantidade == 0) return new Vector2();
        media.scl(1f / quantidade);
        if (media.isZero()) return media;
        return media.nor().scl(agente.maxVelocidade).sub(agente.velocidade);
    }

    private Vector2 coesao(Agente agente) {
        Vector2 centro = new Vector2();
        int quantidade = 0;
        for (Agente outro : bando) {
            if (outro == agente || agente.posicao.dst(outro.posicao) >= RAIO_VIZINHOS) continue;
            centro.add(outro.posicao);
            quantidade++;
        }
        if (quantidade == 0) return new Vector2();
        centro.scl(1f / quantidade);
        Vector2 desejada = centro.sub(agente.posicao);
        if (desejada.isZero()) return desejada;
        return desejada.nor().scl(agente.maxVelocidade).sub(agente.velocidade);
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

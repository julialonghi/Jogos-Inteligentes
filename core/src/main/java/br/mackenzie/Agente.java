package br.mackenzie;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

public class Agente {
    public Vector2 posicao = new Vector2();
    public Vector2 velocidade = new Vector2();
    public float maxVelocidade = 140f;
    public float maxForca = 260f;
    public float massa = 1f;

    private float anguloWander = 0f;
    private static final float DIST_CIRCULO = 55f;
    private static final float RAIO_CIRCULO = 28f;
    private static final float VARIACAO = 0.18f;

    public Agente(float x, float y) {
        posicao.set(x, y);
        velocidade.set(1, 0).setAngleDeg(MathUtils.random(0f, 360f)).scl(25f);
    }

    public Vector2 seek(Vector2 alvo) {
        Vector2 desejada = new Vector2(alvo).sub(posicao);
        if (desejada.isZero()) return new Vector2();
        desejada.nor().scl(maxVelocidade);
        return desejada.sub(velocidade);
    }

    public Vector2 arrive(Vector2 alvo, float raio) {
        Vector2 desejada = new Vector2(alvo).sub(posicao);
        float distancia = desejada.len();
        if (distancia == 0) return new Vector2();
        float vel = distancia < raio ? maxVelocidade * distancia / raio : maxVelocidade;
        desejada.nor().scl(vel);
        return desejada.sub(velocidade);
    }

    public Vector2 wander() {
        Vector2 centro = new Vector2(velocidade);
        if (centro.isZero()) centro.set(1, 0);
        centro.nor().scl(DIST_CIRCULO);
        anguloWander += MathUtils.random(-VARIACAO, VARIACAO);
        Vector2 deslocamento = new Vector2(RAIO_CIRCULO, 0).setAngleRad(anguloWander);
        return centro.add(deslocamento);
    }

    public Vector2 pursuit(Agente alvo) {
        float t = posicao.dst(alvo.posicao) / maxVelocidade;
        t = MathUtils.clamp(t, 0f, 1.5f);
        Vector2 futuro = new Vector2(alvo.posicao).mulAdd(alvo.velocidade, t);
        return seek(futuro);
    }

    public Vector2 flee(Vector2 ameaca) {
        Vector2 desejada = new Vector2(posicao).sub(ameaca);
        if (desejada.isZero()) return new Vector2();
        desejada.nor().scl(maxVelocidade);
        return desejada.sub(velocidade);
    }

    public void mover(Vector2 steering, float delta) {
        steering.limit(maxForca).scl(1f / massa);
        velocidade.mulAdd(steering, delta).limit(maxVelocidade);
        posicao.mulAdd(velocidade, delta);
    }

    public void limitarTela(float largura, float altura) {
        posicao.x = MathUtils.clamp(posicao.x, 12f, largura - 12f);
        posicao.y = MathUtils.clamp(posicao.y, 12f, altura - 12f);
    }
}

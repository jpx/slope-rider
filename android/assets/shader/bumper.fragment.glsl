#ifdef GL_ES
precision mediump float;
#endif

uniform float u_time;

varying vec2 v_uv;

#define PI_2 1.570796

vec3 radialImpulse(vec2 uv, float time, vec3 color0, vec3 color1)
{
    float amplitude = 0.4;
    float frequency = 3.0;

    float distance = distance(vec2(0.5), uv);
    float offset = distance / amplitude;
    float pulseRate = time * PI_2 * frequency;

    return mix(color0, color1, abs(sin(offset + pulseRate)));
}

void main()
{
    vec3 color0 = vec3(0.2, 0.9, 0.1);
    vec3 color1 = color0 * 0.6;

    vec3 diffuse = radialImpulse(v_uv, u_time, color0, color1);

    float distance = distance(vec2(0.5, 0.5), v_uv);

    float dotValue = dot(vec2(0.5) - v_uv, vec2(-1.0, 1.0));

    if (distance > 0.55 ||
        v_uv.x < 0.05 || v_uv.x > 0.95 ||
        v_uv.y < 0.05 || v_uv.y > 0.95 ||
        dotValue > 0.0)
        discard;

    gl_FragColor = vec4(diffuse, 1.0);
}

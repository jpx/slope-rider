#ifdef GL_ES
precision mediump float;
#endif

uniform float u_time;
uniform sampler2D u_diffuseMask;
uniform sampler2D u_baseDiffuseMask;

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

vec3 impulse(vec3 color0, vec3 color1, vec2 uv, vec2 uvMask, float amplitude, float frequency, float time)
{
    float offset = dot(uv, uvMask) / amplitude;
    float pulseRate = time * PI_2 * frequency;

    return mix(color1, color0, abs(sin(offset + pulseRate)));
}

void main()
{
    vec3 diffuse = vec3(0.0);

    vec4 diffuseMask = texture2D(u_diffuseMask, v_uv);
    vec4 baseDiffuseMask = texture2D(u_baseDiffuseMask, v_uv);

    vec3 topColor1 = vec3(0.2, 0.95, 0.6);
    vec3 topColor0 = topColor1 * 0.5;

    diffuse += impulse(topColor0, topColor1, v_uv, vec2(0.0, 1.0), 0.1, 4.0, u_time) * diffuseMask.r;

    vec3 baseColor0 = vec3(0.0);
    vec3 baseColor1 = vec3(1.0);

    diffuse += (baseColor0 * baseDiffuseMask.r +
                baseColor1 * (1.0 - baseDiffuseMask.r)) *
                diffuseMask.g;

    gl_FragColor = vec4(diffuse, diffuseMask.a);
}

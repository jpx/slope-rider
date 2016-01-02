#ifdef GL_ES
precision mediump float;
#endif

uniform float u_time;

uniform sampler2D u_splattingMap0;
uniform sampler2D u_splattingMap1;
uniform sampler2D u_splattingMap2;
uniform sampler2D u_splattingMap3;

varying vec2 v_normal;
varying vec2 v_uv;
varying vec4 v_mask0;
varying vec4 v_mask1;

#define PI_2 1.570796

vec3 impulse(vec3 color0, vec3 color1, vec2 uv, vec2 uvMask, float amplitude, float frequency, float time)
{
    float offset = dot(uv, uvMask) / amplitude;
    float pulseRate = -time * PI_2 * frequency;

    return mix(color1, color0, abs(sin(offset + pulseRate)));
}

void main()
{
    vec3 diffuse = vec3(0.0);

    vec3 splattingMap3Sample = texture2D(u_splattingMap3, v_uv).rgb * v_mask0.w;

    diffuse =
        texture2D(u_splattingMap0, v_uv).rgb * v_mask0.x +
        texture2D(u_splattingMap1, v_uv).rgb * v_mask0.y +
        texture2D(u_splattingMap2, v_uv).rgb * v_mask0.z +
        impulse(splattingMap3Sample, splattingMap3Sample * 0.5, v_uv, vec2(1.0, 0.0), 8.0, 2.0, u_time) +
        vec3(0.0, 1.0, 0.0) * v_mask1.x +
        vec3(0.0, 1.0, 1.0) * v_mask1.y +
        vec3(0.0, 0.0, 1.0) * v_mask1.z +
        vec3(1.0, 1.0, 0.0) * v_mask1.w;

    diffuse /= v_mask0.x + v_mask0.y + v_mask0.z + v_mask0.w + v_mask1.x + v_mask1.y + v_mask1.z + v_mask1.w;

    gl_FragColor = vec4(diffuse, 1.0);
}

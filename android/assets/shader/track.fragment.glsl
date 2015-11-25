#ifdef GL_ES
precision mediump float;
#endif

uniform float u_time;

uniform sampler2D u_splattingMap0;
uniform sampler2D u_splattingMap1;
uniform sampler2D u_splattingMap2;
uniform sampler2D u_splattingMap3;

varying vec2 v_uv;
varying vec4 v_mask;

#define PI_2 1.570796

void main()
{
    vec3 diffuse = vec3(0.0);

    vec3 splattingMap3Sample = texture2D(u_splattingMap3, v_uv).rgb * v_mask.w;

    float amplitude = 8.0;
    float frequency = 2.0;

    float offset = dot(v_uv, vec2(1.0, 0.0)) / amplitude;
    float pulseRate = -u_time * PI_2 * frequency;

    splattingMap3Sample = mix(splattingMap3Sample * 0.5, splattingMap3Sample, abs(sin(offset + pulseRate)));

    diffuse =
        texture2D(u_splattingMap0, v_uv).rgb * v_mask.x +
        texture2D(u_splattingMap1, v_uv).rgb * v_mask.y +
        texture2D(u_splattingMap2, v_uv).rgb * v_mask.z +
        splattingMap3Sample;

    diffuse /= v_mask.x + v_mask.y + v_mask.z + v_mask.w;

    gl_FragColor = vec4(diffuse, 1.0);
}

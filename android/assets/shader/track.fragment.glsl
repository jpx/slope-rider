#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D u_splattingMap0;
uniform sampler2D u_splattingMap1;
uniform sampler2D u_splattingMap2;

varying vec2 v_uv;
varying vec3 v_mask;

void main()
{
    vec3 diffuse = vec3(0.0);

    diffuse =
        texture2D(u_splattingMap0, v_uv).rgb * v_mask.x +
        texture2D(u_splattingMap1, v_uv).rgb * v_mask.y +
        texture2D(u_splattingMap2, v_uv).rgb * v_mask.z;

    diffuse /= v_mask.x + v_mask.y + v_mask.z;

    gl_FragColor = vec4(diffuse, 1.0);
}

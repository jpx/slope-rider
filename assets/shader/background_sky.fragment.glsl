#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D u_diffuseMap;

varying vec2 v_uv;

void main()
{
    vec3 diffuse = vec3(0.0);

    diffuse = texture2D(u_diffuseMap, v_uv).rgb;

    gl_FragColor = vec4(diffuse, 1.0);
}

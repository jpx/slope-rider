#ifdef GL_ES
precision mediump float;
#endif

uniform vec4 u_diffuseColor;
//uniform sampler2D u_tex0;

varying vec2 v_uv;
varying vec2 v_mask;

void main()
{
    vec3 diffuse = u_diffuseColor.rgb;

    diffuse = vec3(0.0, 0.0, 1.0) * v_mask.x + vec3(0.0, 1.0, 0.0) * v_mask.y;

    //vec4 color = texture2D(u_tex0, v_texCoord0);

    gl_FragColor = vec4(diffuse.rgb, 1.0);
}

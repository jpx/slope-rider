attribute vec2 a_position;
attribute vec2 a_uv;
attribute vec4 a_mask;

uniform mat4 u_modelToWorldMatrix;
uniform mat4 u_worldToScreenMatrix;

varying vec2 v_uv;
varying vec4 v_mask;

void main()
{
    v_uv = a_uv;
    v_mask = a_mask;

    vec4 position = vec4(a_position, 0.0, 1.0);

    gl_Position = u_worldToScreenMatrix * u_modelToWorldMatrix * position;
}

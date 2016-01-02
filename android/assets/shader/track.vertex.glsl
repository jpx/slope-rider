attribute vec2 a_position;
attribute vec2 a_normal;
attribute vec2 a_uv;
attribute vec4 a_mask0;
attribute vec4 a_mask1;

uniform mat4 u_modelToWorldMatrix;
uniform mat4 u_worldToScreenMatrix;

varying vec2 v_normal;
varying vec2 v_uv;
varying vec4 v_mask0;
varying vec4 v_mask1;

void main()
{
    v_normal = a_normal;
    v_uv = a_uv;
    v_mask0 = a_mask0;
    v_mask1 = a_mask1;

    vec4 position = vec4(a_position, 0.0, 1.0);

    gl_Position = u_worldToScreenMatrix * u_modelToWorldMatrix * position;
}

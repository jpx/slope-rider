attribute vec2 a_position;
attribute vec2 a_uv;

uniform mat4 u_modelToWorldMatrix;
uniform mat4 u_projectionMatrix;
uniform mat4 u_worldToScreenMatrix;
uniform float u_animationMask;

varying vec2 v_uv;

void main()
{
    v_uv = a_uv;

    vec4 position = vec4(a_position, 0.0, 1.0);

    position = u_modelToWorldMatrix * position;

    if (u_animationMask > 0.0)
    {
        position = u_projectionMatrix * position;
    }
    else
    {
        position = u_worldToScreenMatrix * position;
    }

    gl_Position = position;
}

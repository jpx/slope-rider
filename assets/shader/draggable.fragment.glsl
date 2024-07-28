#ifdef GL_ES
precision mediump float;
#endif

uniform vec2 u_size;
uniform vec2 u_position;
uniform vec2 u_anchorPosition;
uniform float u_limit;
uniform vec2 u_limitMask;

varying vec2 v_uv;

#define OUTLINE_SIZE 0.15

#define GRADUATION_SIZE 0.1

void main()
{
    vec4 diffuse = vec4(vec3(0.0), 0.25);

    float distance = distance(u_anchorPosition * u_limitMask, v_uv * u_limitMask);
    float distanceFromLimit = distance - u_limit;

    diffuse += mix(
        vec4(0.0, 0.7, 0.0, 0.0),
        vec4(1.0, 0.1, 0.1, 0.0),
        distanceFromLimit * 50.0
    );

    vec4 anchorGraduationDiffuse = vec4(0.0);

    if (v_uv.x > 0.6)
    {
        float graduationSize = 1.0 / u_size.y;

        if (mod(distance, graduationSize) < GRADUATION_SIZE / u_size.y)
        {
            if (distance < GRADUATION_SIZE / u_size.y)
                anchorGraduationDiffuse = vec4(0.4, 0.2, 1.0, 0.6);
            else
                diffuse = vec4(1.0, 1.0, 1.0, 0.4);
        }
    }

    if (v_uv.x < OUTLINE_SIZE / u_size.x || v_uv.x > 1.0 - OUTLINE_SIZE / u_size.x ||
        v_uv.y < OUTLINE_SIZE / u_size.y || v_uv.y > 1.0 - OUTLINE_SIZE / u_size.y)
    {
        diffuse = vec4(vec3(0.5), 0.4);
    }

    if (anchorGraduationDiffuse.a > 0.0)
        diffuse = anchorGraduationDiffuse;

    gl_FragColor = diffuse;
}

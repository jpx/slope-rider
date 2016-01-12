#ifdef GL_ES
precision mediump float;
#endif

uniform float u_time;
uniform vec4 u_diffuseColor;
uniform sampler2D u_diffuseMap;
uniform sampler2D u_maskMap;

uniform float u_cooldownAnimationMask;
uniform float u_cooldownAnimationDuration;
uniform float u_cooldownAnimationStartTime;

varying vec2 v_uv;

#define PI_2 1.570796

void main()
{
    vec4 diffuse = u_diffuseColor;

    vec4 mask = texture2D(u_maskMap, v_uv);

    if (mask.r <= 0.1 && mask.g <= 0.1)
        discard;

    if (mask.r > 0.1)
    {
        float scale = 0.5;
        vec2 uv = v_uv * (2.0 - scale) - vec2(0.5 * (1.0 - scale));
        vec4 diffuseMapSample = texture2D(u_diffuseMap, uv);

        if (uv.x >= 0.0 && uv.y >= 0.0 &&
            uv.x <= 1.0 && uv.y <= 1.0 &&
            diffuseMapSample.a > 0.1)
            diffuse = diffuseMapSample;

        diffuse *= mask.r;

        if (u_cooldownAnimationMask > 0.0)
        {
            vec2 v0 = normalize(vec2(0.0, -0.5));
            vec2 v1 = normalize(vec2(0.5, 0.5) - v_uv);

            float angle = atan(v0.y, v0.x) - atan(v1.y, v1.x);
            if (angle < 0.0)
                angle += 4.0 * PI_2;

            float rate = clamp((u_time - u_cooldownAnimationStartTime) / u_cooldownAnimationDuration, 0.0, 1.0);

            float currentAngle = 4.0 * PI_2 * rate;

            if (angle < currentAngle)
                diffuse = vec4(diffuse.rgb * 0.5, 0.6);
        }
    }
    else if (mask.g > 0.1)
    {
        diffuse = vec4(vec3(0.0), 0.6);
        diffuse *= mask.g;
    }

    gl_FragColor = diffuse;
}

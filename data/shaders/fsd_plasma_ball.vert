attribute vec2 a_position;
attribute vec2 a_texCoord;
attribute vec2 a_particlePos;
attribute float a_particleSize;
attribute vec4 a_particleColor;
attribute float a_particleTime;
attribute float a_particleType;

varying vec2 v_texCoord;
varying vec4 v_color;
varying float v_particleTime;
varying float v_particleType;

uniform float u_time;
uniform float u_useInstanceTime;

float rand(vec2 co) {
    return fract(sin(dot(co.xy, vec2(12.9898, 78.233))) * 43758.5453);
}

vec2 plasma_distortion(vec2 pos, vec2 offset, float particleSize, float time, float particleTime, float particleType) {
    float effectiveTime = u_useInstanceTime > 0.5 ? u_time * 0.2 + particleTime * 1.5 : u_time;
    
    if (particleType < 0.5) {
        float scale = 1.0 + sin(effectiveTime * 1.5) * 0.02;
        
        float angle = effectiveTime * 0.05;
        float c = cos(angle);
        float s = sin(angle);
        vec2 rotated = vec2(
            offset.x * c - offset.y * s,
            offset.x * s + offset.y * c
        );
        
        return pos + rotated * scale;
    } 
    else {
        return pos + offset;
    }
}

void main() {
    vec2 offset = a_position * a_particleSize;
    vec2 distortedPosition = plasma_distortion(a_particlePos, offset, a_particleSize, u_time, a_particleTime, a_particleType);
    gl_Position = gl_ModelViewProjectionMatrix * vec4(distortedPosition, 0.0, 1.0);
    v_texCoord = a_texCoord;
    v_color = a_particleColor;
    v_particleTime = a_particleTime;
    v_particleType = a_particleType;
}
// PC test: runs the overlay's real rasterizer with GL stubbed out and writes RGBA files.
#include <stdint.h>
#include <stdio.h>
typedef unsigned int GLuint; typedef int GLint; typedef unsigned int GLenum; typedef float GLfloat; typedef unsigned char GLboolean;
#define GL_VERTEX_SHADER 1
#define GL_FRAGMENT_SHADER 2
#define GL_COMPILE_STATUS 3
#define GL_LINK_STATUS 4
#define GL_TEXTURE3 5
#define GL_TEXTURE0 6
#define GL_TEXTURE_2D 7
#define GL_TEXTURE_MIN_FILTER 8
#define GL_TEXTURE_MAG_FILTER 9
#define GL_LINEAR 10
#define GL_TEXTURE_WRAP_S 11
#define GL_TEXTURE_WRAP_T 12
#define GL_CLAMP_TO_EDGE 13
#define GL_RGBA 14
#define GL_UNSIGNED_BYTE 15
#define GL_BLEND 16
#define GL_SRC_ALPHA 17
#define GL_ONE_MINUS_SRC_ALPHA 18
#define GL_FLOAT 19
#define GL_FALSE 0
#define GL_TRIANGLE_FAN 20
static void glActiveTexture(GLenum){} static void glBindTexture(GLenum, GLuint){}
static void glTexImage2D(GLenum, GLint, GLint, int, int, GLint, GLenum, GLenum, const void*){}
#define LOG(...) do{}while(0)
#define LOGE(...) do{}while(0)
static GLuint glCreateShader(GLenum){return 1;} static void glShaderSource(GLuint,int,const char**,const GLint*){}
static void glCompileShader(GLuint){} static void glGetShaderiv(GLuint,GLenum,GLint* v){*v=1;} static void glDeleteShader(GLuint){}
static GLuint glCreateProgram(){return 1;} static void glAttachShader(GLuint,GLuint){} static void glLinkProgram(GLuint){}
static void glGetProgramiv(GLuint,GLenum,GLint* v){*v=1;} static GLint glGetAttribLocation(GLuint,const char*){return 0;}
static GLint glGetUniformLocation(GLuint,const char*){return 0;} static void glGenTextures(int,GLuint* t){*t=1;}
static void glTexParameteri(GLenum,GLenum,GLint){} static void glUseProgram(GLuint){} static void glUniform1i(GLint,GLint){}
static void glEnable(GLenum){} static void glDisable(GLenum){} static void glBlendFunc(GLenum,GLenum){}
static void glVertexAttribPointer(GLint,int,GLenum,GLboolean,int,const void*){} static void glEnableVertexAttribArray(GLint){}
static void glDisableVertexAttribArray(GLint){} static void glDrawArrays(GLenum,int,int){}
#include "../player/speed_overlay.h"
int main() {
    int vals[] = {0, 52, 128};
    for (int i = 0; i < 3; i++) {
        so_rasterize(vals[i]);
        char n[64]; snprintf(n, sizeof n, "/out/raster_%d_%d.rgba", vals[i], so_box_w);
        FILE* f = fopen(n, "wb"); fwrite(so_px, 1, sizeof so_px, f); fclose(f);
        printf("%d -> box width %d\n", vals[i], so_box_w);
    }
    return 0;
}

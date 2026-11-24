/*
 * Speed overlay for the mib-cluster-map stream player (our addition, GPL-3.0 like the player).
 *
 * Draws a small "52 km/h" box over the phone map in the cockpit. The speed comes
 * from VehLog (Java HMI, read-only), which writes the speedometer value to
 * /tmp/vehlog_speed ("52\n") every 2 s and on every change.
 *
 * Safety: everything here only draws. No file -> nothing drawn. A value older than
 * SO_STALE_S seconds -> nothing drawn. /tmp/speedoverlay-off -> nothing drawn.
 * If the shader does not compile, the overlay switches itself off for good and the
 * player draws the map exactly as before.
 *
 * Position: bottom left by default, SO_LEFT_DEFAULT pixels from the left edge (on the
 * A6/A7 C7 the rev counter covers about the left 180 of 800 pixels) and
 * SO_BOTTOM_DEFAULT pixels above the bottom edge. /mnt/persist/speedoverlay.conf can
 * change it (read when the player starts):
 *   pos=bl|br|tl|tr   left=<px>   right=<px>   top=<px>   bottom=<px>
 */
#pragma once
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <sys/stat.h>
#include "speed_font.h"

#define SO_FILE "/tmp/vehlog_speed"
#define SO_OFF "/tmp/speedoverlay-off"
#define SO_STALE_S 6
#define SO_TEX_W 176
#define SO_TEX_H 60
#define SO_MARGIN 12
#define SO_BOTTOM_DEFAULT 12
#define SO_LEFT_DEFAULT 200
#define SO_CONF "/mnt/persist/speedoverlay.conf"

static int so_state = 0;            /* 0 = not set up, 1 = ready, -1 = off for good */
static GLuint so_prog, so_tex;
static GLint so_pos, so_uv, so_smp;
static int so_speed = -1;           /* value in the texture, -1 = none */
static int so_box_w = 0;            /* used width of the texture */
static uint64_t so_last_check = 0;
static bool so_visible = false;
static int so_corner = 2;            /* 0 top left, 1 top right, 2 bottom left, 3 bottom right */
static int so_bottom = SO_BOTTOM_DEFAULT;
static int so_leftpx = SO_LEFT_DEFAULT;
static int so_rightpx = SO_MARGIN;
static int so_toppx = SO_MARGIN;
static bool so_logged = false;
static uint8_t so_px[SO_TEX_W * SO_TEX_H * 4];

static const char* so_vs =
    "attribute vec2 p; attribute vec2 t; varying vec2 v;"
    "void main(){ gl_Position = vec4(p, 0.0, 1.0); v = t; }";
static const char* so_fs =
    "precision mediump float; varying vec2 v; uniform sampler2D s;"
    "void main(){ gl_FragColor = texture2D(s, v); }";

static GLuint so_shader(GLenum type, const char* src) {
    GLuint s = glCreateShader(type);
    glShaderSource(s, 1, &src, NULL);
    glCompileShader(s);
    GLint ok = 0;
    glGetShaderiv(s, GL_COMPILE_STATUS, &ok);
    if (!ok) { glDeleteShader(s); return 0; }
    return s;
}

static void so_init() {
    so_state = -1;
    FILE* cf = fopen(SO_CONF, "r");
    if (cf) {
        char line[64];
        while (fgets(line, sizeof(line), cf)) {
            if (strncmp(line, "pos=tl", 6) == 0) so_corner = 0;
            else if (strncmp(line, "pos=tr", 6) == 0) so_corner = 1;
            else if (strncmp(line, "pos=bl", 6) == 0) so_corner = 2;
            else if (strncmp(line, "pos=br", 6) == 0) so_corner = 3;
            else if (strncmp(line, "bottom=", 7) == 0) {
                int b = atoi(line + 7);
                if (b >= 0 && b <= 400) so_bottom = b;
            }
            else if (strncmp(line, "left=", 5) == 0) {
                int b = atoi(line + 5);
                if (b >= 0 && b <= 700) so_leftpx = b;
            }
            else if (strncmp(line, "right=", 6) == 0) {
                int b = atoi(line + 6);
                if (b >= 0 && b <= 700) so_rightpx = b;
            }
            else if (strncmp(line, "top=", 4) == 0) {
                int b = atoi(line + 4);
                if (b >= 0 && b <= 400) so_toppx = b;
            }
        }
        fclose(cf);
    }
    GLuint vs = so_shader(GL_VERTEX_SHADER, so_vs);
    GLuint fs = so_shader(GL_FRAGMENT_SHADER, so_fs);
    if (!vs || !fs) { LOGE("speed overlay: shader compile failed, overlay off"); return; }
    so_prog = glCreateProgram();
    glAttachShader(so_prog, vs);
    glAttachShader(so_prog, fs);
    glLinkProgram(so_prog);
    GLint ok = 0;
    glGetProgramiv(so_prog, GL_LINK_STATUS, &ok);
    if (!ok) { LOGE("speed overlay: shader link failed, overlay off"); return; }
    so_pos = glGetAttribLocation(so_prog, "p");
    so_uv = glGetAttribLocation(so_prog, "t");
    so_smp = glGetUniformLocation(so_prog, "s");
    glGenTextures(1, &so_tex);
    glActiveTexture(GL_TEXTURE3);
    glBindTexture(GL_TEXTURE_2D, so_tex);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glActiveTexture(GL_TEXTURE0);
    so_state = 1;
    LOG("speed overlay: ready (corner %d, left %d, right %d, top %d, bottom %d)", so_corner, so_leftpx, so_rightpx, so_toppx, so_bottom);
}

/* Reads the speed file; -1 when missing, stale, switched off or not a number. */
static int so_read_speed() {
    struct stat st;
    if (stat(SO_OFF, &st) == 0) return -1;
    if (stat(SO_FILE, &st) != 0) return -1;
    time_t now = time(NULL);
    if (now - st.st_mtime > SO_STALE_S || st.st_mtime - now > SO_STALE_S) return -1;
    FILE* f = fopen(SO_FILE, "r");
    if (!f) return -1;
    char buf[16] = {0};
    size_t n = fread(buf, 1, sizeof(buf) - 1, f);
    fclose(f);
    if (n == 0) return -1;
    char* end = NULL;
    long v = strtol(buf, &end, 10);
    if (end == buf || v < 0 || v > 400) return -1;
    return (int)v;
}

/* Box with rounded corners, white digits, grey "km/h", into so_px (straight alpha). */
static void so_rasterize(int speed) {
    char digits[8];
    snprintf(digits, sizeof(digits), "%d", speed);
    int nd = (int)strlen(digits);
    int text_w = 0;
    for (int i = 0; i < nd; i++) text_w += so_dig_w[digits[i] - '0'] - 4;
    int pad = 12, gap = 6;
    so_box_w = pad + text_w + gap + SO_UNIT_W + pad;
    if (so_box_w > SO_TEX_W) so_box_w = SO_TEX_W;
    memset(so_px, 0, sizeof(so_px));
    const int r = 10;
    for (int y = 0; y < SO_TEX_H; y++) {
        for (int x = 0; x < so_box_w; x++) {
            int dx = x < r ? r - x : (x >= so_box_w - r ? x - (so_box_w - r - 1) : 0);
            int dy = y < r ? r - y : (y >= SO_TEX_H - r ? y - (SO_TEX_H - r - 1) : 0);
            if (dx * dx + dy * dy > r * r) continue;
            uint8_t* p = &so_px[(y * SO_TEX_W + x) * 4];
            p[0] = p[1] = p[2] = 0; p[3] = 160;
        }
    }
    int top = (SO_TEX_H - SO_DIG_H) / 2 + 2;
    int x0 = pad - 2;
    for (int i = 0; i < nd; i++) {
        int d = digits[i] - '0';
        const uint8_t* g = &so_dig_px[so_dig_off[d]];
        int gw = so_dig_w[d];
        for (int y = 0; y < SO_DIG_H; y++) {
            int ty = top + y;
            if (ty < 0 || ty >= SO_TEX_H) continue;
            for (int x = 0; x < gw; x++) {
                int tx = x0 + x;
                if (tx < 0 || tx >= so_box_w) continue;
                int a = g[y * gw + x];
                if (!a) continue;
                uint8_t* p = &so_px[(ty * SO_TEX_W + tx) * 4];
                int ba = p[3];
                int oa = a + ba * (255 - a) / 255;
                p[0] = p[1] = p[2] = (uint8_t)(oa ? 255 * a / oa : 0);
                p[3] = (uint8_t)oa;
            }
        }
        x0 += gw - 4;
    }
    int ux = x0 + gap, uy = top + SO_DIG_BASE - SO_UNIT_BASE;
    for (int y = 0; y < SO_UNIT_H; y++) {
        int ty = uy + y;
        if (ty < 0 || ty >= SO_TEX_H) continue;
        for (int x = 0; x < SO_UNIT_W; x++) {
            int tx = ux + x;
            if (tx < 0 || tx >= so_box_w) continue;
            int a = so_unit_px[y * SO_UNIT_W + x];
            if (!a) continue;
            uint8_t* p = &so_px[(ty * SO_TEX_W + tx) * 4];
            int ba = p[3];
            int oa = a + ba * (255 - a) / 255;
            p[0] = p[1] = p[2] = (uint8_t)(oa ? 180 * a / oa : 0);
            p[3] = (uint8_t)oa;
        }
    }
    glActiveTexture(GL_TEXTURE3);
    glBindTexture(GL_TEXTURE_2D, so_tex);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, SO_TEX_W, SO_TEX_H, 0, GL_RGBA, GL_UNSIGNED_BYTE, so_px);
    glActiveTexture(GL_TEXTURE0);
}

/* Called once per drawn frame, after the map and before eglSwapBuffers. */
static void speed_overlay_draw(int winW, int winH, uint64_t nowUs) {
    if (so_state == 0) so_init();
    if (so_state != 1 || winW <= 0 || winH <= 0) return;
    if (nowUs - so_last_check >= 300000ULL || so_last_check == 0) {
        so_last_check = nowUs;
        int v = so_read_speed();
        so_visible = v >= 0;
        if (so_visible && v != so_speed) {
            so_speed = v;
            so_rasterize(v);
        }
    }
    if (!so_visible || so_speed < 0) return;

    if (!so_logged) {
        so_logged = true;
        LOG("speed overlay: window %dx%d, first value %d km/h", winW, winH, so_speed);
    }
    bool left = so_corner == 0 || so_corner == 2;
    bool bottom = so_corner >= 2;
    float x0 = left ? (float)so_leftpx : (float)(winW - so_rightpx - so_box_w);
    float y0 = bottom ? (float)(winH - so_bottom - SO_TEX_H) : (float)so_toppx;
    if (x0 < 0.0f) x0 = 0.0f;
    if (x0 > (float)(winW - so_box_w)) x0 = (float)(winW - so_box_w);
    if (y0 < 0.0f) y0 = 0.0f;
    if (y0 > (float)(winH - SO_TEX_H)) y0 = (float)(winH - SO_TEX_H);
    float x1 = x0 + so_box_w, y1 = y0 + SO_TEX_H;
    float l = x0 / winW * 2.0f - 1.0f, rgt = x1 / winW * 2.0f - 1.0f;
    float t = 1.0f - y0 / winH * 2.0f, b = 1.0f - y1 / winH * 2.0f;
    float u1 = (float)so_box_w / SO_TEX_W;
    GLfloat pos[8] = { l, t,  rgt, t,  rgt, b,  l, b };
    GLfloat uv[8] = { 0.0f, 0.0f,  u1, 0.0f,  u1, 1.0f,  0.0f, 1.0f };

    glUseProgram(so_prog);
    glActiveTexture(GL_TEXTURE3);
    glBindTexture(GL_TEXTURE_2D, so_tex);
    glUniform1i(so_smp, 3);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glVertexAttribPointer(so_pos, 2, GL_FLOAT, GL_FALSE, 0, pos);
    glVertexAttribPointer(so_uv, 2, GL_FLOAT, GL_FALSE, 0, uv);
    glEnableVertexAttribArray(so_pos);
    glEnableVertexAttribArray(so_uv);
    glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
    glDisableVertexAttribArray(so_pos);
    glDisableVertexAttribArray(so_uv);
    glDisable(GL_BLEND);
    glActiveTexture(GL_TEXTURE0);
}

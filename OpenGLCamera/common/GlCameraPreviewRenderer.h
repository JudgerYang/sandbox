#pragma once
#ifndef __GL_CAMERA_PREVIEW_RENDERER_H__
#define __GL_CAMERA_PREVIEW_RENDERER_H__

int on_surface_created();
void on_surface_changed(int width, int height);
void on_draw_frame();
void clear();

#endif//__GL_CAMERA_PREVIEW_RENDERER_H__

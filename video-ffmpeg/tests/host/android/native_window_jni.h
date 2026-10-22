#pragma once
#include "native_window.h"
#include <jni.h>
inline ANativeWindow* ANativeWindow_fromSurface(JNIEnv*, jobject) { return nullptr; }

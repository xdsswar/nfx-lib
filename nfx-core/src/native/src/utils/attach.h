/*
 * Copyright © 2025. XTREME SOFTWARE SOLUTIONS
 *
 * All rights reserved. Unauthorized use, reproduction, or distribution
 * of this software or any portion of it is strictly prohibited and may
 * result in severe civil and criminal penalties. This code is the sole
 * proprietary of XTREME SOFTWARE SOLUTIONS.
 *
 * Commercialization, redistribution, and use without explicit permission
 * from XTREME SOFTWARE SOLUTIONS, are expressly forbidden.
 */

/**
  * @author XDSSWAR
  * Created on 08/13/2025
  */
#pragma once


#ifndef NFX_CORE_WIN64_ATTACH_H
#define NFX_CORE_WIN64_ATTACH_H

#include <jni.h>

class JniAttachGuard {
public:
    explicit JniAttachGuard(JavaVM* jvm) : jvm_(jvm) {
        JNIEnv* e = nullptr;
        const jint s = jvm_->GetEnv(reinterpret_cast<void**>(&e), JNI_VERSION_1_8);
        if (s == JNI_OK) {
            env_ = e;               // already attached by JVM; do NOT detach later
        } else if (s == JNI_EDETACHED) {
            if (jvm_->AttachCurrentThread(reinterpret_cast<void**>(&e), nullptr) == JNI_OK) {
                env_ = e;
                attached_here_ = true; // we'll detach in dtor
            }
        } // else JNI_EVERSION -> env_ stays null
    }

    ~JniAttachGuard() {
        if (attached_here_) jvm_->DetachCurrentThread();
    }

    [[nodiscard]] JNIEnv* env() const { return env_; }

private:
    JavaVM* jvm_{};
    JNIEnv* env_{};
    bool attached_here_{false};
};
#endif //NFX_CORE_WIN64_ATTACH_H
#include <jni.h>
#include <jnipp.h>

#include <cmath>

#include "testing.h"

/*
    jni::Vm Tests
 */
TEST(Vm_externalDetach) {
    jni::Vm vm;

    jni::Class cls("java/lang/String");

    JNIEnv *env = (JNIEnv *)jni::env();
    JavaVM *localVmPointer{};

    auto ret = env->GetJavaVM(&localVmPointer);
    ASSERT(ret == 0);
    ret = localVmPointer->DetachCurrentThread();
    ASSERT(ret == 0);

    ASSERT(1);
}

int main() {
    RUN_TEST(Vm_externalDetach);
    return 0;
}

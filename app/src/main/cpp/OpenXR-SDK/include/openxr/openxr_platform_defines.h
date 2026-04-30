/*
** Copyright (c) 2017-2024, The Khronos Group Inc.
**
** SPDX-License-Identifier: Apache-2.0 OR MIT
*/

#ifndef OPENXR_PLATFORM_DEFINES_H_
#define OPENXR_PLATFORM_DEFINES_H_ 1

#ifdef __cplusplus
extern "C" {
#endif

/* Platform-specific calling convention macros.
 *
 * Platforms should define these so that OpenXR clients call OpenXR functions
 * with the same calling conventions that the OpenXR implementation expects.
 *
 * XRAPI_ATTR - Placed before the return type in function declarations.
 *              Useful for C++11 and GCC/Clang-style function attribute syntax.
 * XRAPI_CALL - Placed after the return type in function declarations.
 *              Useful for MSVC-style calling convention syntax.
 * XRAPI_PTR  - Placed between the '(' and '*' in function pointer types.
 *
 * Function declaration:  XRAPI_ATTR void XRAPI_CALL xrFunction(void);
 * Function pointer type: typedef void (XRAPI_PTR *PFN_xrFunction)(void);
 */
#if defined(_WIN32)
#define XRAPI_ATTR
#define XRAPI_CALL __stdcall
#define XRAPI_PTR XRAPI_CALL
#elif defined(__ANDROID__) && defined(__ARM_ARCH) && __ARM_ARCH < 7
#error "API not supported for the 'armeabi' NDK ABI"
#elif defined(__ANDROID__) && defined(__ARM_ARCH) && __ARM_ARCH >= 7 && defined(__ARM_32BIT_STATE)
#define XRAPI_ATTR __attribute__((pcs("aapcs-vfp")))
#define XRAPI_CALL
#define XRAPI_PTR XRAPI_ATTR
#else
#define XRAPI_ATTR
#define XRAPI_CALL
#define XRAPI_PTR
#endif

#include <stddef.h>

#if !defined(XR_NO_STDINT_H)
#if defined(_MSC_VER) && (_MSC_VER < 1600)
typedef signed __int8 int8_t;
typedef unsigned __int8 uint8_t;
typedef signed __int16 int16_t;
typedef unsigned __int16 uint16_t;
typedef signed __int32 int32_t;
typedef unsigned __int32 uint32_t;
typedef signed __int64 int64_t;
typedef unsigned __int64 uint64_t;
#else
#include <stdint.h>
#endif
#endif  // !defined( XR_NO_STDINT_H )

#if (defined(__LP64__) || defined(_WIN64) || (defined(__x86_64__) && !defined(__ILP32__) ) || defined(_M_X64) || defined(__ia64) || defined(_M_IA64) || defined(__aarch64__) || defined(__powerpc64__))
#define XR_PTR_SIZE 8
#else
#define XR_PTR_SIZE 4
#endif

#if !defined(XR_COMPILER_HAS_FEATURE)
#if defined(__clang__)
#define XR_COMPILER_HAS_FEATURE(x) __has_feature(x)
#else
#define XR_COMPILER_HAS_FEATURE(x) 0
#endif
#endif

#if !defined(XR_CPP11_ENABLED) && defined(__cplusplus)
#if defined(__GNUC__) && defined(__GXX_EXPERIMENTAL_CXX0X__)
#define XR_CPP11_ENABLED 1
#elif defined(_MSC_VER) && (_MSC_VER >= 1600)
#define XR_CPP11_ENABLED 1
#elif (__cplusplus >= 201103L) // 201103 is the first C++11 version.
#define XR_CPP11_ENABLED 1
#endif
#endif

#if !defined(XR_CPP_NULLPTR_SUPPORTED)
#if defined(XR_CPP11_ENABLED) &&                                              \
    ((defined(__clang__) && XR_COMPILER_HAS_FEATURE(cxx_nullptr)) ||          \
     (defined(__GNUC__) && (((__GNUC__ * 1000) + __GNUC_MINOR__) >= 4006)) || \
     (defined(_MSC_VER) && (_MSC_VER >= 1600)) ||                             \
     (defined(__EDG_VERSION__) && (__EDG_VERSION__ >= 403)))
#define XR_CPP_NULLPTR_SUPPORTED 1
#endif
#endif

#if !defined(XR_CPP_NULLPTR_SUPPORTED)
#define XR_CPP_NULLPTR_SUPPORTED 0
#endif  // !defined(XR_CPP_NULLPTR_SUPPORTED)

#ifdef __cplusplus
}
#endif

#endif

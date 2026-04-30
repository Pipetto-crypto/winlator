
#ifndef _STDFS_CONDITIONS_H
#define _STDFS_CONDITIONS_H

#if __cplusplus >= 201703L
#define USE_EXPERIMENTAL_FS 0
#define USE_FINAL_FS 1

#elif defined(_MSC_VER) && _MSC_VER >= 1900

#if defined(_HAS_CXX17) && _HAS_CXX17
#define USE_EXPERIMENTAL_FS 0
#define USE_FINAL_FS 1
#endif  // !_HAS_CXX17

#elif (__GNUC__ >= 6)
#define USE_EXPERIMENTAL_FS 1
#define USE_FINAL_FS 0

#elif defined(__clang__) && (__cpp_lib_filesystem || __cpp_lib_experimental_filesystem)
#if __cpp_lib_filesystem
#define USE_EXPERIMENTAL_FS 0
#define USE_FINAL_FS 1
#else
#define USE_EXPERIMENTAL_FS 1
#define USE_FINAL_FS 0
#endif

#else
#define USE_EXPERIMENTAL_FS 0
#define USE_FINAL_FS 0
#endif

#endif  // !_STDFS_CONDITIONS_H

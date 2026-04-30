
#pragma once

#include "loader_logger.hpp"

#include <openxr/openxr.h>

#include <memory>

std::unique_ptr<LoaderLogRecorder> MakeStdErrLoaderLogRecorder(void* user_data);

std::unique_ptr<LoaderLogRecorder> MakeStdOutLoaderLogRecorder(void* user_data, XrLoaderLogMessageSeverityFlags flags);

#ifdef __ANDROID__
std::unique_ptr<LoaderLogRecorder> MakeLogcatLoaderLogRecorder();
#endif

std::unique_ptr<LoaderLogRecorder> MakeDebugUtilsLoaderLogRecorder(const XrDebugUtilsMessengerCreateInfoEXT* create_info,
                                                                   XrDebugUtilsMessengerEXT debug_messenger);

#ifdef _WIN32
std::unique_ptr<LoaderLogRecorder> MakeDebuggerLoaderLogRecorder(void* user_data);
#endif


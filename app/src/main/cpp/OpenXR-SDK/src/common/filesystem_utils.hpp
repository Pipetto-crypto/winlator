
#pragma once

#include <string>
#include <vector>

bool FileSysUtilsIsRegularFile(const std::string& path);

bool FileSysUtilsIsDirectory(const std::string& path);

bool FileSysUtilsPathExists(const std::string& path);

bool FileSysUtilsGetCurrentPath(std::string& path);

bool FileSysUtilsGetParentPath(const std::string& file_path, std::string& parent_path);

bool FileSysUtilsIsAbsolutePath(const std::string& path);

bool FileSysUtilsGetAbsolutePath(const std::string& path, std::string& absolute);

bool FileSysUtilsGetCanonicalPath(const std::string& path, std::string& canonical);

bool FileSysUtilsCombinePaths(const std::string& parent, const std::string& child, std::string& combined);

bool FileSysUtilsParsePathList(std::string& path_list, std::vector<std::string>& paths);

bool FileSysUtilsFindFilesInPath(const std::string& path, std::vector<std::string>& files);

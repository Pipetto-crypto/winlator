hook_impl_files = files(['hook_impl.cpp', 'hook_impl.h', 'hook_impl_params.h'])
hook_impl_inc = include_directories('../../include')

# hook_impl_lib = shared_library('hook_impl', 'hook_impl.cpp', 'hook_impl.h', 'hook_impl_params.h')
# target_compile_options(['hook_impl', 'PRIVATE', '-Wall', '-Wextra'])
# target_link_libraries(['hook_impl', 'linkernsbypass', 'log'])
# target_include_directories(['hook_impl', 'PRIVATE', '../../include'])
# set_target_properties(['hook_impl', 'PROPERTIES', 'CXX_VISIBILITY_PRESET', 'hidden'])

main_hook_files = files(['main_hook.c'])
# main_hook_lib = shared_library('main_hook', 'main_hook.c')
# target_compile_options(['main_hook', 'PRIVATE', '-Wall', '-Wextra'])
# target_link_options(['main_hook', 'PRIVATE', '-z', 'global'])
# target_link_libraries(['main_hook', 'hook_impl'])
# set_target_properties(['main_hook', 'PROPERTIES', 'CXX_VISIBILITY_PRESET', 'hidden'])

file_redirect_hook_files = files(['file_redirect_hook.c'])
# file_redirect_hook_lib = shared_library('file_redirect_hook', 'file_redirect_hook.c')
# target_compile_options(['file_redirect_hook', 'PRIVATE', '-Wall', '-Wextra'])
# target_link_options(['file_redirect_hook', 'PRIVATE', '-z', 'global'])
# target_link_libraries(['file_redirect_hook', 'hook_impl'])
# set_target_properties(['file_redirect_hook', 'PROPERTIES', 'CXX_VISIBILITY_PRESET', 'hidden'])

gsl_alloc_hook_files = files(['gsl_alloc_hook.c'])
# gsl_alloc_hook_lib = shared_library('gsl_alloc_hook', 'gsl_alloc_hook.c')
# target_compile_options(['gsl_alloc_hook', 'PRIVATE', '-Wall', '-Wextra'])
# target_link_options(['gsl_alloc_hook', 'PRIVATE', '-z', 'global'])
# target_link_libraries(['gsl_alloc_hook', 'hook_impl'])
# set_target_properties(['gsl_alloc_hook', 'PROPERTIES', 'CXX_VISIBILITY_PRESET', 'hidden'])

hook_impl_lib = shared_library(
    'hook_impl',
    hook_impl_files,
    c_args: (['-fvisibility=hidden',  '-Wall', '-Wextra']),
    cpp_args: (['-fvisibility=hidden',  '-Wall', '-Wextra']),
    dependencies: ([linkernsbypass_dep, liblog_dep]),
    include_directories: ([hook_impl_inc]),
    install: true
)

hook_impl_dep = declare_dependency(
    link_args: (['-z', 'global']),
    link_with: ([hook_impl_lib]),
    include_directories: hook_impl_inc,
)

main_hook_lib = shared_library(
    'main_hook',
    main_hook_files,
    c_args: (['-fvisibility=hidden',  '-Wall', '-Wextra']),
    cpp_args: (['-fvisibility=hidden',  '-Wall', '-Wextra']),
    dependencies: ([hook_impl_dep]),
    install: true
)

file_redirect_hook_lib = shared_library(
    'file_redirect_hook',
    file_redirect_hook_files,
    c_args: (['-fvisibility=hidden',  '-Wall', '-Wextra']),
    cpp_args: (['-fvisibility=hidden',  '-Wall', '-Wextra']),
    dependencies: ([hook_impl_dep]),
    install: true
)

gsl_alloc_hook_lib = shared_library(
    'gsl_alloc_hook',
    gsl_alloc_hook_files,
    c_args: (['-fvisibility=hidden',  '-Wall', '-Wextra']),
    cpp_args: (['-fvisibility=hidden',  '-Wall', '-Wextra']),
    dependencies: ([hook_impl_dep]),
    install: true
)
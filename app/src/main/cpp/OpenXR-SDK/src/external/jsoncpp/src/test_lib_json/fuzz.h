
#ifndef FUZZ_H_INCLUDED
#define FUZZ_H_INCLUDED

#include <cstddef>
#include <stdint.h>

extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size);

#endif // ifndef FUZZ_H_INCLUDED

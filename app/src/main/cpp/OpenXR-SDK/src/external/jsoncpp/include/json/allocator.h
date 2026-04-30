
#ifndef JSON_ALLOCATOR_H_INCLUDED
#define JSON_ALLOCATOR_H_INCLUDED

#include <cstring>
#include <memory>

#pragma pack(push, 8)

namespace Json {
template <typename T> class SecureAllocator {
public:
  using value_type = T;
  using pointer = T*;
  using const_pointer = const T*;
  using reference = T&;
  using const_reference = const T&;
  using size_type = std::size_t;
  using difference_type = std::ptrdiff_t;

  /**
   * Allocate memory for N items using the standard allocator.
   */
  pointer allocate(size_type n) {
    return static_cast<pointer>(::operator new(n * sizeof(T)));
  }

  /**
   * Release memory which was allocated for N items at pointer P.
   *
   * The memory block is filled with zeroes before being released.
   */
  void deallocate(pointer p, size_type n) {
    memset_s(p, n * sizeof(T), 0, n * sizeof(T));
    ::operator delete(p);
  }

  /**
   * Construct an item in-place at pointer P.
   */
  template <typename... Args> void construct(pointer p, Args&&... args) {
    ::new (static_cast<void*>(p)) T(std::forward<Args>(args)...);
  }

  size_type max_size() const { return size_t(-1) / sizeof(T); }

  pointer address(reference x) const { return std::addressof(x); }

  const_pointer address(const_reference x) const { return std::addressof(x); }

  /**
   * Destroy an item in-place at pointer P.
   */
  void destroy(pointer p) {
    p->~T();
  }

  SecureAllocator() {}
  template <typename U> SecureAllocator(const SecureAllocator<U>&) {}
  template <typename U> struct rebind { using other = SecureAllocator<U>; };
};

template <typename T, typename U>
bool operator==(const SecureAllocator<T>&, const SecureAllocator<U>&) {
  return true;
}

template <typename T, typename U>
bool operator!=(const SecureAllocator<T>&, const SecureAllocator<U>&) {
  return false;
}

} // namespace Json

#pragma pack(pop)

#endif // JSON_ALLOCATOR_H_INCLUDED

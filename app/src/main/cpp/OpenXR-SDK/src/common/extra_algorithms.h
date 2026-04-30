
/*!
 * @file
 *
 * Additional functions along the lines of the standard library algorithms.
 */

#pragma once

#include <algorithm>
#include <vector>

template <typename T, typename Pred>
static inline void map_erase_if(T &container, Pred &&predicate) {
    for (auto it = container.begin(); it != container.end();) {
        if (predicate(*it)) {
            it = container.erase(it);
        } else {
            ++it;
        }
    }
}

/*!
 * Moves all elements matching the predicate to the end of the vector then erases them.
 *
 * Combines the two parts of the erase-remove idiom to simplify things and avoid accidentally using the wrong erase overload.
 */
template <typename T, typename Alloc, typename Pred>
static inline void vector_remove_if_and_erase(std::vector<T, Alloc> &vec, Pred &&predicate) {
    auto b = vec.begin();
    auto e = vec.end();
    vec.erase(std::remove_if(b, e, std::forward<Pred>(predicate)), e);
}


#ifndef JSON_H_INCLUDED
#define JSON_H_INCLUDED

#if !defined(JSON_IS_AMALGAMATION)
#include "forwards.h"
#endif // if !defined(JSON_IS_AMALGAMATION)

#if !defined(JSONCPP_NORETURN)
#if defined(_MSC_VER) && _MSC_VER == 1800
#define JSONCPP_NORETURN __declspec(noreturn)
#else
#define JSONCPP_NORETURN [[noreturn]]
#endif
#endif

#if !defined(JSONCPP_TEMPLATE_DELETE)
#if defined(__clang__) && defined(__apple_build_version__)
#if __apple_build_version__ <= 8000042
#define JSONCPP_TEMPLATE_DELETE
#endif
#elif defined(__clang__)
#if __clang_major__ == 3 && __clang_minor__ <= 8
#define JSONCPP_TEMPLATE_DELETE
#endif
#endif
#if !defined(JSONCPP_TEMPLATE_DELETE)
#define JSONCPP_TEMPLATE_DELETE = delete
#endif
#endif

#include <array>
#include <exception>
#include <map>
#include <memory>
#include <string>
#include <vector>

#if defined(JSONCPP_DISABLE_DLL_INTERFACE_WARNING)
#pragma warning(push)
#pragma warning(disable : 4251 4275)
#endif // if defined(JSONCPP_DISABLE_DLL_INTERFACE_WARNING)

#pragma pack(push, 8)

/** \brief JSON (JavaScript Object Notation).
 */
namespace Json {

#if JSON_USE_EXCEPTION
/** Base class for all exceptions we throw.
 *
 * We use nothing but these internally. Of course, STL can throw others.
 */
class JSON_API Exception : public std::exception {
public:
  Exception(String msg);
  ~Exception() noexcept override;
  char const* what() const noexcept override;

protected:
  String msg_;
};

/** Exceptions which the user cannot easily avoid.
 *
 * E.g. out-of-memory (when we use malloc), stack-overflow, malicious input
 *
 * \remark derived from Json::Exception
 */
class JSON_API RuntimeError : public Exception {
public:
  RuntimeError(String const& msg);
};

/** Exceptions thrown by JSON_ASSERT/JSON_FAIL macros.
 *
 * These are precondition-violations (user bugs) and internal errors (our bugs).
 *
 * \remark derived from Json::Exception
 */
class JSON_API LogicError : public Exception {
public:
  LogicError(String const& msg);
};
#endif

JSONCPP_NORETURN void throwRuntimeError(String const& msg);
JSONCPP_NORETURN void throwLogicError(String const& msg);

/** \brief Type of the value held by a Value object.
 */
enum ValueType {
  nullValue = 0, ///< 'null' value
  intValue,      ///< signed integer value
  uintValue,     ///< unsigned integer value
  realValue,     ///< double value
  stringValue,   ///< UTF-8 string value
  booleanValue,  ///< bool value
  arrayValue,    ///< array value (ordered list)
  objectValue    ///< object value (collection of name/value pairs).
};

enum CommentPlacement {
  commentBefore = 0,      ///< a comment placed on the line before a value
  commentAfterOnSameLine, ///< a comment just after a value on the same line
  commentAfter, ///< a comment on the line after a value (only make sense for
  numberOfCommentPlacement
};

/** \brief Type of precision for formatting of real values.
 */
enum PrecisionType {
  significantDigits = 0, ///< we set max number of significant digits in string
  decimalPlaces          ///< we set max number of digits after "." in string
};

/** \brief Lightweight wrapper to tag static string.
 *
 * Value constructor and objectValue member assignment takes advantage of the
 * StaticString and avoid the cost of string duplication when storing the
 * string or the member name.
 *
 * Example of usage:
 * \code
 * Json::Value aValue( StaticString("some text") );
 * Json::Value object;
 * static const StaticString code("code");
 * object[code] = 1234;
 * \endcode
 */
class JSON_API StaticString {
public:
  explicit StaticString(const char* czstring) : c_str_(czstring) {}

  operator const char*() const { return c_str_; }

  const char* c_str() const { return c_str_; }

private:
  const char* c_str_;
};

/** \brief Represents a <a HREF="http://www.json.org">JSON</a> value.
 *
 * This class is a discriminated union wrapper that can represents a:
 * - signed integer [range: Value::minInt - Value::maxInt]
 * - unsigned integer (range: 0 - Value::maxUInt)
 * - double
 * - UTF-8 string
 * - boolean
 * - 'null'
 * - an ordered list of Value
 * - collection of name/value pairs (javascript object)
 *
 * The type of the held value is represented by a #ValueType and
 * can be obtained using type().
 *
 * Values of an #objectValue or #arrayValue can be accessed using operator[]()
 * methods.
 * Non-const methods will automatically create the a #nullValue element
 * if it does not exist.
 * The sequence of an #arrayValue will be automatically resized and initialized
 * with #nullValue. resize() can be used to enlarge or truncate an #arrayValue.
 *
 * The get() methods can be used to obtain default value in the case the
 * required element does not exist.
 *
 * It is possible to iterate over the list of member keys of an object using
 * the getMemberNames() method.
 *
 * \note #Value string-length fit in size_t, but keys must be < 2^30.
 * (The reason is an implementation detail.) A #CharReader will raise an
 * exception if a bound is exceeded to avoid security holes in your app,
 * but the Value API does *not* check bounds. That is the responsibility
 * of the caller.
 */
class JSON_API Value {
  friend class ValueIteratorBase;

public:
  using Members = std::vector<String>;
  using iterator = ValueIterator;
  using const_iterator = ValueConstIterator;
  using UInt = Json::UInt;
  using Int = Json::Int;
#if defined(JSON_HAS_INT64)
  using UInt64 = Json::UInt64;
  using Int64 = Json::Int64;
#endif // defined(JSON_HAS_INT64)
  using LargestInt = Json::LargestInt;
  using LargestUInt = Json::LargestUInt;
  using ArrayIndex = Json::ArrayIndex;

  using value_type = std::string;

#if JSON_USE_NULLREF
  static const Value& null;
  static const Value& nullRef;
#endif

  static Value const& nullSingleton();

  static constexpr LargestInt minLargestInt =
      LargestInt(~(LargestUInt(-1) / 2));
  static constexpr LargestInt maxLargestInt = LargestInt(LargestUInt(-1) / 2);
  static constexpr LargestUInt maxLargestUInt = LargestUInt(-1);

  static constexpr Int minInt = Int(~(UInt(-1) / 2));
  static constexpr Int maxInt = Int(UInt(-1) / 2);
  static constexpr UInt maxUInt = UInt(-1);

#if defined(JSON_HAS_INT64)
  static constexpr Int64 minInt64 = Int64(~(UInt64(-1) / 2));
  static constexpr Int64 maxInt64 = Int64(UInt64(-1) / 2);
  static constexpr UInt64 maxUInt64 = UInt64(-1);
#endif // defined(JSON_HAS_INT64)
  static constexpr UInt defaultRealPrecision = 17;
  static constexpr double maxUInt64AsDouble = 18446744073709551615.0;
#ifdef __NVCC__
public:
#else
private:
#endif
#ifndef JSONCPP_DOC_EXCLUDE_IMPLEMENTATION
  class CZString {
  public:
    enum DuplicationPolicy { noDuplication = 0, duplicate, duplicateOnCopy };
    CZString(ArrayIndex index);
    CZString(char const* str, unsigned length, DuplicationPolicy allocate);
    CZString(CZString const& other);
    CZString(CZString&& other) noexcept;
    ~CZString();
    CZString& operator=(const CZString& other);
    CZString& operator=(CZString&& other) noexcept;

    bool operator<(CZString const& other) const;
    bool operator==(CZString const& other) const;
    ArrayIndex index() const;
    char const* data() const;
    unsigned length() const;
    bool isStaticString() const;

  private:
    void swap(CZString& other);

    struct StringStorage {
      unsigned policy_ : 2;
      unsigned length_ : 30; // 1GB max
    };

    char const* cstr_; // actually, a prefixed string, unless policy is noDup
    union {
      ArrayIndex index_;
      StringStorage storage_;
    };
  };

public:
  typedef std::map<CZString, Value> ObjectValues;
#endif // ifndef JSONCPP_DOC_EXCLUDE_IMPLEMENTATION

public:
  /**
   * \brief Create a default Value of the given type.
   *
   * This is a very useful constructor.
   * To create an empty array, pass arrayValue.
   * To create an empty object, pass objectValue.
   * Another Value can then be set to this one by assignment.
   * This is useful since clear() and resize() will not alter types.
   *
   * Examples:
   *   \code
   *   Json::Value null_value; // null
   *   Json::Value arr_value(Json::arrayValue); // []
   *   Json::Value obj_value(Json::objectValue); // {}
   *   \endcode
   */
  Value(ValueType type = nullValue);
  Value(Int value);
  Value(UInt value);
#if defined(JSON_HAS_INT64)
  Value(Int64 value);
  Value(UInt64 value);
#endif // if defined(JSON_HAS_INT64)
  Value(double value);
  Value(const char* value); ///< Copy til first 0. (NULL causes to seg-fault.)
  Value(const char* begin, const char* end); ///< Copy all, incl zeroes.
  /**
   * \brief Constructs a value from a static string.
   *
   * Like other value string constructor but do not duplicate the string for
   * internal storage. The given string must remain alive after the call to
   * this constructor.
   *
   * \note This works only for null-terminated strings. (We cannot change the
   * size of this class, so we have nowhere to store the length, which might be
   * computed later for various operations.)
   *
   * Example of usage:
   *   \code
   *   static StaticString foo("some text");
   *   Json::Value aValue(foo);
   *   \endcode
   */
  Value(const StaticString& value);
  Value(const String& value);
  Value(bool value);
  Value(std::nullptr_t ptr) = delete;
  Value(const Value& other);
  Value(Value&& other) noexcept;
  ~Value();

  Value& operator=(const Value& other);
  Value& operator=(Value&& other) noexcept;

  void swap(Value& other);
  void swapPayload(Value& other);

  void copy(const Value& other);
  void copyPayload(const Value& other);

  ValueType type() const;

  bool operator<(const Value& other) const;
  bool operator<=(const Value& other) const;
  bool operator>=(const Value& other) const;
  bool operator>(const Value& other) const;
  bool operator==(const Value& other) const;
  bool operator!=(const Value& other) const;
  int compare(const Value& other) const;

  const char* asCString() const; ///< Embedded zeroes could cause you trouble!
#if JSONCPP_USING_SECURE_MEMORY
  unsigned getCStringLength() const; // Allows you to understand the length of
#endif
  String asString() const; ///< Embedded zeroes are possible.
  /** Get raw char* of string-value.
   *  \return false if !string. (Seg-fault if str or end are NULL.)
   */
  bool getString(char const** begin, char const** end) const;
  Int asInt() const;
  UInt asUInt() const;
#if defined(JSON_HAS_INT64)
  Int64 asInt64() const;
  UInt64 asUInt64() const;
#endif // if defined(JSON_HAS_INT64)
  LargestInt asLargestInt() const;
  LargestUInt asLargestUInt() const;
  float asFloat() const;
  double asDouble() const;
  bool asBool() const;

  bool isNull() const;
  bool isBool() const;
  bool isInt() const;
  bool isInt64() const;
  bool isUInt() const;
  bool isUInt64() const;
  bool isIntegral() const;
  bool isDouble() const;
  bool isNumeric() const;
  bool isString() const;
  bool isArray() const;
  bool isObject() const;

  template <typename T> T as() const JSONCPP_TEMPLATE_DELETE;
  template <typename T> bool is() const JSONCPP_TEMPLATE_DELETE;

  bool isConvertibleTo(ValueType other) const;

  ArrayIndex size() const;

  bool empty() const;

  explicit operator bool() const;

  void clear();

  void resize(ArrayIndex newSize);

  Value& operator[](ArrayIndex index);
  Value& operator[](int index);

  const Value& operator[](ArrayIndex index) const;
  const Value& operator[](int index) const;

  Value get(ArrayIndex index, const Value& defaultValue) const;
  bool isValidIndex(ArrayIndex index) const;
  Value& append(const Value& value);
  Value& append(Value&& value);

  bool insert(ArrayIndex index, const Value& newValue);
  bool insert(ArrayIndex index, Value&& newValue);

  Value& operator[](const char* key);
  const Value& operator[](const char* key) const;
  Value& operator[](const String& key);
  const Value& operator[](const String& key) const;
  /** \brief Access an object value by name, create a null member if it does not
   * exist.
   *
   * If the object has no entry for that name, then the member name used to
   * store the new entry is not duplicated.
   * Example of use:
   *   \code
   *   Json::Value object;
   *   static const StaticString code("code");
   *   object[code] = 1234;
   *   \endcode
   */
  Value& operator[](const StaticString& key);
  Value get(const char* key, const Value& defaultValue) const;
  Value get(const char* begin, const char* end,
            const Value& defaultValue) const;
  Value get(const String& key, const Value& defaultValue) const;
  Value const* find(char const* begin, char const* end) const;
  Value* demand(char const* begin, char const* end);
  void removeMember(const char* key);
  void removeMember(const String& key);
  bool removeMember(const char* key, Value* removed);
  /** \brief Remove the named map member.
   *
   *  Update 'removed' iff removed.
   *  \param key may contain embedded nulls.
   *  \return true iff removed (no exceptions)
   */
  bool removeMember(String const& key, Value* removed);
  bool removeMember(const char* begin, const char* end, Value* removed);
  /** \brief Remove the indexed array element.
   *
   *  O(n) expensive operations.
   *  Update 'removed' iff removed.
   *  \return true if removed (no exceptions)
   */
  bool removeIndex(ArrayIndex index, Value* removed);

  bool isMember(const char* key) const;
  bool isMember(const String& key) const;
  bool isMember(const char* begin, const char* end) const;

  Members getMemberNames() const;

  JSONCPP_DEPRECATED("Use setComment(String const&) instead.")
  void setComment(const char* comment, CommentPlacement placement) {
    setComment(String(comment, strlen(comment)), placement);
  }
  void setComment(const char* comment, size_t len, CommentPlacement placement) {
    setComment(String(comment, len), placement);
  }
  void setComment(String comment, CommentPlacement placement);
  bool hasComment(CommentPlacement placement) const;
  String getComment(CommentPlacement placement) const;

  String toStyledString() const;

  const_iterator begin() const;
  const_iterator end() const;

  iterator begin();
  iterator end();

  void setOffsetStart(ptrdiff_t start);
  void setOffsetLimit(ptrdiff_t limit);
  ptrdiff_t getOffsetStart() const;
  ptrdiff_t getOffsetLimit() const;

private:
  void setType(ValueType v) {
    bits_.value_type_ = static_cast<unsigned char>(v);
  }
  bool isAllocated() const { return bits_.allocated_; }
  void setIsAllocated(bool v) { bits_.allocated_ = v; }

  void initBasic(ValueType type, bool allocated = false);
  void dupPayload(const Value& other);
  void releasePayload();
  void dupMeta(const Value& other);

  Value& resolveReference(const char* key);
  Value& resolveReference(const char* key, const char* end);


  union ValueHolder {
    LargestInt int_;
    LargestUInt uint_;
    double real_;
    bool bool_;
    char* string_; // if allocated_, ptr to { unsigned, char[] }.
    ObjectValues* map_;
  } value_;

  struct {
    unsigned int value_type_ : 8;
    unsigned int allocated_ : 1;
  } bits_;

  class Comments {
  public:
    Comments() = default;
    Comments(const Comments& that);
    Comments(Comments&& that) noexcept;
    Comments& operator=(const Comments& that);
    Comments& operator=(Comments&& that) noexcept;
    bool has(CommentPlacement slot) const;
    String get(CommentPlacement slot) const;
    void set(CommentPlacement slot, String comment);

  private:
    using Array = std::array<String, numberOfCommentPlacement>;
    std::unique_ptr<Array> ptr_;
  };
  Comments comments_;

  ptrdiff_t start_;
  ptrdiff_t limit_;
};

template <> inline bool Value::as<bool>() const { return asBool(); }
template <> inline bool Value::is<bool>() const { return isBool(); }

template <> inline Int Value::as<Int>() const { return asInt(); }
template <> inline bool Value::is<Int>() const { return isInt(); }

template <> inline UInt Value::as<UInt>() const { return asUInt(); }
template <> inline bool Value::is<UInt>() const { return isUInt(); }

#if defined(JSON_HAS_INT64)
template <> inline Int64 Value::as<Int64>() const { return asInt64(); }
template <> inline bool Value::is<Int64>() const { return isInt64(); }

template <> inline UInt64 Value::as<UInt64>() const { return asUInt64(); }
template <> inline bool Value::is<UInt64>() const { return isUInt64(); }
#endif

template <> inline double Value::as<double>() const { return asDouble(); }
template <> inline bool Value::is<double>() const { return isDouble(); }

template <> inline String Value::as<String>() const { return asString(); }
template <> inline bool Value::is<String>() const { return isString(); }

template <> inline float Value::as<float>() const { return asFloat(); }
template <> inline const char* Value::as<const char*>() const {
  return asCString();
}

/** \brief Experimental and untested: represents an element of the "path" to
 * access a node.
 */
class JSON_API PathArgument {
public:
  friend class Path;

  PathArgument();
  PathArgument(ArrayIndex index);
  PathArgument(const char* key);
  PathArgument(String key);

private:
  enum Kind { kindNone = 0, kindIndex, kindKey };
  String key_;
  ArrayIndex index_{};
  Kind kind_{kindNone};
};

/** \brief Experimental and untested: represents a "path" to access a node.
 *
 * Syntax:
 * - "." => root node
 * - ".[n]" => elements at index 'n' of root node (an array value)
 * - ".name" => member named 'name' of root node (an object value)
 * - ".name1.name2.name3"
 * - ".[0][1][2].name1[3]"
 * - ".%" => member name is provided as parameter
 * - ".[%]" => index is provided as parameter
 */
class JSON_API Path {
public:
  Path(const String& path, const PathArgument& a1 = PathArgument(),
       const PathArgument& a2 = PathArgument(),
       const PathArgument& a3 = PathArgument(),
       const PathArgument& a4 = PathArgument(),
       const PathArgument& a5 = PathArgument());

  const Value& resolve(const Value& root) const;
  Value resolve(const Value& root, const Value& defaultValue) const;
  Value& make(Value& root) const;

private:
  using InArgs = std::vector<const PathArgument*>;
  using Args = std::vector<PathArgument>;

  void makePath(const String& path, const InArgs& in);
  void addPathInArg(const String& path, const InArgs& in,
                    InArgs::const_iterator& itInArg, PathArgument::Kind kind);
  static void invalidPath(const String& path, int location);

  Args args_;
};

/** \brief base class for Value iterators.
 *
 */
class JSON_API ValueIteratorBase {
public:
  using iterator_category = std::bidirectional_iterator_tag;
  using size_t = unsigned int;
  using difference_type = int;
  using SelfType = ValueIteratorBase;

  bool operator==(const SelfType& other) const { return isEqual(other); }

  bool operator!=(const SelfType& other) const { return !isEqual(other); }

  difference_type operator-(const SelfType& other) const {
    return other.computeDistance(*this);
  }

  Value key() const;

  UInt index() const;

  String name() const;

  JSONCPP_DEPRECATED("Use `key = name();` instead.")
  char const* memberName() const;
  char const* memberName(char const** end) const;

protected:
  /*! Internal utility functions to assist with implementing
   *   other iterator functions. The const and non-const versions
   *   of the "deref" protected methods expose the protected
   *   current_ member variable in a way that can often be
   *   optimized away by the compiler.
   */
  const Value& deref() const;
  Value& deref();

  void increment();

  void decrement();

  difference_type computeDistance(const SelfType& other) const;

  bool isEqual(const SelfType& other) const;

  void copy(const SelfType& other);

private:
  Value::ObjectValues::iterator current_;
  bool isNull_{true};

public:
  ValueIteratorBase();
  explicit ValueIteratorBase(const Value::ObjectValues::iterator& current);
};

/** \brief const iterator for object and array value.
 *
 */
class JSON_API ValueConstIterator : public ValueIteratorBase {
  friend class Value;

public:
  using value_type = const Value;
  using reference = const Value&;
  using pointer = const Value*;
  using SelfType = ValueConstIterator;

  ValueConstIterator();
  ValueConstIterator(ValueIterator const& other);

private:
  /*! \internal Use by Value to create an iterator.
   */
  explicit ValueConstIterator(const Value::ObjectValues::iterator& current);

public:
  SelfType& operator=(const ValueIteratorBase& other);

  SelfType operator++(int) {
    SelfType temp(*this);
    ++*this;
    return temp;
  }

  SelfType operator--(int) {
    SelfType temp(*this);
    --*this;
    return temp;
  }

  SelfType& operator--() {
    decrement();
    return *this;
  }

  SelfType& operator++() {
    increment();
    return *this;
  }

  reference operator*() const { return deref(); }

  pointer operator->() const { return &deref(); }
};

/** \brief Iterator for object and array value.
 */
class JSON_API ValueIterator : public ValueIteratorBase {
  friend class Value;

public:
  using value_type = Value;
  using size_t = unsigned int;
  using difference_type = int;
  using reference = Value&;
  using pointer = Value*;
  using SelfType = ValueIterator;

  ValueIterator();
  explicit ValueIterator(const ValueConstIterator& other);
  ValueIterator(const ValueIterator& other);

private:
  /*! \internal Use by Value to create an iterator.
   */
  explicit ValueIterator(const Value::ObjectValues::iterator& current);

public:
  SelfType& operator=(const SelfType& other);

  SelfType operator++(int) {
    SelfType temp(*this);
    ++*this;
    return temp;
  }

  SelfType operator--(int) {
    SelfType temp(*this);
    --*this;
    return temp;
  }

  SelfType& operator--() {
    decrement();
    return *this;
  }

  SelfType& operator++() {
    increment();
    return *this;
  }

  /*! The return value of non-const iterators can be
   *  changed, so the these functions are not const
   *  because the returned references/pointers can be used
   *  to change state of the base class.
   */
  reference operator*() const { return const_cast<reference>(deref()); }
  pointer operator->() const { return const_cast<pointer>(&deref()); }
};

inline void swap(Value& a, Value& b) { a.swap(b); }

} // namespace Json

#pragma pack(pop)

#if defined(JSONCPP_DISABLE_DLL_INTERFACE_WARNING)
#pragma warning(pop)
#endif // if defined(JSONCPP_DISABLE_DLL_INTERFACE_WARNING)

#endif // JSON_H_INCLUDED

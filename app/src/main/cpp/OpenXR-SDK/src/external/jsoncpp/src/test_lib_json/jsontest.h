
#ifndef JSONTEST_H_INCLUDED
#define JSONTEST_H_INCLUDED

#include <cstdio>
#include <deque>
#include <iomanip>
#include <json/config.h>
#include <json/value.h>
#include <json/writer.h>
#include <sstream>
#include <string>


/** \brief Unit testing framework.
 * \warning: all assertions are non-aborting, test case execution will continue
 *           even if an assertion namespace.
 *           This constraint is for portability: the framework needs to compile
 *           on Visual Studio 6 and must not require exception usage.
 */
namespace JsonTest {

class Failure {
public:
  const char* file_;
  unsigned int line_;
  Json::String expr_;
  Json::String message_;
  unsigned int nestingLevel_;
};

struct PredicateContext {
  using Id = unsigned int;
  Id id_;
  const char* file_;
  unsigned int line_;
  const char* expr_;
  PredicateContext* next_;
  Failure* failure_;
};

class TestResult {
public:
  TestResult();

  PredicateContext::Id predicateId_{1};

  PredicateContext* predicateStackTail_;

  void setTestName(const Json::String& name);

  TestResult& addFailure(const char* file, unsigned int line,
                         const char* expr = nullptr);

  TestResult& popPredicateContext();

  bool failed() const;

  void printFailure(bool printTestName) const;

  template <typename T> TestResult& operator<<(const T& value) {
    Json::OStringStream oss;
    oss << std::setprecision(16) << std::hexfloat << value;
    return addToLastFailure(oss.str());
  }

  TestResult& operator<<(bool value);
  TestResult& operator<<(Json::Int64 value);
  TestResult& operator<<(Json::UInt64 value);

private:
  TestResult& addToLastFailure(const Json::String& message);
  void addFailureInfo(const char* file, unsigned int line, const char* expr,
                      unsigned int nestingLevel);
  static Json::String indentText(const Json::String& text,
                                 const Json::String& indent);

  using Failures = std::deque<Failure>;
  Failures failures_;
  Json::String name_;
  PredicateContext rootPredicateNode_;
  PredicateContext::Id lastUsedPredicateId_{0};
  Failure* messageTarget_{nullptr};
};

class TestCase {
public:
  TestCase();

  virtual ~TestCase();

  void run(TestResult& result);

  virtual const char* testName() const = 0;

protected:
  TestResult* result_{nullptr};

private:
  virtual void runTestCase() = 0;
};

using TestCaseFactory = TestCase* (*)();

class Runner {
public:
  Runner();

  Runner& add(TestCaseFactory factory);

  int runCommandLine(int argc, const char* argv[]) const;

  bool runAllTest(bool printSummary) const;

  size_t testCount() const;

  Json::String testNameAt(size_t index) const;

  void runTestAt(size_t index, TestResult& result) const;

  static void printUsage(const char* appName);

private: // prevents copy construction and assignment
  Runner(const Runner& other) = delete;
  Runner& operator=(const Runner& other) = delete;

private:
  void listTests() const;
  bool testIndex(const Json::String& testName, size_t& indexOut) const;
  static void preventDialogOnCrash();

private:
  using Factories = std::deque<TestCaseFactory>;
  Factories tests_;
};

template <typename T, typename U>
TestResult& checkEqual(TestResult& result, T expected, U actual,
                       const char* file, unsigned int line, const char* expr) {
  if (static_cast<U>(expected) != actual) {
    result.addFailure(file, line, expr);
    result << "Expected: " << static_cast<U>(expected) << "\n";
    result << "Actual  : " << actual;
  }
  return result;
}

Json::String ToJsonString(const char* toConvert);
Json::String ToJsonString(Json::String in);
#if JSONCPP_USING_SECURE_MEMORY
Json::String ToJsonString(std::string in);
#endif

TestResult& checkStringEqual(TestResult& result, const Json::String& expected,
                             const Json::String& actual, const char* file,
                             unsigned int line, const char* expr);

} // namespace JsonTest

#define JSONTEST_ASSERT(expr)                                                  \
  if (expr) {                                                                  \
  } else                                                                       \
    result_->addFailure(__FILE__, __LINE__, #expr)

#define JSONTEST_ASSERT_PRED(expr)                                             \
  do {                                                                         \
    JsonTest::PredicateContext _minitest_Context = {                           \
        result_->predicateId_, __FILE__, __LINE__, #expr, NULL, NULL};         \
    result_->predicateStackTail_->next_ = &_minitest_Context;                  \
    result_->predicateId_ += 1;                                                \
    result_->predicateStackTail_ = &_minitest_Context;                         \
    (expr);                                                                    \
    result_->popPredicateContext();                                            \
  } while (0)

#define JSONTEST_ASSERT_EQUAL(expected, actual)                                \
  JsonTest::checkEqual(*result_, expected, actual, __FILE__, __LINE__,         \
                       #expected " == " #actual)

#define JSONTEST_ASSERT_STRING_EQUAL(expected, actual)                         \
  JsonTest::checkStringEqual(*result_, JsonTest::ToJsonString(expected),       \
                             JsonTest::ToJsonString(actual), __FILE__,         \
                             __LINE__, #expected " == " #actual)

#define JSONTEST_ASSERT_THROWS(expr)                                           \
  do {                                                                         \
    bool _threw = false;                                                       \
    try {                                                                      \
      expr;                                                                    \
    } catch (...) {                                                            \
      _threw = true;                                                           \
    }                                                                          \
    if (!_threw)                                                               \
      result_->addFailure(__FILE__, __LINE__,                                  \
                          "expected exception thrown: " #expr);                \
  } while (0)

#define JSONTEST_FIXTURE(FixtureType, name)                                    \
  class Test##FixtureType##name : public FixtureType {                         \
  public:                                                                      \
    static JsonTest::TestCase* factory() {                                     \
      return new Test##FixtureType##name();                                    \
    }                                                                          \
                                                                               \
  public: /* overridden from TestCase */                                       \
    const char* testName() const override { return #FixtureType "/" #name; }   \
    void runTestCase() override;                                               \
  };                                                                           \
                                                                               \
  void Test##FixtureType##name::runTestCase()

#define JSONTEST_FIXTURE_FACTORY(FixtureType, name)                            \
  &Test##FixtureType##name::factory

#define JSONTEST_REGISTER_FIXTURE(runner, FixtureType, name)                   \
  (runner).add(JSONTEST_FIXTURE_FACTORY(FixtureType, name))

#define JSONTEST_FIXTURE_V2(FixtureType, name, collections)                    \
  class Test##FixtureType##name : public FixtureType {                         \
  public:                                                                      \
    static JsonTest::TestCase* factory() {                                     \
      return new Test##FixtureType##name();                                    \
    }                                                                          \
    static bool collect() {                                                    \
      (collections).push_back(JSONTEST_FIXTURE_FACTORY(FixtureType, name));    \
      return true;                                                             \
    }                                                                          \
                                                                               \
  public: /* overridden from TestCase */                                       \
    const char* testName() const override { return #FixtureType "/" #name; }   \
    void runTestCase() override;                                               \
  };                                                                           \
                                                                               \
  static bool test##FixtureType##name##collect =                               \
      Test##FixtureType##name::collect();                                      \
                                                                               \
  void Test##FixtureType##name::runTestCase()

#endif // ifndef JSONTEST_H_INCLUDED

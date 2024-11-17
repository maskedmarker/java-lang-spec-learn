# java lambda

## ::运算符
In Java 8, the double colon (::) operator is called method references

### 方法引用分类:
There are four kinds of method references:
Reference to a static method ClassName::staticMethodName
Reference to an instance method of a particular object Object::instanceMethodName (实例方法涉及实例状态)
Reference to an instance method of an arbitrary object of a particular type ContainingType::methodName(实例方法不涉及实例状态)
Reference to a constructor ClassName::new

#### Static method举例
Lambda expression.
(args) -> ClassName.staticMethodName(args)
Method Reference.
ClassName::staticMethodName

#### Reference to an instance method of a particular object举例
Lambda expression.
(args) -> object.instanceMethodName(args)
Method Reference.
object::instanceMethodName

#### Reference to an instance method of an arbitrary object of a particular type举例
Lambda expression.
// arg0 is the first argument
(arg0, rest_of_args) -> arg0.methodName(rest_of_args)
// example, assume a and b are String
(a, b) -> a.compareToIgnoreCase(b)

Method Reference.
// first argument type
arg0_Type::methodName
// arg0 is type of ClassName
ClassName::methodName
// example, a is type of String
String::compareToIgnoreCase


## 注意区分Anonymous class/Lambda expressions/Method Reference
Anonymous class -> Lambda expressions.
List<String> list = Arrays.asList("node", "java", "python", "ruby");
list.forEach(str -> System.out.println(str)); // lambda

Lambda expressions -> Method references.
List<String> list = Arrays.asList("node", "java", "python", "ruby");
list.forEach(System.out::println);          // method references

Anonymous Class -> Lambda expression -> Method Reference

Note:
Both lambda expression or method reference does nothing but just another way call to an existing method. With method reference, it gains better readability.


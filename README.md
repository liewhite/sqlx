# TODO
[ ] 解析返回值 

# Combined of query and migration tool
# Known issues
* 无法插入或更新 `Int`等基本类型的值到 `null`, 因为
```scala
null.asInstanceOf[Int] == 0
```
尽管在Converter里返回了null， 但是类型转换的时候会转到0
> 解决办法， 尽量不使用Option， 可以使用默认值代替使得migrate时不报错
> 虽然无法把非null值改回null ， 但是可以在插入时就故意缺失该字段， 则会自动为null
> 或者定义为java.math.Integer


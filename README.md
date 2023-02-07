# Combined of query and migration tool
# Known issues
* 无法插入或更新 `Int`等基本类型的值到 `null`, 因为
```scala
null.asInstanceOf[Int] == 0
```
尽管在Converter里返回了null， 但是类型转换的时候会转到0
> 解决办法， 尽量不使用Option， 可以使用默认值代替使得migrate时不报错



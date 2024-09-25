#!/bin/sh
java -cp classes -Xmx1g ir.Engine -d davis_wiki -l dd2477.png -p patterns.txt
#java -cp classes -Xmx1g ir.Engine -d sample -l dd2477.png -p patterns.txt
# java -cp classes -Xmx1g ir.Engine -d one_sample -l dd2477.png -p patterns.txt
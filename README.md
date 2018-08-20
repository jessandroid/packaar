# pack-aar
#### How to use?


1.upload plugin to Local Respo

  如何上传plugin产考:https://blog.csdn.net/u011060103/article/details/80696524

2. Configure your project build script.

   > build.gradle

   ```groovy
   buildscript {
     repositories {
       maven {
         url "https://plugins.gradle.org/m2/"
       }
     }
     dependencies {
       classpath 'com.android.tools.build:gradle:3.0.1'
       classpath "com.dengquan:jar-plugin:1.4.XXX"
     }
   }
   ```

3. Apply the plugin and add 'fatLibraryExt ' on the top of your library module **build.gradle**:

   'enable = true' is mean fat-aar turn on.

   'enable = false' is mean fat-aar turn off.

   > library/build.gradle

   ```groovy
   apply plugin: 'com.dengquan.jarplugin'

   dependencies {
     embed project(path: ':subLibrary', configuration: 'default')
     implementation project(':subLibrary')
   }

   fatLibraryExt {
       enable true
   }

   ```

4. If you need to remove some files while packing, please add 'excludeFiles '.

   ```groovy
   packAARExt {
       enabled true
       excludeFiles {
         libs {
           fileNames('gson.jar')
         }
         jni {
            fileNames('test/test.so')
         }
       }
   }

   ```

​        
​        
